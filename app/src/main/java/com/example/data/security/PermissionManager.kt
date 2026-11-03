package com.example.data.security

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class AppPermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val permissions: List<String>,
    val isRequiredForCore: Boolean = false
)

object PermissionManager {

    private const val PREFS_NAME = "nillbook_permission_prefs"
    private const val KEY_FIRST_LAUNCH_PROMPTED = "key_first_launch_prompted"
    private const val KEY_DENIED_PREFIX = "key_denied_"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the user has already seen the first-launch permission rationale onboarding
     */
    fun hasCompletedFirstLaunchPrompt(context: Context): Boolean {
        val prefs = sharedPreferences ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_LAUNCH_PROMPTED, false)
    }

    /**
     * Marks first launch permission onboarding as completed
     */
    fun markFirstLaunchPromptCompleted(context: Context) {
        val prefs = sharedPreferences ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_PROMPTED, true).apply()
    }

    /**
     * Returns true if a specific permission category was explicitly declined by user
     */
    fun isPermissionDeniedByUser(context: Context, permissionId: String): Boolean {
        val prefs = sharedPreferences ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DENIED_PREFIX + permissionId, false)
    }

    /**
     * Records when a user chooses to skip or deny a permission so they are not repeatedly forced
     */
    fun setPermissionDeniedByUser(context: Context, permissionId: String, denied: Boolean) {
        val prefs = sharedPreferences ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DENIED_PREFIX + permissionId, denied).apply()
    }

    /**
     * Checks if all permissions in a list are currently granted
     */
    fun arePermissionsGranted(context: Context, permissions: List<String>): Boolean {
        return permissions.all { perm ->
            // On pre-33, POST_NOTIFICATIONS does not exist
            if (perm == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * List of permission categories matching the prompt's explicit requirements
     */
    fun getAppPermissionItems(): List<AppPermissionItem> {
        val list = mutableListOf(
            AppPermissionItem(
                id = "contacts",
                title = "Contacts",
                description = "Required to display, search, and manage your caller directory contacts.",
                permissions = listOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                ),
                isRequiredForCore = true
            ),
            AppPermissionItem(
                id = "microphone",
                title = "Microphone",
                description = "Required for end-to-end encrypted audio calls and recording voice notes.",
                permissions = listOf(
                    Manifest.permission.RECORD_AUDIO
                )
            ),
            AppPermissionItem(
                id = "camera",
                title = "Camera",
                description = "Required for end-to-end encrypted video calls and taking photo attachments.",
                permissions = listOf(
                    Manifest.permission.CAMERA
                )
            ),
            AppPermissionItem(
                id = "phone_call_logs",
                title = "Phone & Call Logs",
                description = "Required to make direct phone calls and access your recent call history.",
                permissions = listOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CALL_LOG
                )
            ),
            AppPermissionItem(
                id = "sms",
                title = "SMS",
                description = "Required to send and receive emergency carrier SMS when internet is offline.",
                permissions = listOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                )
            )
        )

        // Android 13+ Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                AppPermissionItem(
                    id = "notifications",
                    title = "Notifications",
                    description = "Required to notify you about incoming encrypted calls and messages.",
                    permissions = listOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            )
        }

        return list
    }

    /**
     * Aggregates all permission strings needed for the runtime request contract
     */
    fun getAllRequiredPermissionsArray(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }
}
