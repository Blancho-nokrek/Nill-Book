package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.components.SmartCallOptionDialog
import com.example.ui.theme.*

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
    val isOnline by NillBookRepository.isOnline.collectAsState()
    val contacts by NillBookRepository.userContacts.collectAsState()

    var filterText by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(ContactGroup.ALL) }

    var showContactDialog by remember { mutableStateOf(initialAddName != null || initialAddPhone != null) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Contact?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Smart Call option dialog state
    var callDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Read Contacts Runtime Permission launcher
    // Read Contacts Runtime Permission launcher - Auto imports on permission grant
    val requestContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val deviceList = DeviceContactsHelper.readDeviceContacts(context)
            val count = NillBookRepository.importDeviceContacts(deviceList)
            if (count > 0) {
                toastMessage = "Auto-synced $count contacts from phone!"
            }
        }
    }

    // Auto-request contact permission on launch and load device contacts automatically
    LaunchedEffect(Unit) {
        if (DeviceContactsHelper.hasContactsPermission(context)) {
            val deviceList = DeviceContactsHelper.readDeviceContacts(context)
            if (deviceList.isNotEmpty()) {
                NillBookRepository.importDeviceContacts(deviceList)
            }
        } else {
            requestContactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    // Filter contacts by query and group
    val filteredContacts = remember(contacts, filterText, selectedGroup) {
        var list = contacts
        if (selectedGroup == ContactGroup.FAVORITES) {
            list = list.filter { it.isFavorite }
        } else if (selectedGroup != ContactGroup.ALL) {
            list = list.filter { it.group == selectedGroup }
        }

        if (filterText.isNotBlank()) {
            list = list.filter {
                it.name.contains(filterText, ignoreCase = true) ||
                it.phone.contains(filterText) ||
                (it.email != null && it.email.contains(filterText, ignoreCase = true))
            }
        }
        list.sortedBy { it.name.lowercase() }
    }

    // Check for duplicates
    val duplicatesList = remember(contacts) {
        NillBookRepository.findDuplicateContacts()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            // PINNED SECTION: Header, Search field, and Group Chips stay pinned while scrolling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate950)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Header (Clean, reload/upload buttons removed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate900)
                                .size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate200)
                        }
                    }
                    Text(
                        text = LanguageManager.t("contacts"),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pinned Search / Filter Field
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Filter contacts by name or number...", color = Slate500, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
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

                Spacer(modifier = Modifier.height(8.dp))

                // Pinned Contact Group Chips (All, Favorites, Family, Work, Friends, VIP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ContactGroup.values().forEach { group ->
                        val isSelected = selectedGroup == group
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroup = group },
                            label = { Text(group.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400,
                                selectedLabelColor = Slate950,
                                containerColor = Slate900,
                                labelColor = Slate300
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Cyan400 else Slate800
                            )
                        )
                    }
                }
            }

            // Scrollable Contacts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

            // Toast banner
            toastMessage?.let { msg ->
                item {
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Slate200, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Contacts List
            if (filteredContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = Slate600, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filterText.isEmpty()) "No contacts in this group" else "No contacts matching \"$filterText\"",
                            color = Slate300,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add new contact or sync device phone book",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredContacts, key = { it.id }) { contact ->
                    AdvancedContactListItem(
                        contact = contact,
                        onContactClick = { onViewProfile?.invoke(contact.phone) },
                        onCallClick = {
                            callDialogTarget = Pair(contact.name, contact.phone)
                        },
                        onMessageClick = { onOpenChat?.invoke(contact.phone, contact.name) },
                        onToggleFavorite = { NillBookRepository.toggleFavorite(contact.id) },
                        onEditClick = {
                            editingContact = contact
                            showContactDialog = true
                        },
                        onDeleteClick = { deleteCandidate = contact }
                    )
                }
            }
        }
    }

    // WhatsApp-style Floating Action Button (FAB) at bottom-right corner for New Contact
    FloatingActionButton(
        onClick = {
            editingContact = null
            showContactDialog = true
        },
        containerColor = Cyan400,
        contentColor = Slate950,
        shape = CircleShape,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 24.dp)
            .size(56.dp)
            .testTag("add_contact_button")
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = "New Contact",
            modifier = Modifier.size(26.dp)
        )
    }
}

    // Smart Call Dialog
    callDialogTarget?.let { (name, phone) ->
        SmartCallOptionDialog(
            contactName = name,
            phone = phone,
            isOnline = isOnline,
            onDismiss = { callDialogTarget = null },
            onInternetAudioCall = {
                NillBookRepository.startInternetCall(name, phone, CallMode.INTERNET_AUDIO)
            },
            onInternetVideoCall = {
                NillBookRepository.startInternetCall(name, phone, CallMode.INTERNET_VIDEO)
            },
            onSimCall = { simSlot ->
                NillBookRepository.recordCall(name, phone, CallType.OUTGOING, CallMode.SIM_CARRIER, simSlot)
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(dialIntent)
            }
        )
    }

    // Add / Edit Dialog
    if (showContactDialog) {
        AdvancedContactFormDialog(
            existing = editingContact,
            initialName = if (editingContact == null) initialAddName.orEmpty() else "",
            initialPhone = if (editingContact == null) initialAddPhone.orEmpty() else "",
            onDismiss = { showContactDialog = false },
            onSave = { name, phone, secPhone, email, address, birthday, group, isFav ->
                if (editingContact != null) {
                    val ok = NillBookRepository.updateContact(
                        id = editingContact!!.id,
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        birthday = birthday,
                        secondaryPhone = secPhone,
                        group = group,
                        isFavorite = isFav
                    )
                    if (ok) {
                        toastMessage = "Contact updated"
                        showContactDialog = false
                    } else {
                        toastMessage = "Duplicate phone number detected"
                    }
                } else {
                    val ok = NillBookRepository.addContact(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        birthday = birthday,
                        secondaryPhone = secPhone,
                        group = group,
                        isFavorite = isFav
                    )
                    if (ok) {
                        toastMessage = "Contact added to Nill Book"
                        showContactDialog = false
                    } else {
                        toastMessage = "Contact with this number already exists"
                    }
                }
            }
        )
    }

    // Import / Export Dialog
    if (showImportExportDialog) {
        ImportExportVcfDialog(
            contacts = contacts,
            onDismiss = { showImportExportDialog = false },
            onImport = { vcfText, country ->
                val count = NillBookRepository.importVcf(vcfText, country)
                toastMessage = "Imported $count contacts from vCard!"
                showImportExportDialog = false
            }
        )
    }

    // Merge Duplicates Dialog
    if (showDuplicatesDialog) {
        MergeDuplicatesDialog(
            duplicates = duplicatesList,
            onDismiss = { showDuplicatesDialog = false },
            onMerge = { primaryId, duplicateId ->
                NillBookRepository.mergeContacts(primaryId, duplicateId)
                toastMessage = "Contacts merged successfully!"
            }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Contact?", color = Color.White) },
            text = { Text("Are you sure you want to remove ${c.name} (${c.phone})?", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.deleteContact(c.id)
                        deleteCandidate = null
                        toastMessage = "Contact deleted"
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
fun AdvancedContactListItem(
    contact: Contact,
    onContactClick: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onContactClick() }
            .testTag("contact_${contact.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ProfileAvatarPlaceholder(name = contact.name, size = 46, photoUrl = contact.photo)
                    if (contact.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Emerald400)
                                .border(2.dp, Slate900, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val isRegistered = remember(contact.phone, contact.isRegisteredOnNillBook) {
                    contact.isRegisteredOnNillBook || NillBookRepository.isUserRegistered(contact.phone)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.phone,
                        color = Cyan400,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRegistered) {
                            Surface(
                                color = Cyan400.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Cyan400.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Cyan400, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("NillBook User", color = Cyan400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Surface(
                                color = Amber400.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Amber400.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SimCard, contentDescription = null, tint = Amber400, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Phone Contact (SIM)", color = Amber400, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (contact.group != ContactGroup.ALL) {
                            Surface(color = Slate800, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = contact.group.label,
                                    color = Slate300,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (!contact.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📍 ${contact.address}",
                            color = Slate500,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (contact.isFavorite) Amber400 else Slate600,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Call
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("contact_call_${contact.id}")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald400, modifier = Modifier.size(19.dp))
                }

                // Message
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("contact_msg_${contact.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Cyan400, modifier = Modifier.size(19.dp))
                }

                // Edit
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate500, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedContactFormDialog(
    existing: Contact? = null,
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, secondaryPhone: String?, email: String?, address: String?, birthday: String?, group: ContactGroup, isFavorite: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: initialName) }
    var phone by remember { mutableStateOf(existing?.phone ?: initialPhone) }
    var secondaryPhone by remember { mutableStateOf(existing?.secondaryPhone.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var birthday by remember { mutableStateOf(existing?.birthday.orEmpty()) }
    var selectedGroup by remember { mutableStateOf(existing?.group ?: ContactGroup.ALL) }
    var isFavorite by remember { mutableStateOf(existing?.isFavorite ?: false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (existing == null) "New Directory Contact" else "Edit Contact",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                errorText?.let { Text(it, color = Red400, fontSize = 12.sp) }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *", color = Slate400) },
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
                    label = { Text("Primary Phone Number *", color = Slate400) },
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
                    value = secondaryPhone,
                    onValueChange = { secondaryPhone = it },
                    label = { Text("Secondary Phone (Optional)", color = Slate400) },
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)", color = Slate400) },
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
                    label = { Text("Address (Optional)", color = Slate400) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_address")
                )

                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Date of Birth (Optional, e.g. YYYY-MM-DD)", color = Slate400) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_birthday")
                )

                // Contact Group Selection
                Text("Contact Group", color = Slate400, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ContactGroup.FAMILY, ContactGroup.WORK, ContactGroup.FRIENDS, ContactGroup.VIP).forEach { grp ->
                        val isSel = selectedGroup == grp
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedGroup = grp },
                            color = if (isSel) Cyan400 else Slate800
                        ) {
                            Text(
                                text = grp.label,
                                color = if (isSel) Slate950 else Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || phone.isBlank()) {
                                errorText = "Name and phone cannot be empty."
                            } else {
                                onSave(
                                    name.trim(),
                                    phone.trim(),
                                    secondaryPhone.trim().ifEmpty { null },
                                    email.trim().ifEmpty { null },
                                    address.trim().ifEmpty { null },
                                    birthday.trim().ifEmpty { null },
                                    selectedGroup,
                                    isFavorite
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

@Composable
fun ImportExportVcfDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onImport: (vcfText: String, country: String) -> Unit
) {
    var vcfText by remember {
        mutableStateOf(DeviceContactsHelper.exportContactsToVcf(contacts))
    }
    var copiedToClipboard by remember { mutableStateOf(false) }

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
                Text("vCard Contacts (.vcf) Import & Export", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Export your contacts to vCard format or paste new .vcf cards to import:", color = Slate400, fontSize = 12.sp)

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
                        .height(130.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Slate400)
                    }

                    Button(
                        onClick = { onImport(vcfText, "BD") },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Import vCard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MergeDuplicatesDialog(
    duplicates: List<Pair<Contact, Contact>>,
    onDismiss: () -> Unit,
    onMerge: (primaryId: String, duplicateId: String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Amber400)),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Merge Duplicate Contacts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Found ${duplicates.size} duplicate entries with matching phone numbers or names:", color = Slate300, fontSize = 13.sp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(duplicates) { (primary, duplicate) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate950),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("1. ${primary.name} • ${primary.phone}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("2. ${duplicate.name} • ${duplicate.phone}", color = Slate400, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onMerge(primary.id, duplicate.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Merge into Single Contact", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
