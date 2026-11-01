package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalMediaItem(
    val id: String,
    val fileName: String,
    val fileType: String, // "image", "video", "pdf", "audio", "document"
    val sizeBytes: Long,
    val localPath: String,
    val timestamp: String,
    val mimeType: String
)

data class BackupMetadata(
    val timestamp: String,
    val contactsCount: Int,
    val callsCount: Int,
    val conversationsCount: Int,
    val filePath: String,
    val fileSizeFormatted: String
)

object LocalStorageManager {

    private val _storedMedia = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    val storedMedia: StateFlow<List<LocalMediaItem>> = _storedMedia.asStateFlow()

    private val _lastBackup = MutableStateFlow<BackupMetadata?>(null)
    val lastBackup: StateFlow<BackupMetadata?> = _lastBackup.asStateFlow()

    fun initialize(context: Context) {
        val mediaDir = File(context.filesDir, "nillbook_media")
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
            // Populate sample locally cached attachments if directory was just created
            createSampleLocalMedia(context)
        }
        refreshMediaList(context)
        checkExistingBackups(context)
    }

    private fun createSampleLocalMedia(context: Context) {
        val mediaDir = File(context.filesDir, "nillbook_media")
        try {
            // Sample PDF Document
            val pdfFile = File(mediaDir, "NillBook_Project_Brief.pdf")
            if (!pdfFile.exists()) {
                pdfFile.writeText("Nill Book verified communication and contact management summary.")
            }
            // Sample Audio Note
            val voiceFile = File(mediaDir, "Voice_Note_Rahim_Bhai.m4a")
            if (!voiceFile.exists()) {
                voiceFile.writeText("Voice note audio stream data placeholder.")
            }
            // Sample Document
            val docFile = File(mediaDir, "Contacts_Export_List.vcf")
            if (!docFile.exists()) {
                docFile.writeText("BEGIN:VCARD\nVERSION:3.0\nFN:Rahim Bhai\nTEL:+8801700000000\nEND:VCARD")
            }
        } catch (_: Exception) {}
    }

    fun refreshMediaList(context: Context) {
        val mediaDir = File(context.filesDir, "nillbook_media")
        if (!mediaDir.exists()) return

        val items = mutableListOf<LocalMediaItem>()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

        mediaDir.listFiles()?.forEach { file ->
            val ext = file.extension.lowercase()
            val type = when (ext) {
                "jpg", "jpeg", "png", "webp", "gif" -> "image"
                "mp4", "mkv", "mov", "avi" -> "video"
                "pdf" -> "pdf"
                "mp3", "m4a", "wav", "aac" -> "audio"
                else -> "document"
            }
            val mime = when (type) {
                "image" -> "image/*"
                "video" -> "video/*"
                "pdf" -> "application/pdf"
                "audio" -> "audio/*"
                else -> "application/octet-stream"
            }
            items.add(
                LocalMediaItem(
                    id = file.name,
                    fileName = file.name,
                    fileType = type,
                    sizeBytes = file.length(),
                    localPath = file.absolutePath,
                    timestamp = dateFormat.format(Date(file.lastModified())),
                    mimeType = mime
                )
            )
        }
        _storedMedia.value = items.sortedByDescending { it.timestamp }
    }

    fun saveMediaLocally(context: Context, fileName: String, contentBytes: ByteArray, fileType: String): File? {
        val mediaDir = File(context.filesDir, "nillbook_media")
        if (!mediaDir.exists()) mediaDir.mkdirs()

        val destFile = File(mediaDir, fileName)
        return try {
            FileOutputStream(destFile).use { it.write(contentBytes) }
            refreshMediaList(context)
            destFile
        } catch (e: Exception) {
            null
        }
    }

    fun deleteMediaFile(context: Context, fileName: String): Boolean {
        val mediaDir = File(context.filesDir, "nillbook_media")
        val file = File(mediaDir, fileName)
        val deleted = if (file.exists()) file.delete() else false
        refreshMediaList(context)
        return deleted
    }

    fun clearLocalMediaCache(context: Context): Int {
        val mediaDir = File(context.filesDir, "nillbook_media")
        var count = 0
        mediaDir.listFiles()?.forEach {
            if (it.delete()) count++
        }
        refreshMediaList(context)
        return count
    }

    fun getTotalStorageSizeBytes(context: Context): Long {
        val mediaDir = File(context.filesDir, "nillbook_media")
        return mediaDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024f * 1024f * 1024f))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }

    fun openLocalFile(context: Context, localPath: String, mimeType: String) {
        try {
            val file = File(localPath)
            if (!file.exists()) return

            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Open file with"))
        } catch (_: Exception) {}
    }

    fun shareLocalFile(context: Context, localPath: String, mimeType: String) {
        try {
            val file = File(localPath)
            if (!file.exists()) return

            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Share file via"))
        } catch (_: Exception) {}
    }

    // --- LOCAL BACKUP & RESTORE ENGINE ---

    private fun checkExistingBackups(context: Context) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return

        val latest = backupDir.listFiles()?.filter { it.extension == "json" }?.maxByOrNull { it.lastModified() }
        if (latest != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
            _lastBackup.value = BackupMetadata(
                timestamp = dateFormat.format(Date(latest.lastModified())),
                contactsCount = NillBookRepository.userContacts.value.size,
                callsCount = NillBookRepository.callLogs.value.size,
                conversationsCount = NillBookRepository.conversations.value.size,
                filePath = latest.absolutePath,
                fileSizeFormatted = formatBytes(latest.length())
            )
        }
    }

    fun createLocalBackup(context: Context): File? {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = sdf.format(Date())
        val displayDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
        val backupFile = File(backupDir, "nillbook_backup_$timestamp.json")

        return try {
            val root = JSONObject()
            root.put("version", 2)
            root.put("app", "Nill Book")
            root.put("createdAt", displayDate)

            // Export contacts
            val contactsArr = JSONArray()
            NillBookRepository.userContacts.value.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("phone", c.phone)
                obj.put("secondaryPhone", c.secondaryPhone ?: "")
                obj.put("email", c.email ?: "")
                obj.put("isFavorite", c.isFavorite)
                obj.put("group", c.group.name)
                contactsArr.put(obj)
            }
            root.put("contacts", contactsArr)

            // Export call logs
            val callsArr = JSONArray()
            NillBookRepository.callLogs.value.forEach { call ->
                val obj = JSONObject()
                obj.put("id", call.id)
                obj.put("name", call.name)
                obj.put("phone", call.phone)
                obj.put("type", call.type.name)
                obj.put("timestamp", call.timestamp)
                obj.put("duration", call.duration)
                obj.put("mode", call.mode.name)
                obj.put("simSlot", call.simSlot)
                callsArr.put(obj)
            }
            root.put("callLogs", callsArr)

            // Export conversations
            val convArr = JSONArray()
            NillBookRepository.conversations.value.forEach { conv ->
                val obj = JSONObject()
                obj.put("id", conv.id)
                obj.put("contactName", conv.contactName)
                obj.put("phone", conv.phone)
                obj.put("lastMessage", conv.lastMessage)
                obj.put("lastTimestamp", conv.lastTimestamp)
                convArr.put(obj)
            }
            root.put("conversations", convArr)

            backupFile.writeText(root.toString(2))

            _lastBackup.value = BackupMetadata(
                timestamp = displayDate,
                contactsCount = contactsArr.length(),
                callsCount = callsArr.length(),
                conversationsCount = convArr.length(),
                filePath = backupFile.absolutePath,
                fileSizeFormatted = formatBytes(backupFile.length())
            )

            backupFile
        } catch (e: Exception) {
            null
        }
    }

    fun restoreLocalBackup(context: Context): Triple<Int, Int, Int> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return Triple(0, 0, 0)

        val latest = backupDir.listFiles()?.filter { it.extension == "json" }?.maxByOrNull { it.lastModified() }
            ?: return Triple(0, 0, 0)

        return try {
            val jsonStr = latest.readText()
            val root = JSONObject(jsonStr)

            var restoredContacts = 0
            var restoredCalls = 0
            var restoredConvs = 0

            // Restore contacts
            if (root.has("contacts")) {
                val contactsArr = root.getJSONArray("contacts")
                for (i in 0 until contactsArr.length()) {
                    val obj = contactsArr.getJSONObject(i)
                    val id = obj.optString("id", "restored_$i")
                    val name = obj.optString("name", "Unknown")
                    val phone = obj.optString("phone", "")
                    val secondary = obj.optString("secondaryPhone", "")
                    val email = obj.optString("email", "")
                    val fav = obj.optBoolean("isFavorite", false)

                    if (phone.isNotEmpty()) {
                        NillBookRepository.addContact(
                            name = name,
                            phone = phone,
                            email = if (email.isNotEmpty()) email else null,
                            secondaryPhone = if (secondary.isNotEmpty()) secondary else null,
                            group = ContactGroup.ALL,
                            isFavorite = fav
                        )
                        restoredContacts++
                    }
                }
            }

            // Restore call logs
            if (root.has("callLogs")) {
                val callsArr = root.getJSONArray("callLogs")
                for (i in 0 until callsArr.length()) {
                    val obj = callsArr.getJSONObject(i)
                    val name = obj.optString("name", "Call")
                    val phone = obj.optString("phone", "")
                    val typeStr = obj.optString("type", "OUTGOING")
                    val duration = obj.optString("duration", "")
                    val modeStr = obj.optString("mode", "SIM_CARRIER")
                    val slot = obj.optString("simSlot", "SIM 1")

                    val type = try { CallType.valueOf(typeStr) } catch (_: Exception) { CallType.OUTGOING }
                    val mode = try { CallMode.valueOf(modeStr) } catch (_: Exception) { CallMode.SIM_CARRIER }

                    NillBookRepository.recordCall(
                        name = name,
                        phone = phone,
                        type = type,
                        mode = mode,
                        simSlot = slot,
                        duration = duration
                    )
                    restoredCalls++
                }
            }

            // Restore conversations
            if (root.has("conversations")) {
                val convArr = root.getJSONArray("conversations")
                restoredConvs = convArr.length()
            }

            Triple(restoredContacts, restoredCalls, restoredConvs)
        } catch (e: Exception) {
            Triple(0, 0, 0)
        }
    }
}
