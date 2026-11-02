package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.components.SmartCallOptionDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    initialPhone: String? = null,
    initialName: String? = null,
    onOpenChat: ((phone: String, name: String) -> Unit)? = null,
    onClearTarget: (() -> Unit)? = null,
    onViewProfile: ((userIdOrPhone: String) -> Unit)? = null,
    onTriggerCall: ((name: String, phone: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by NillBookRepository.isOnline.collectAsState()
    val conversations by NillBookRepository.conversations.collectAsState()
    val contacts by NillBookRepository.userContacts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeConversationPhone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var activeConversationName by remember(initialName, initialPhone) { mutableStateOf(initialName ?: initialPhone ?: "Contact") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "GROUPS", "UNREAD"

    // Call dialog state
    var callDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(initialPhone, initialName) {
        if (!initialPhone.isNullOrBlank()) {
            activeConversationPhone = initialPhone
            activeConversationName = initialName ?: initialPhone
        } else {
            activeConversationPhone = null
        }
    }

    val activeConversation = remember(conversations, activeConversationPhone) {
        conversations.find { it.phone == activeConversationPhone }
    }

    // Filter conversations
    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        var list = conversations
        if (selectedFilter == "GROUPS") {
            list = list.filter { it.isGroup }
        } else if (selectedFilter == "UNREAD") {
            list = list.filter { it.unreadCount > 0 }
        }

        if (q.isEmpty()) list
        else list.filter {
            it.contactName.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.lastMessage.lowercase().contains(q)
        }
    }

    val matchingContacts = remember(contacts, conversations, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else {
            val existingPhones = conversations.map { it.phone }.toSet()
            contacts.filter {
                !existingPhones.contains(it.phone) &&
                (it.name.lowercase().contains(q) || it.phone.contains(q))
            }
        }
    }

    BackHandler(enabled = activeConversationPhone != null) {
        activeConversationPhone = null
        onClearTarget?.invoke()
    }

    LaunchedEffect(activeConversationPhone) {
        if (activeConversationPhone != null) {
            onOpenChat?.invoke(activeConversationPhone!!, activeConversationName)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        if (activeConversationPhone != null) {
            ChatDetailView(
                contactName = activeConversationName,
                phone = activeConversationPhone!!,
                messages = activeConversation?.messages ?: emptyList(),
                isOnline = isOnline,
                onBack = {
                    activeConversationPhone = null
                    onClearTarget?.invoke()
                },
                onViewProfile = {
                    onViewProfile?.invoke(activeConversationPhone!!)
                },
                onTriggerCallDialog = {
                    if (onTriggerCall != null) {
                        onTriggerCall(activeConversationName, activeConversationPhone!!)
                    } else {
                        callDialogTarget = Pair(activeConversationName, activeConversationPhone!!)
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
            ) {
                // PINNED SECTION: Search box and filter chips stay pinned when scrolling
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Pinned Search Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search messages, numbers, or contacts...", color = Slate500, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
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
                            .testTag("messages_search_box")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pinned Quick Filter Chips (All, Pinned, Unread)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "GROUPS" to "Groups",
                            "UNREAD" to "Unread"
                        ).forEach { (key, label) ->
                            val isSelected = selectedFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // Matching Directory Contacts
                    if (matchingContacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "START CHAT WITH CONTACTS",
                                color = Cyan400,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        items(matchingContacts, key = { "contact_${it.id}" }) { contact ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeConversationPhone = contact.phone
                                        activeConversationName = contact.name
                                        searchQuery = ""
                                        onOpenChat?.invoke(contact.phone, contact.name)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatarPlaceholder(name = contact.name, size = 38)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(contact.phone, color = Slate400, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Conversations List
                    if (filteredConversations.isEmpty() && matchingContacts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = null, tint = Slate600, modifier = Modifier.size(52.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "No conversations yet" else "No chats matching \"$searchQuery\"",
                                    color = Slate300,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Send an Internet or SIM message to start chatting",
                                    color = Slate500,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(filteredConversations, key = { it.phone }) { conv ->
                            EnhancedConversationCard(
                                conversation = conv,
                                onClick = {
                                    activeConversationPhone = conv.phone
                                    activeConversationName = conv.contactName
                                    onOpenChat?.invoke(conv.phone, conv.contactName)
                                },
                                onAvatarClick = {
                                    onViewProfile?.invoke(conv.phone)
                                },
                                onTogglePin = {
                                    NillBookRepository.togglePinConversation(conv.phone)
                                },
                                onToggleMute = {
                                    NillBookRepository.toggleMuteConversation(conv.phone)
                                }
                            )
                        }
                    }

                }
            }

            // WhatsApp-style Floating Action Button (FAB)
            FloatingActionButton(
                onClick = {
                    if (selectedFilter == "GROUPS") {
                        showCreateGroupDialog = true
                    } else {
                        showNewChatDialog = true
                    }
                },
                containerColor = Cyan400,
                contentColor = Slate950,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
                    .size(56.dp)
                    .testTag(if (selectedFilter == "GROUPS") "btn_create_new_group" else "btn_start_new_conversation")
            ) {
                Icon(
                    imageVector = if (selectedFilter == "GROUPS") Icons.Default.GroupAdd else Icons.AutoMirrored.Filled.Chat,
                    contentDescription = if (selectedFilter == "GROUPS") "Create New Group" else "Start New Conversation",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // New Chat Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Conversation", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select a contact to message:", color = Slate400, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(contacts, key = { it.id }) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate950)
                                    .clickable {
                                        activeConversationPhone = c.phone
                                        activeConversationName = c.name
                                        showNewChatDialog = false
                                        onOpenChat?.invoke(c.phone, c.name)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileAvatarPlaceholder(name = c.name, size = 38)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(c.phone, color = Cyan400, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Close", color = Cyan400)
                }
            },
            containerColor = Slate900
        )
    }

    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            contacts = contacts,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { name, members ->
                val newGroup = NillBookRepository.createGroupConversation(name, members)
                showCreateGroupDialog = false
                activeConversationPhone = newGroup.phone
                activeConversationName = newGroup.contactName
                onOpenChat?.invoke(newGroup.phone, newGroup.contactName)
            }
        )
    }

    // Call Options Dialog triggered from anywhere in Messages
    callDialogTarget?.let { (targetName, targetPhone) ->
        SmartCallOptionDialog(
            contactName = targetName,
            phone = targetPhone,
            isOnline = isOnline,
            onDismiss = { callDialogTarget = null },
            onInternetAudioCall = {
                NillBookRepository.startInternetCall(targetName, targetPhone, CallMode.INTERNET_AUDIO)
            },
            onInternetVideoCall = {
                NillBookRepository.startInternetCall(targetName, targetPhone, CallMode.INTERNET_VIDEO)
            },
            onSimCall = { simSlot ->
                NillBookRepository.recordCall(targetName, targetPhone, CallType.OUTGOING, CallMode.SIM_CARRIER, simSlot)
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetPhone"))
                context.startActivity(dialIntent)
            }
        )
    }
}

@Composable
fun EnhancedConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("conversation_${conversation.phone}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (conversation.isPinned) Cyan400.copy(alpha = 0.4f) else Slate800)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val contactPhoto = remember(conversation.phone) {
                    NillBookRepository.getPhotoForPhone(conversation.phone)
                }
                ProfileAvatarPlaceholder(
                    name = conversation.contactName,
                    size = 48,
                    photoUrl = contactPhoto,
                    modifier = if (onAvatarClick != null) Modifier.clickable { onAvatarClick() } else Modifier
                )
                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Emerald400)
                            .border(2.dp, Slate900, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = conversation.contactName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (conversation.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = Cyan400, modifier = Modifier.size(13.dp))
                        }
                        if (conversation.isMuted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.NotificationsOff, contentDescription = "Muted", tint = Slate500, modifier = Modifier.size(13.dp))
                        }
                    }
                    Text(
                        text = conversation.lastTimestamp,
                        color = Slate500,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        color = Slate400,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Cyan400)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                color = Slate950,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailView(
    contactName: String,
    phone: String,
    messages: List<ChatMessage>,
    isOnline: Boolean,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onTriggerCallDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(NillBookRepository.activeDraftText.value) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isRegisteredContact = remember(phone) {
        phone.startsWith("group_") || NillBookRepository.isUserRegistered(phone)
    }

    // 3 Smart Messaging Options: INTERNET, SMS, MMS (INTERNET only if registered on NillBook)
    var selectedChannel by remember {
        mutableStateOf(if (isOnline && isRegisteredContact) MessageChannel.INTERNET else MessageChannel.SMS)
    }

    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentUri by remember { mutableStateOf<String?>(null) }
    var showChatMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var audioSeconds by remember { mutableStateOf(0) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var selectedSimSlot by remember { mutableStateOf("SIM 1") }
    var isMuted by remember { mutableStateOf(false) }
    var showInChatSearch by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }

    // Media Picker for Images and MMS
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAttachmentUri = uri.toString()
            toastMessage = "Media attached"
        }
    }

    // Audio recording simulator ticker
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            audioSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                audioSeconds++
            }
        }
    }

    // Automatically update default channel when online status or contact registration shifts
    LaunchedEffect(isOnline, isRegisteredContact) {
        if (!isRegisteredContact) {
            if (selectedChannel == MessageChannel.INTERNET) {
                selectedChannel = MessageChannel.SMS
            }
        } else if (isOnline && selectedChannel == MessageChannel.SMS) {
            selectedChannel = MessageChannel.INTERNET
        } else if (!isOnline && selectedChannel == MessageChannel.INTERNET) {
            selectedChannel = MessageChannel.SMS
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // 1. PINNED TOP BAR (Status bar padded, strictly fixed, all 5 items: Back, Avatar/Name/Status, Audio Call, Video Call, 3-Dots)
        Surface(
            color = Slate900,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1) Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // 2) Profile Avatar + Name + Online Status (Clickable to open profile)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewProfile() }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val photoUrl = remember(phone) {
                        NillBookRepository.getPhotoForPhone(phone)
                    }
                    ProfileAvatarPlaceholder(name = contactName, size = 38, photoUrl = photoUrl)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!phone.startsWith("group_")) {
                            Text(
                                text = phone,
                                color = Slate400,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 3) Audio Call Button
                IconButton(
                    onClick = onTriggerCallDialog,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_audio_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Audio Call",
                        tint = Emerald400
                    )
                }

                // 4) Video Call Button
                IconButton(
                    onClick = {
                        if (isOnline && isRegisteredContact) {
                            NillBookRepository.startInternetCall(contactName, phone, CallMode.INTERNET_VIDEO)
                        } else {
                            onTriggerCallDialog()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_video_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = Cyan400
                    )
                }

                // 5) 3-Dots Menu Button (Standard Messaging Menu)
                Box {
                    IconButton(
                        onClick = { showChatMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("chat_more_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Slate300
                        )
                    }
                    DropdownMenu(
                        expanded = showChatMenu,
                        onDismissRequest = { showChatMenu = false },
                        modifier = Modifier.background(Slate900)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Contact", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Cyan400) },
                            onClick = {
                                showChatMenu = false
                                onViewProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Media, Links & Docs", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.PermMedia, contentDescription = null, tint = Cyan400) },
                            onClick = {
                                showChatMenu = false
                                val mediaCount = messages.count { it.attachmentUrl != null }
                                toastMessage = "Shared Media: $mediaCount attachments in chat"
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showInChatSearch) "Close Search" else "Search in Chat", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Cyan400) },
                            onClick = {
                                showChatMenu = false
                                showInChatSearch = !showInChatSearch
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications", color = Color.White) },
                            leadingIcon = { Icon(if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = null, tint = Slate300) },
                            onClick = {
                                showChatMenu = false
                                NillBookRepository.toggleMuteConversation(phone)
                                isMuted = !isMuted
                                toastMessage = if (isMuted) "Notifications muted" else "Notifications unmuted"
                            }
                        )
                        HorizontalDivider(color = Slate800)
                        DropdownMenuItem(
                            text = { Text("Clear Chat", color = Red400) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Red400) },
                            onClick = {
                                showChatMenu = false
                                showClearConfirm = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block Contact", color = Red400) },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Red400) },
                            onClick = {
                                showChatMenu = false
                                NillBookRepository.blockUser(phone)
                                toastMessage = "Contact $contactName blocked"
                            }
                        )
                    }
                }
            }
        }
    },
    bottomBar = {
            // 3. PINNED BOTTOM INPUT BAR (Accommodates navigation bar and keyboard)
            Surface(
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Clean, compact channel selector & SIM slot selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel Badges: Internet, SMS, MMS (Internet only if contact is registered on NillBook)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            val availableChannels = remember(isOnline, isRegisteredContact) {
                                if (isRegisteredContact) {
                                    if (isOnline) listOf(MessageChannel.INTERNET, MessageChannel.SMS, MessageChannel.MMS)
                                    else listOf(MessageChannel.SMS, MessageChannel.MMS)
                                } else {
                                    listOf(MessageChannel.SMS, MessageChannel.MMS)
                                }
                            }

                            if (!isRegisteredContact) {
                                Surface(
                                    color = Amber400.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "SIM Contact",
                                        color = Amber400,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            availableChannels.forEach { channel ->
                                val isSelected = selectedChannel == channel
                                val bg = when (channel) {
                                    MessageChannel.INTERNET -> if (isSelected) Cyan400 else Slate800
                                    MessageChannel.SMS -> if (isSelected) Amber400 else Slate800
                                    MessageChannel.MMS -> if (isSelected) Emerald400 else Slate800
                                }
                                val fg = if (isSelected) Slate950 else Slate400

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedChannel = channel
                                        }
                                        .testTag("channel_${channel.name.lowercase()}"),
                                    color = bg
                                ) {
                                    Text(
                                        text = channel.displayName,
                                        color = fg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Compact SIM Slot selector & char count if SMS or MMS
                        if (selectedChannel != MessageChannel.INTERNET) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (inputText.isNotEmpty()) {
                                    Text(
                                        text = "${inputText.length}/160",
                                        color = Slate400,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                listOf("SIM 1", "SIM 2").forEach { slot ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { selectedSimSlot = slot },
                                        color = if (selectedSimSlot == slot) Amber400 else Slate800
                                    ) {
                                        Text(
                                            text = slot,
                                            color = if (selectedSimSlot == slot) Slate950 else Slate400,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reply quotation preview if active
                    replyingToMessage?.let { replyMsg ->
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Replying to:", color = Cyan400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(replyMsg.text, color = Slate300, fontSize = 12.sp, maxLines = 1)
                                }
                                IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Slate400, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Media Attachment Preview
                    selectedAttachmentUri?.let { uri ->
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Cyan400, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Attached media image", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedAttachmentUri = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Slate400, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Input & Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Photo attachment button
                        IconButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Slate400, modifier = Modifier.size(20.dp))
                        }

                        // Voice message simulation button
                        IconButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    isRecordingAudio = false
                                    NillBookRepository.sendMessage(
                                        phone = phone,
                                        contactName = contactName,
                                        text = "",
                                        channel = selectedChannel,
                                        attachmentType = "audio",
                                        audioDuration = audioSeconds
                                    )
                                    toastMessage = "Voice message sent (${audioSeconds}s)"
                                } else {
                                    isRecordingAudio = true
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isRecordingAudio) Red400 else Color.Transparent)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Note",
                                tint = if (isRecordingAudio) Slate950 else Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isRecordingAudio) {
                            Text(
                                text = "Recording audio: ${audioSeconds}s (Tap mic to send)",
                                color = Red400,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = {
                                    inputText = it
                                    NillBookRepository.setActiveDraft(it)
                                },
                                placeholder = {
                                    Text(
                                        when (selectedChannel) {
                                            MessageChannel.INTERNET -> "Type a message..."
                                            MessageChannel.SMS -> "Type SMS message..."
                                            MessageChannel.MMS -> "Type MMS message..."
                                        },
                                        color = Slate500,
                                        fontSize = 13.sp
                                    )
                                },
                                singleLine = false,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (selectedChannel == MessageChannel.INTERNET) Cyan400 else Amber400,
                                    unfocusedBorderColor = Slate800,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field")
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || selectedAttachmentUri != null) {
                                    if (editingMessageId != null) {
                                        NillBookRepository.editMessage(phone, editingMessageId!!, inputText)
                                        editingMessageId = null
                                    } else {
                                        NillBookRepository.sendMessage(
                                            phone = phone,
                                            contactName = contactName,
                                            text = inputText,
                                            channel = selectedChannel,
                                            attachmentUrl = selectedAttachmentUri,
                                            attachmentType = if (selectedAttachmentUri != null) "image" else null,
                                            replyToText = replyingToMessage?.text
                                        )
                                    }
                                    inputText = ""
                                    NillBookRepository.setActiveDraft("")
                                    selectedAttachmentUri = null
                                    replyingToMessage = null
                                    scope.launch {
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (selectedChannel == MessageChannel.INTERNET) Cyan400 else Amber400)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Slate950,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // 2. MIDDLE MESSAGES AREA (Scrolls independently between pinned top and bottom bars)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    InteractiveMessageBubble(
                        message = msg,
                        onReply = { replyingToMessage = msg },
                        onEdit = {
                            if (msg.isFromMe) {
                                editingMessageId = msg.id
                                inputText = msg.text
                            }
                        },
                        onDelete = {
                            NillBookRepository.deleteMessage(phone, msg.id)
                        },
                        onToggleReaction = { emoji ->
                            NillBookRepository.toggleReaction(phone, msg.id, emoji)
                        }
                    )
                }
            }

            toastMessage?.let { toast ->
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Cyan400)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Text(toast, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                LaunchedEffect(toast) {
                    delay(2500)
                    toastMessage = null
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Chat?", color = Color.White) },
            text = { Text("All messages in this conversation will be cleared.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.clearConversation(phone)
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Slate950)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun InteractiveMessageBubble(
    message: ChatMessage,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleReaction: (String) -> Unit
) {
    val isMe = message.isFromMe
    var showEmojiBar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Floating Emoji Reaction Picker Bar on tap/click
        AnimatedVisibility(visible = showEmojiBar) {
            Surface(
                color = Slate800,
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("❤️", "👍", "😂", "😮", "😢", "🔥").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onToggleReaction(emoji)
                                    showEmojiBar = false
                                }
                                .padding(4.dp)
                        )
                    }
                    if (isMe) {
                        IconButton(onClick = { onEdit(); showEmojiBar = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate400, modifier = Modifier.size(14.dp))
                        }
                    }
                    IconButton(onClick = { onDelete(); showEmojiBar = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red400, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Surface(
            color = if (isMe) (if (message.channel == MessageChannel.INTERNET) Cyan950 else Amber950.copy(alpha = 0.6f)) else Slate900,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (isMe) (if (message.channel == MessageChannel.INTERNET) Cyan400.copy(alpha = 0.35f) else Amber400.copy(alpha = 0.35f))
                    else Slate800
                )
            ),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clickable { showEmojiBar = !showEmojiBar }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Reply quote if present
                message.replyToText?.let { reply ->
                    Surface(
                        color = Slate950.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "“$reply”",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // Media picture preview if present
                if (!message.attachmentUrl.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        AsyncImage(
                            model = message.attachmentUrl,
                            contentDescription = "Attachment",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Main message text
                Text(
                    text = message.text,
                    color = if (isMe) (if (message.channel == MessageChannel.INTERNET) Cyan200 else Amber100) else Slate100,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Footer: Timestamp + Delivery/Read indicators + Channel Tag
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Channel indicator
                    Text(
                        text = when (message.channel) {
                            MessageChannel.INTERNET -> "Internet"
                            MessageChannel.SMS -> "SIM"
                            MessageChannel.MMS -> "MMS"
                        },
                        color = if (message.channel == MessageChannel.INTERNET) Cyan400 else Amber400,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = message.timestamp,
                        color = Slate500,
                        fontSize = 10.sp
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageDeliveryStatus.READ -> {
                                Icon(Icons.Default.DoneAll, contentDescription = "Read", tint = Cyan400, modifier = Modifier.size(12.dp))
                            }
                            MessageDeliveryStatus.DELIVERED -> {
                                Icon(Icons.Default.DoneAll, contentDescription = "Delivered", tint = Slate400, modifier = Modifier.size(12.dp))
                            }
                            MessageDeliveryStatus.SENT -> {
                                Icon(Icons.Default.Check, contentDescription = "Sent", tint = Slate400, modifier = Modifier.size(12.dp))
                            }
                            MessageDeliveryStatus.SENDING -> {
                                Icon(Icons.Default.Schedule, contentDescription = "Pending sync", tint = Amber400, modifier = Modifier.size(12.dp))
                            }
                            MessageDeliveryStatus.FAILED -> {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Failed", tint = Red400, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Display reactions pill if present
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.reactions.forEach { r ->
                            Surface(
                                color = Slate900,
                                shape = RoundedCornerShape(10.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
                            ) {
                                Text(
                                    text = "${r.emoji} ${if (r.count > 1) r.count else ""}",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, members: List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name *", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Members (${selectedMembers.size} selected):", color = Slate300, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(contacts, key = { it.id }) { c ->
                        val isSelected = selectedMembers.contains(c.phone)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Slate800 else Slate950)
                                .clickable {
                                    if (isSelected) selectedMembers.remove(c.phone)
                                    else selectedMembers.add(c.phone)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedMembers.add(c.phone)
                                    else selectedMembers.remove(c.phone)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Cyan400,
                                    checkmarkColor = Slate950
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            ProfileAvatarPlaceholder(name = c.name, size = 32)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(c.phone, color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = Red400, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        errorText = "Please enter a group name"
                    } else if (selectedMembers.isEmpty()) {
                        errorText = "Please select at least 1 member"
                    } else {
                        onCreateGroup(groupName.trim(), selectedMembers.toList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                modifier = Modifier.testTag("btn_confirm_create_group")
            ) {
                Text("Create Group", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = Slate900
    )
}
