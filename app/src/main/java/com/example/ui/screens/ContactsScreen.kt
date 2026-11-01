package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.components.ContactListItem
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.theme.*

enum class ContactsTab {
    CONTACTS,
    CALL_HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    initialAddName: String? = null,
    initialAddPhone: String? = null,
    onBack: (() -> Unit)? = null,
    onOpenChat: ((phone: String, name: String) -> Unit)? = null,
    onViewProfile: ((userIdOrPhone: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by NillBookRepository.userContacts.collectAsState()
    val callLogs by NillBookRepository.callLogs.collectAsState()

    var activeTab by remember { mutableStateOf(ContactsTab.CONTACTS) }
    var filterText by remember { mutableStateOf("") }
    var callHistoryFilter by remember { mutableStateOf("ALL") } // "ALL", "MISSED", "INCOMING", "OUTGOING"

    var showContactDialog by remember { mutableStateOf(initialAddName != null || initialAddPhone != null) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Contact?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val filteredContacts = remember(contacts, filterText) {
        if (filterText.isBlank()) contacts
        else contacts.filter {
            it.name.contains(filterText, ignoreCase = true) ||
            it.phone.contains(filterText) ||
            (it.email != null && it.email.contains(filterText, ignoreCase = true))
        }
    }

    val filteredCallLogs = remember(callLogs, callHistoryFilter) {
        when (callHistoryFilter) {
            "MISSED" -> callLogs.filter { it.type == CallType.MISSED }
            "INCOMING" -> callLogs.filter { it.type == CallType.INCOMING }
            "OUTGOING" -> callLogs.filter { it.type == CallType.OUTGOING }
            else -> callLogs
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate900)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate200)
                            }
                        }
                        Column {
                            Text(
                                text = if (activeTab == ContactsTab.CONTACTS) "DIRECTORY CONTACTS" else "CALL HISTORY",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeTab == ContactsTab.CONTACTS)
                                    "${contacts.size} saved directory contacts"
                                else
                                    "${callLogs.size} recorded call activities",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (activeTab == ContactsTab.CONTACTS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate900)
                                    .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                    .testTag("import_vcf_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Import VCF", tint = Cyan400)
                            }

                            Button(
                                onClick = {
                                    editingContact = null
                                    showContactDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("add_contact_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        if (callLogs.isNotEmpty()) {
                            TextButton(
                                onClick = { showClearHistoryDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Red400)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Segmented Tab Switcher: Contacts vs Call History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTab = ContactsTab.CONTACTS }
                            .testTag("tab_contacts"),
                        color = if (activeTab == ContactsTab.CONTACTS) Cyan400 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = if (activeTab == ContactsTab.CONTACTS) Slate950 else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Contacts (${contacts.size})",
                                    color = if (activeTab == ContactsTab.CONTACTS) Slate950 else Slate400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTab = ContactsTab.CALL_HISTORY }
                            .testTag("tab_call_history"),
                        color = if (activeTab == ContactsTab.CALL_HISTORY) Cyan400 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneCallback,
                                    contentDescription = null,
                                    tint = if (activeTab == ContactsTab.CALL_HISTORY) Slate950 else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Call History (${callLogs.size})",
                                    color = if (activeTab == ContactsTab.CALL_HISTORY) Slate950 else Slate400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content: CONTACTS
            if (activeTab == ContactsTab.CONTACTS) {
                // Filter Field
                item {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Filter contacts by name, phone or email...", color = Slate500, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = { filterText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate800,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contacts_filter_field")
                    )
                }

                // Toast message
                if (toastMessage != null) {
                    item {
                        Surface(
                            color = Slate900,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(toastMessage ?: "", color = Slate200, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Empty state
                if (filteredContacts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = Slate600, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (filterText.isEmpty()) "No contacts saved yet" else "No matching contacts found",
                                color = Slate400,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add contacts or import a .vcf card file to contribute to caller ID",
                                color = Slate500,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactListItem(
                            contact = contact,
                            onContactClick = {
                                onViewProfile?.invoke(contact.phone)
                            },
                            onCallClick = {
                                NillBookRepository.recordCall(contact.name, contact.phone, CallType.OUTGOING)
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                context.startActivity(dialIntent)
                            },
                            onMessageClick = {
                                onOpenChat?.invoke(contact.phone, contact.name)
                            },
                            onEditClick = {
                                editingContact = contact
                                showContactDialog = true
                            },
                            onDeleteClick = {
                                deleteCandidate = contact
                            }
                        )
                    }
                }
            } else {
                // Tab Content: CALL HISTORY
                item {
                    // Filter Chips: All, Missed, Incoming, Outgoing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All Calls",
                            "MISSED" to "Missed",
                            "INCOMING" to "Incoming",
                            "OUTGOING" to "Outgoing"
                        ).forEach { (key, label) ->
                            val isSel = callHistoryFilter == key
                            FilterChip(
                                selected = isSel,
                                onClick = { callHistoryFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan400,
                                    selectedLabelColor = Slate950,
                                    containerColor = Slate900,
                                    labelColor = Slate300
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = if (isSel) Cyan400 else Slate800
                                )
                            )
                        }
                    }
                }

                if (filteredCallLogs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhoneMissed, contentDescription = null, tint = Slate600, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No call history found", color = Slate400, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Your recent incoming, outgoing, and missed calls appear here", color = Slate500, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredCallLogs, key = { it.id }) { log ->
                        CallLogCard(
                            log = log,
                            onProfileClick = {
                                onViewProfile?.invoke(log.phone)
                            },
                            onCall = {
                                NillBookRepository.recordCall(log.name, log.phone, CallType.OUTGOING)
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${log.phone}"))
                                context.startActivity(dialIntent)
                            },
                            onMessage = {
                                onOpenChat?.invoke(log.phone, log.name)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showContactDialog) {
        ContactFormDialog(
            existing = editingContact,
            initialName = if (editingContact == null) initialAddName.orEmpty() else "",
            initialPhone = if (editingContact == null) initialAddPhone.orEmpty() else "",
            onDismiss = { showContactDialog = false },
            onSave = { name, phone, email, address, birthday ->
                if (editingContact != null) {
                    val ok = NillBookRepository.updateContact(editingContact!!.id, name, phone, email, address, birthday)
                    if (ok) {
                        toastMessage = "Contact updated successfully"
                        showContactDialog = false
                    } else {
                        toastMessage = "Duplicate phone number in contacts"
                    }
                } else {
                    val ok = NillBookRepository.addContact(name, phone, email, address, birthday)
                    if (ok) {
                        toastMessage = "Contact added to NILLBOOK directory"
                        showContactDialog = false
                    } else {
                        toastMessage = "Contact already exists with this number"
                    }
                }
            }
        )
    }

    // Import VCF Dialog
    if (showImportDialog) {
        ImportVcfDialog(
            onDismiss = { showImportDialog = false },
            onImport = { vcfText, country ->
                val imported = NillBookRepository.importVcf(vcfText, country)
                toastMessage = "Successfully imported $imported contacts!"
                showImportDialog = false
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Call History?", color = Color.White) },
            text = { Text("All recent call records will be cleared.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.clearCallLogs()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Slate950)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Contact?", color = Color.White) },
            text = { Text("Are you sure you want to remove ${c.name} from your contacts?", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.deleteContact(c.id)
                        deleteCandidate = null
                        toastMessage = "Contact removed"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Slate950)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun CallLogCard(
    log: CallLogEntry,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("call_log_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatarPlaceholder(
                name = log.name,
                size = 44,
                modifier = if (onProfileClick != null) Modifier.clickable { onProfileClick() } else Modifier
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onProfileClick != null) Modifier.clickable { onProfileClick() }
                        else Modifier
                    )
            ) {
                Text(
                    text = log.name,
                    color = if (log.type == CallType.MISSED) Red400 else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (log.type) {
                        CallType.MISSED -> Icons.Default.CallMissed to Red400
                        CallType.INCOMING -> Icons.Default.CallReceived to Emerald400
                        CallType.OUTGOING -> Icons.Default.CallMade to Cyan400
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = log.type.name,
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.phone,
                        color = Slate400,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (log.duration.isNotEmpty()) "${log.timestamp} • ${log.duration}" else log.timestamp,
                    color = Slate500,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onCall,
                    modifier = Modifier.size(40.dp).testTag("call_log_call_${log.id}")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald400, modifier = Modifier.size(19.dp))
                }
                IconButton(
                    onClick = onMessage,
                    modifier = Modifier.size(40.dp).testTag("call_log_msg_${log.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Cyan400, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormDialog(
    existing: Contact? = null,
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String?, address: String?, birthday: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: initialName) }
    var phone by remember { mutableStateOf(existing?.phone ?: initialPhone) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var birthday by remember { mutableStateOf(existing?.birthday.orEmpty()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (existing == null) "New Contact" else "Edit Contact",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (errorText != null) {
                    Text(text = errorText ?: "", color = Red400, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_contact_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_contact_phone")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Slate400)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || phone.isBlank()) {
                                errorText = "Name and phone cannot be empty."
                            } else {
                                onSave(
                                    name.trim(),
                                    phone.trim(),
                                    email.trim().ifEmpty { null },
                                    address.trim().ifEmpty { null },
                                    birthday.trim().ifEmpty { null }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_contact_btn")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportVcfDialog(
    onDismiss: () -> Unit,
    onImport: (vcfText: String, country: String) -> Unit
) {
    var vcfText by remember {
        mutableStateOf(
            "BEGIN:VCARD\nVERSION:3.0\nFN:Tanvir Ahmed\nTEL:+8801912334455\nEMAIL:tanvir@example.com\nEND:VCARD\nBEGIN:VCARD\nVERSION:3.0\nFN:Dr. Kamal Hossain\nTEL:+8801555667788\nEND:VCARD"
        )
    }
    var selectedCountry by remember { mutableStateOf("BD") }
    var isExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Import vCard / Contacts (.vcf)", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Paste .vcf vCard data below to bulk import contacts into directory:", color = Slate400, fontSize = 12.sp)

                OutlinedTextField(
                    value = vcfText,
                    onValueChange = { vcfText = it },
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Slate400)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onImport(vcfText, selectedCountry) },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Import Contacts", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
