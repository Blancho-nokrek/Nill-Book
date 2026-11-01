package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global localization manager locked to English only per project guidelines.
 */
object LanguageManager {
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        // English only
        _currentLanguage.value = "en"
    }

    fun toggleLanguage() {
        // English only
        _currentLanguage.value = "en"
    }

    fun t(key: String): String {
        return when (key) {
            "app_name" -> "Nill Book"
            "app_tagline" -> "Smart Caller Directory & Messenger"
            "search" -> "Search"
            "chats" -> "Chats"
            "contacts" -> "Contacts"
            "friends" -> "Friends"
            "calls" -> "Calls"
            "profile" -> "Profile"
            "online_mode" -> "Online Mode Active"
            "offline_mode" -> "Offline Mode (SIM & Local)"
            "call_dialog_title" -> "How would you like to call?"
            "call_option_internet_audio" -> "Internet Audio Call (Free)"
            "call_option_internet_video" -> "Internet Video Call (HD)"
            "call_option_sim" -> "SIM / Carrier Call (Mobile Network)"
            "sim_charges_disclaimer" -> "Standard carrier call rates or minutes apply according to your telecom provider."
            "msg_option_internet" -> "Internet Message (Real-Time)"
            "msg_option_sms" -> "Carrier SMS (Offline)"
            "msg_option_mms" -> "Carrier MMS"
            "sms_charges_disclaimer" -> "Carrier SMS rates apply. Sent via SIM cellular network."
            "send" -> "Send"
            "write_message" -> "Write a message..."
            "start_call" -> "Call"
            "favorites" -> "Favorites"
            "all" -> "All"
            "import_export" -> "Import / Export"
            "merge_duplicates" -> "Merge Duplicates"
            "sync_now" -> "Sync Now"
            "settings" -> "Settings"
            "language" -> "Language"
            "theme" -> "Theme"
            "dark_mode" -> "Dark Theme"
            "light_mode" -> "Light Theme"
            "system_mode" -> "System Default"
            "clear_history" -> "Clear History"
            "backup_restore" -> "Backup & Restore"
            "about_app" -> "About Nill Book"
            "logout" -> "Log Out"
            "delete_account" -> "Delete Account"
            "offline_dashboard" -> "Offline Dashboard"
            "sms_mms" -> "SMS & MMS"
            "sim_calls" -> "SIM Calls"
            "pending_sync" -> "Pending Sync"
            "local_profile" -> "Local Profile"
            "offline_banner_title" -> "OFFLINE MODE • SIM & LOCAL ONLY"
            "offline_banner_sub" -> "Carrier SIM & Local phone data active"
            "simulate_online" -> "Simulate Online"
            "simulate_offline" -> "Simulate Offline"
            "sim_network_ready" -> "SIM Network Ready"
            "draft_preserved" -> "Draft Preserved"
            "retry_sync" -> "Retry Sync Now"
            "queued_for_sync" -> "Will sync automatically when internet is restored"
            else -> key
        }
    }
}
