package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object DeviceContactsHelper {

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readDeviceContacts(context: Context): List<Contact> {
        if (!hasContactsPermission(context)) return emptyList()

        val contactList = mutableListOf<Contact>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenPhones = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val rawPhone = if (numberIndex != -1) it.getString(numberIndex) else ""
                    val cleanPhone = rawPhone.replace(" ", "").replace("-", "")
                    if (cleanPhone.isBlank() || seenPhones.contains(cleanPhone)) continue

                    seenPhones.add(cleanPhone)
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Contact" else "Contact"
                    val id = if (idIndex != -1) it.getString(idIndex) ?: java.util.UUID.randomUUID().toString() else java.util.UUID.randomUUID().toString()
                    val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null

                    contactList.add(
                        Contact(
                            id = "dev_$id",
                            name = name,
                            phone = cleanPhone,
                            photo = photoUri,
                            syncStatus = SyncStatus.SYNCED
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        return contactList
    }

    fun exportContactsToVcf(contacts: List<Contact>): String {
        val sb = StringBuilder()
        for (c in contacts) {
            sb.append("BEGIN:VCARD\n")
            sb.append("VERSION:3.0\n")
            sb.append("FN:${c.name}\n")
            sb.append("TEL;TYPE=CELL:${c.phone}\n")
            if (!c.secondaryPhone.isNullOrBlank()) {
                sb.append("TEL;TYPE=WORK:${c.secondaryPhone}\n")
            }
            if (!c.email.isNullOrBlank()) {
                sb.append("EMAIL:${c.email}\n")
            }
            if (!c.address.isNullOrBlank()) {
                sb.append("ADR;TYPE=HOME:;;${c.address};;;;\n")
            }
            if (!c.birthday.isNullOrBlank()) {
                sb.append("BDAY:${c.birthday}\n")
            }
            sb.append("END:VCARD\n")
        }
        return sb.toString()
    }
}
