package com.securephoneapps.securebrowser.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.securephoneapps.securebrowser.MainActivity
import com.securephoneapps.securebrowser.data.DownloadEventBus
import com.securephoneapps.securebrowser.data.SecureDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream

class SecureDownloadService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val CHANNEL_ID = "secure_downloads_channel"
        private const val NOTIFICATION_ID = 4004
        
        fun startDownload(context: Context, url: String, userAgent: String?, contentDisposition: String?, mimetype: String?) {
            val intent = Intent(context, SecureDownloadService::class.java).apply {
                putExtra("EXTRA_URL", url)
                putExtra("EXTRA_USER_AGENT", userAgent)
                putExtra("EXTRA_CONTENT_DISPOSITION", contentDisposition)
                putExtra("EXTRA_MIMETYPE", mimetype)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("EXTRA_URL")
        val userAgent = intent?.getStringExtra("EXTRA_USER_AGENT")
        val contentDisposition = intent?.getStringExtra("EXTRA_CONTENT_DISPOSITION")
        val mimetype = intent?.getStringExtra("EXTRA_MIMETYPE")

        if (url == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val fileName = getFileName(url, contentDisposition, mimetype)
        val notification = buildNotification("Downloading $fileName", "Secure download in progress...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            try {
                val success = downloadAndEncryptFile(url, userAgent, contentDisposition, mimetype)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(applicationContext, "Successfully downloaded: $fileName", Toast.LENGTH_LONG).show()
                        showCompleteNotification(fileName)
                    } else {
                        Toast.makeText(applicationContext, "Download failed: $fileName", Toast.LENGTH_LONG).show()
                        showFailureNotification(fileName)
                    }
                    DownloadEventBus.notifyCompleted()
                    stopSelf()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Download error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showFailureNotification(fileName)
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun showCompleteNotification(fileName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText("Securely downloaded $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showFailureNotification(fileName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("Failed to download $fileName")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private suspend fun downloadAndEncryptFile(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ): Boolean = withContext(Dispatchers.IO) {
        var currentUrl = url
        var connection = URL(currentUrl).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        if (!userAgent.isNullOrEmpty()) {
            connection.setRequestProperty("User-Agent", userAgent)
        }

        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookies = cookieManager.getCookie(currentUrl)
        if (!cookies.isNullOrEmpty()) {
            connection.setRequestProperty("Cookie", cookies)
        }

        var responseCode = connection.responseCode
        var redirectCount = 0
        val maxRedirects = 5

        while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) && redirectCount < maxRedirects) {
            val newUrl = connection.getHeaderField("Location") ?: break
            connection.disconnect()
            currentUrl = URL(URL(currentUrl), newUrl).toString()

            connection = URL(currentUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            if (!userAgent.isNullOrEmpty()) {
                connection.setRequestProperty("User-Agent", userAgent)
            }

            val loopCookies = cookieManager.getCookie(currentUrl)
            if (!loopCookies.isNullOrEmpty()) {
                connection.setRequestProperty("Cookie", loopCookies)
            }
            responseCode = connection.responseCode
            redirectCount++
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val fileName = getFileName(currentUrl, contentDisposition, mimetype)
            val downloadsDir = File(noBackupFilesDir, "secure_downloads").apply { if (!exists()) mkdirs() }
            val outputFile = File(downloadsDir, fileName)

            val downloadManager = SecureDownloadManager(this@SecureDownloadService)
            val secretKey = downloadManager.getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey) }
            
            FileOutputStream(outputFile).use { fos ->
                fos.write(cipher.iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    connection.inputStream.use { input ->
                        input.copyTo(cos)
                    }
                }
            }
            true
        } else {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, text: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Secure Background Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun getFileName(url: String, cd: String?, mime: String?): String {
        var name = cd?.substringAfter("filename=", "")?.substringBefore(";")?.trim()?.removeSurrounding("\"") ?: ""
        if (name.isEmpty()) name = url.substringAfterLast("/").substringBefore("?")
        if (name.isEmpty()) name = "downloaded_asset"

        if (!name.contains(".") && !mime.isNullOrEmpty()) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (!ext.isNullOrEmpty()) {
                name = "$name.$ext"
            }
        }
        return name
    }
}
