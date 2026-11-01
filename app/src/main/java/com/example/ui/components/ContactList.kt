package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Contact
import com.example.data.PhoneUtils
import com.example.ui.theme.*

/**
 * A dedicated LazyColumn list component to display contacts with their name,
 * phone number, and a profile avatar placeholder.
 */
@Composable
fun ContactLazyColumn(
    contacts: List<Contact>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onContactClick: ((Contact) -> Unit)? = null,
    onCallClick: ((Contact) -> Unit)? = null,
    onMessageClick: ((Contact) -> Unit)? = null,
    onEditClick: ((Contact) -> Unit)? = null,
    onDeleteClick: ((Contact) -> Unit)? = null,
    emptyContent: @Composable (() -> Unit)? = null
) {
    if (contacts.isEmpty()) {
        if (emptyContent != null) {
            emptyContent()
        } else {
            DefaultEmptyContactsState(modifier = modifier)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("contacts_lazy_column"),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = contacts,
                key = { it.id }
            ) { contact ->
                ContactListItem(
                    contact = contact,
                    onContactClick = onContactClick,
                    onCallClick = onCallClick,
                    onMessageClick = onMessageClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    modifier = Modifier.testTag("contact_item_${contact.id}")
                )
            }
        }
    }
}

/**
 * Individual contact item card displaying profile avatar placeholder, name, and number.
 */
@Composable
fun ContactListItem(
    contact: Contact,
    modifier: Modifier = Modifier,
    onContactClick: ((Contact) -> Unit)? = null,
    onCallClick: ((Contact) -> Unit)? = null,
    onMessageClick: ((Contact) -> Unit)? = null,
    onEditClick: ((Contact) -> Unit)? = null,
    onDeleteClick: ((Contact) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onContactClick != null) Modifier.clickable { onContactClick(contact) }
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Avatar Placeholder
                ProfileAvatarPlaceholder(
                    name = contact.name,
                    size = 50,
                    modifier = Modifier.testTag("avatar_placeholder_${contact.id}")
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Name & Phone number details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = contact.phone,
                        color = Cyan400,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Action buttons (Call, Message, Edit, Delete)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCallClick != null) {
                        IconButton(
                            onClick = { onCallClick(contact) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("contact_call_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call ${contact.name}",
                                tint = Emerald400,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (onMessageClick != null) {
                        IconButton(
                            onClick = { onMessageClick(contact) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("contact_msg_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Message ${contact.name}",
                                tint = Cyan400,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (onEditClick != null) {
                        IconButton(
                            onClick = { onEditClick(contact) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("contact_edit_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${contact.name}",
                                tint = Slate400,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = { onDeleteClick(contact) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("contact_delete_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${contact.name}",
                                tint = Red400,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Optional metadata tags (Email, Address, Birthday)
            if (!contact.email.isNullOrBlank() || !contact.address.isNullOrBlank() || !contact.birthday.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!contact.email.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(contact.email, color = Slate400, fontSize = 12.sp)
                        }
                    }

                    if (!contact.address.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(contact.address, color = Slate400, fontSize = 12.sp)
                        }
                    }

                    if (!contact.birthday.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = PhoneUtils.formatBirthday(contact.birthday),
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Distinctive Profile Avatar Placeholder component.
 * Displays formatted uppercase initials with high-contrast cyan glow borders,
 * or fallback person icon placeholder.
 */
@Composable
fun ProfileAvatarPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 48,
    photoUrl: String? = null
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Cyan400.copy(alpha = 0.14f))
            .border(1.5.dp, Cyan400.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = Cyan400,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 2.5).sp,
                letterSpacing = 0.5.sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile avatar placeholder",
                tint = Cyan400,
                modifier = Modifier.size((size * 0.55).dp)
            )
        }
    }
}

@Composable
private fun DefaultEmptyContactsState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Slate900)
                .border(1.dp, Slate800, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No contacts found",
            color = Slate300,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add contacts or import a .vcf file to populate your directory",
            color = Slate500,
            fontSize = 13.sp
        )
    }
}
