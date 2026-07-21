package com.securephoneapps.securebrowser.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class SecureDownloadManager(private val context: Context) {

    private val KEY_ALIAS = "secure_browser_download_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    suspend fun decryptFileToTemp(file: File): File? = withContext(Dispatchers.IO) {
        try {
            val decryptedDir = File(context.cacheDir, "decrypted").apply { if (!exists()) mkdirs() }
            val decryptedFile = File(decryptedDir, file.name)
            
            val secretKey = getOrCreateSecretKey()
            FileInputStream(file).use { fis ->
                val iv = ByteArray(12)
                fis.read(iv)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                }
                
                FileOutputStream(decryptedFile).use { fos ->
                    javax.crypto.CipherInputStream(fis, cipher).use { cis ->
                        cis.copyTo(fos)
                    }
                }
            }
            decryptedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportFileToPublicStorage(file: File) = withContext(Dispatchers.IO) {
        try {
            val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    val subDir = if (mimeType.startsWith("image/")) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$subDir/SecureBrowserDownloads")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = if (mimeType.startsWith("image/")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val uri = context.contentResolver.insert(collectionUri, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        val secretKey = getOrCreateSecretKey()
                        FileInputStream(file).use { fis ->
                            val iv = ByteArray(12)
                            fis.read(iv)
                            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                            }
                            javax.crypto.CipherInputStream(fis, cipher).use { cis -> cis.copyTo(out) }
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Exported to Public Vault", Toast.LENGTH_LONG).show() }
                }
            } else {
                val destDir = if (mimeType.startsWith("image/")) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val secureDir = File(destDir, "SecureBrowserDownloads").apply { if (!exists()) mkdirs() }
                val destFile = File(secureDir, file.name)
                FileInputStream(file).use { input -> FileOutputStream(destFile).use { out -> input.copyTo(out) } }
                withContext(Dispatchers.Main) { Toast.makeText(context, "Exported to legacy storage", Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    inner class SecureContainerDownloadListener(
        private val scope: CoroutineScope,
        private val viewModel: BrowserStateViewModel?,
        private val onPdfTriggered: (String) -> Unit
    ) : DownloadListener {

        override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
            if (url == null) return
            if (url.endsWith(".pdf", true) || mimetype?.contains("pdf", true) == true) {
                scope.launch(Dispatchers.Main) { onPdfTriggered("file:///android_asset/pdfjs/web/viewer.html?file=${java.net.URLEncoder.encode(url, "UTF-8")}") }
                return
            }

            Toast.makeText(context, "Secure Download Started...", Toast.LENGTH_SHORT).show()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.setRequestProperty("User-Agent", userAgent)
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            val fileName = getFileName(url, contentDisposition, mimetype)
                            val downloadsDir = File(context.noBackupFilesDir, "secure_downloads").apply { if (!exists()) mkdirs() }
                            val outputFile = File(downloadsDir, fileName)

                            val secretKey = getOrCreateSecretKey()
                            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey) }
                            FileOutputStream(outputFile).use { fos ->
                                fos.write(cipher.iv)
                                CipherOutputStream(fos, cipher).use { cos -> connection.inputStream.use { input -> input.copyTo(cos) } }
                            }
                            withContext(Dispatchers.Main) {
                                viewModel?.refreshDownloadedFiles(context)
                                Toast.makeText(context, "Securely downloaded: ${fileName}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        private fun getFileName(url: String, cd: String?, mime: String?): String {
            var name = cd?.substringAfter("filename=", "")?.substringBefore(";")?.trim()?.removeSurrounding("\"") ?: ""
            if (name.isEmpty()) name = url.substringAfterLast("/").substringBefore("?")
            return if (name.isEmpty()) "downloaded_asset" else name
        }
    }
}
