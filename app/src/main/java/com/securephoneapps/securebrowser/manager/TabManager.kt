package com.securephoneapps.securebrowser.manager

import com.securephoneapps.securebrowser.model.TabInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class TabManager {
    private val _tabs = MutableStateFlow<List<TabInfo>>(listOf(TabInfo(url = "https://www.google.com", title = "New Tab")))
    val tabs = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId = _activeTabId.asStateFlow()

    fun addTab(url: String = "https://www.google.com", title: String = "New Tab") {
        val newTab = TabInfo(url = url, title = title)
        _tabs.value += newTab
        _activeTabId.value = newTab.id
    }

    fun removeTab(tabId: UUID) {
        if (_tabs.value.size > 1) {
            val currentTabs = _tabs.value
            val index = currentTabs.indexOfFirst { it.id == tabId }
            _tabs.value = currentTabs.filter { it.id != tabId }
            if (_activeTabId.value == tabId) {
                val nextIndex = if (index >= _tabs.value.size) _tabs.value.size - 1 else index
                _activeTabId.value = _tabs.value[nextIndex].id
            }
        }
    }

    fun switchTab(tabId: UUID) {
        _activeTabId.value = tabId
    }

    fun updateTabUrl(tabId: UUID, url: String, title: String? = null) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(url = url, title = title ?: it.title) else it
        }
    }

    fun getActiveTab(): TabInfo? = _tabs.value.find { it.id == _activeTabId.value }
}
