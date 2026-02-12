package com.screenrest.app.presentation.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object BreakConfig : Screen("break_config")
    object CustomMessages : Screen("custom_messages")
    object IslamicReminders : Screen("islamic_reminders")
    object Permissions : Screen("permissions")
    object About : Screen("about")
}
