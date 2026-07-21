package com.securephoneapps.securebrowser.manager

import com.securephoneapps.securebrowser.model.TabInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class TabManager {
    private val _tabs = MutableStateFlow<List<TabInstance>>(emptyList())
    val tabs = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId = _activeTabId.asStateFlow()

    fun addTab(url: String = "https://www.google.com", title: String = "New Tab") {
        val newTab = TabInstance(tabId = UUID.randomUUID().toString(), currentUrl = url, pageTitle = title)
        _tabs.value += newTab
        _activeTabId.value = newTab.tabId
    }

    fun removeTab(tabId: String) {
        if (_tabs.value.size > 1) {
            val currentTabs = _tabs.value
            val index = currentTabs.indexOfFirst { it.tabId == tabId }
            _tabs.value = currentTabs.filter { it.tabId != tabId }
            if (_activeTabId.value == tabId) {
                val nextIndex = if (index >= _tabs.value.size) _tabs.value.size - 1 else index
                _activeTabId.value = _tabs.value[nextIndex].tabId
            }
        }
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun updateTabUrl(tabId: String, url: String, title: String? = null) {
        _tabs.value = _tabs.value.map {
            if (it.tabId == tabId) it.copy(currentUrl = url, pageTitle = title ?: it.pageTitle) else it
        }
    }

    fun setTabs(tabs: List<TabInstance>) {
        _tabs.value = tabs
    }

    fun getActiveTab(): TabInstance? = _tabs.value.find { it.tabId == _activeTabId.value }
}
