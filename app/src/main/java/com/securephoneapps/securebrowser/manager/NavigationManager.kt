package com.securephoneapps.securebrowser.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationManager {
    enum class Screen { Browser, Settings, AdvancedTabs, Downloads }

    private val _currentScreen = MutableStateFlow(Screen.Browser)
    val currentScreen = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun goBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            true
        } else {
            false
        }
    }
}
