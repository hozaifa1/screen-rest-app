package com.screenrest.app.presentation.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object BreakConfig : Screen("break_config")
    object CustomMessages : Screen("custom_messages")
    object IslamicReminders : Screen("islamic_reminders")
    object AyahList : Screen("ayah_list")
    object Permissions : Screen("permissions")
    object About : Screen("about")
    object BlockTime : Screen("block_time")
    object BlockTimeEditor : Screen("block_time_editor/{profileId}") {
        fun createRoute(profileId: Long = -1L) = "block_time_editor/$profileId"
    }
    object QuickBlock : Screen("quick_block")
}
