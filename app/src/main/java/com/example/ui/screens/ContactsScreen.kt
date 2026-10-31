package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.InitialsAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    initialAddName: String? = null,
    initialAddPhone: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by NillBookRepository.userContacts.collectAsState()
    var filterText by remember { mutableStateOf("") }

    var showContactDialog by remember { mutableStateOf(initialAddName != null || initialAddPhone != null) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Contact?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val filteredContacts = remember(contacts, filterText) {
        if (filterText.isBlank()) contacts
        else contacts.filter {
            it.name.contains(filterText, ignoreCase = true) ||
            it.phone.contains(filterText) ||
            (it.email != null && it.email.contains(filterText, ignoreCase = true))
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
                    Column {
                        Text(
                            text = "MY CONTACTS",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${contacts.size} contacts connected to caller directory",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }

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
                }
            }

            // Filter Field
            item {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Filter by name, phone or email...", color = Slate500, fontSize = 14.sp) },
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
                        .testTag("contacts_filter_input")
                )
            }

            // Status message
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
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(56.dp)
                        )
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
            }

            // Contact Items
            items(filteredContacts, key = { it.id }) { contact ->
                ContactCard(
                    contact = contact,
                    onCall = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                        context.startActivity(dialIntent)
                    },
                    onEdit = {
                        editingContact = contact
                        showContactDialog = true
                    },
                    onDelete = {
                        deleteCandidate = contact
                    }
                )
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
fun ContactCard(
    contact: Contact,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contact_item_${contact.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                InitialsAvatar(name = contact.name, size = 48)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.phone,
                        color = Cyan400,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.testTag("contact_call_${contact.id}")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald400, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("contact_edit_${contact.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate400, modifier = Modifier.size(19.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("contact_delete_${contact.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red400, modifier = Modifier.size(19.dp))
                    }
                }
            }

            // Extra details if available
            if (!contact.email.isNullOrBlank() || !contact.address.isNullOrBlank() || !contact.birthday.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!contact.email.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(contact.email, color = Slate400, fontSize = 12.sp)
                        }
                    }
                    if (!contact.address.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(contact.address, color = Slate400, fontSize = 12.sp)
                        }
                    }
                    if (!contact.birthday.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(PhoneUtils.formatBirthday(contact.birthday), color = Slate400, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormDialog(
    existing: Contact?,
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String?, address: String?, birthday: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: initialName) }
    var selectedCountry by remember { mutableStateOf("BD") }
    var phone by remember { mutableStateOf(existing?.phone ?: initialPhone) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var birthday by remember { mutableStateOf(existing?.birthday.orEmpty()) }
    var consentGranted by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countryMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (existing != null) "Edit Contact" else "Add New Contact",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

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
                    modifier = Modifier.fillMaxWidth().testTag("form_name_input")
                )

                // Country selector
                ExposedDropdownMenuBox(
                    expanded = countryMenuExpanded,
                    onExpandedChange = { countryMenuExpanded = !countryMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentCountry = PhoneUtils.COUNTRIES.find { it.code == selectedCountry } ?: PhoneUtils.COUNTRIES.first()
                    OutlinedTextField(
                        value = currentCountry.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country", color = Slate400) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = countryMenuExpanded,
                        onDismissRequest = { countryMenuExpanded = false },
                        modifier = Modifier.background(Slate900)
                    ) {
                        PhoneUtils.COUNTRIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.label, color = Slate200) },
                                onClick = {
                                    selectedCountry = c.code
                                    countryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *", color = Slate400) },
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
                    modifier = Modifier.fillMaxWidth().testTag("form_phone_input")
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
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Birthday (YYYY-MM-DD)", color = Slate400) },
                    placeholder = { Text("e.g. 1995-05-20", color = Slate600) },
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

                // Mandatory Consent Checkbox (matching Next.js logic)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = consentGranted,
                        onCheckedChange = { consentGranted = it },
                        colors = CheckboxDefaults.colors(checkedColor = Cyan400, checkmarkColor = Slate950),
                        modifier = Modifier.testTag("form_consent_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I consent to saving this contact to NILLBOOK caller network",
                        color = Slate300,
                        fontSize = 11.sp
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = Red400, fontSize = 12.sp)
                }

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
                            if (name.isBlank()) {
                                errorMessage = "Please enter a name"
                                return@Button
                            }
                            val normalized = PhoneUtils.toE164(phone, selectedCountry)
                            if (normalized == null) {
                                errorMessage = "Please enter a valid phone number"
                                return@Button
                            }
                            if (!consentGranted) {
                                errorMessage = "Consent is required to save contacts"
                                return@Button
                            }
                            onSave(name, normalized, email, address, birthday)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("form_save_button")
                    ) {
                        Text("Save Contact", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportVcfDialog(
    onDismiss: () -> Unit,
    onImport: (vcfText: String, country: String) -> Unit
) {
    var country by remember { mutableStateOf("BD") }
    var vcfText by remember {
        mutableStateOf(
            """
            BEGIN:VCARD
            VERSION:3.0
            FN:Rahman Electrician
            TEL:+8801711223344
            EMAIL:rahman@service.bd
            ADR:Mirpur-10, Dhaka
            BDAY:1985-03-12
            END:VCARD
            BEGIN:VCARD
            VERSION:3.0
            FN:Sultana Razia
            TEL:+8801999887766
            EMAIL:sultana@univ.edu
            ADR:Uttara Sector 4
            BDAY:1992-10-14
            END:VCARD
            """.trimIndent()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Import Contacts (.vcf / vCard)", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Paste or edit vCard text below to import contacts directly:", color = Slate400, fontSize = 12.sp)

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
                    modifier = Modifier.fillMaxWidth().height(160.dp).testTag("vcf_input_text")
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
                        onClick = { onImport(vcfText, country) },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_import_button")
                    ) {
                        Text("Parse & Import", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
