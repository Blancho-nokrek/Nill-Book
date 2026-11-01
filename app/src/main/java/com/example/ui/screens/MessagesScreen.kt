package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatConversation
import com.example.data.ChatMessage
import com.example.data.NillBookRepository
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    initialPhone: String? = null,
    initialName: String? = null,
    onClearTarget: (() -> Unit)? = null,
    onViewProfile: ((userIdOrPhone: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val conversations by NillBookRepository.conversations.collectAsState()
    val contacts by NillBookRepository.userContacts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeConversationPhone by remember { mutableStateOf<String?>(initialPhone) }
    var activeConversationName by remember { mutableStateOf(initialName ?: "Contact") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    // If initial params passed, set active conversation
    LaunchedEffect(initialPhone, initialName) {
        if (!initialPhone.isNullOrBlank()) {
            activeConversationPhone = initialPhone
            activeConversationName = initialName ?: initialPhone
        }
    }

    val activeConversation = remember(conversations, activeConversationPhone) {
        conversations.find { it.phone == activeConversationPhone }
    }

    // Filtered conversations based on search box
    val filteredConversations = remember(conversations, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) conversations
        else conversations.filter {
            it.contactName.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.lastMessage.lowercase().contains(q)
        }
    }

    // Filtered contacts when searching
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

    // Handle Back Press in active chat to return to chat list
    BackHandler(enabled = activeConversationPhone != null) {
        activeConversationPhone = null
        onClearTarget?.invoke()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        if (activeConversationPhone != null) {
            // Clean In-App Chat Detail View
            ChatDetailView(
                contactName = activeConversationName,
                phone = activeConversationPhone!!,
                messages = activeConversation?.messages ?: emptyList(),
                onBack = {
                    activeConversationPhone = null
                    onClearTarget?.invoke()
                },
                onViewProfile = {
                    onViewProfile?.invoke(activeConversationPhone!!)
                },
                onSendMessage = { text ->
                    NillBookRepository.sendMessage(activeConversationPhone!!, activeConversationName, text)
                },
                onCall = {
                    NillBookRepository.recordCall(activeConversationName, activeConversationPhone!!)
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$activeConversationPhone"))
                    context.startActivity(dialIntent)
                }
            )
        } else {
            // Neat & Clean Conversations List
            Scaffold(
                containerColor = Slate950,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showNewChatDialog = true },
                        containerColor = Cyan400,
                        contentColor = Slate950,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(bottom = 68.dp)
                            .testTag("fab_new_chat")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Message")
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title Header
                    item {
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = "MESSAGES",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Direct caller messaging & chat history",
                                color = Slate400,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Neat & Clean Search Box
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages or contacts...", color = Slate500, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Cyan400, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
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
                    }

                    // Matching Directory Contacts if Search Query present
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

                    // Conversations Header
                    if (filteredConversations.isNotEmpty()) {
                        item {
                            Text(
                                text = if (searchQuery.isEmpty()) "RECENT CHATS (${filteredConversations.size})" else "MATCHING CHATS (${filteredConversations.size})",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Empty State
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
                                    text = if (searchQuery.isEmpty()) "No messages yet" else "No chats matching \"$searchQuery\"",
                                    color = Slate300,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start a message by tapping the chat button below",
                                    color = Slate500,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(filteredConversations, key = { it.phone }) { conv ->
                            CleanConversationCard(
                                conversation = conv,
                                onClick = {
                                    activeConversationPhone = conv.phone
                                    activeConversationName = conv.contactName
                                },
                                onAvatarClick = {
                                    onViewProfile?.invoke(conv.phone)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // New Chat Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Message", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select a contact to start messaging:", color = Slate400, fontSize = 13.sp)
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
}

@Composable
fun CleanConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("conversation_${conversation.phone}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatarPlaceholder(
                name = conversation.contactName,
                size = 48,
                modifier = if (onAvatarClick != null) Modifier.clickable { onAvatarClick() } else Modifier
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.contactName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onCall: () -> Unit,
    onViewProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showChatMenu by remember { mutableStateOf(false) }
    var showVideoCallDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var chatNotificationToast by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onViewProfile != null) { onViewProfile?.invoke() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        ProfileAvatarPlaceholder(name = contactName, size = 36)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(contactName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (isMuted) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = "Muted",
                                        tint = Slate500,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(phone, color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Audio Call button
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.testTag("chat_audio_call_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = Emerald400)
                    }

                    // Video Call button
                    IconButton(
                        onClick = { showVideoCallDialog = true },
                        modifier = Modifier.testTag("chat_video_call_btn")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Cyan400)
                    }

                    // 3-dot Menu settings
                    Box {
                        IconButton(
                            onClick = { showChatMenu = true },
                            modifier = Modifier.testTag("chat_more_options_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Slate200)
                        }

                        DropdownMenu(
                            expanded = showChatMenu,
                            onDismissRequest = { showChatMenu = false },
                            modifier = Modifier.background(Slate900)
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Profile", color = Color.White, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showChatMenu = false
                                    onViewProfile?.invoke()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isMuted) "Unmute Notifications" else "Mute Notifications",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMuted = !isMuted
                                    chatNotificationToast = if (isMuted) "Notifications muted for $contactName" else "Notifications unmuted"
                                    showChatMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Clear Messages", color = Red400, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Red400, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showChatMenu = false
                                    showClearConfirm = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Block & Report", color = Red400, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = Red400, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showChatMenu = false
                                    showBlockConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        bottomBar = {
            Surface(
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Write a message...", color = Slate500, fontSize = 14.sp) },
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate800,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                                scope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Cyan400)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Slate950,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            color = Slate900.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Direct in-app messaging",
                                color = Slate500,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                }
            }

            // Feedback toast notification
            if (chatNotificationToast != null) {
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(chatNotificationToast ?: "", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { chatNotificationToast = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Video Call Dialog
    if (showVideoCallDialog) {
        VideoCallDialog(
            contactName = contactName,
            phone = phone,
            onDismiss = { showVideoCallDialog = false }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Chat Messages?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all message history with $contactName? This action cannot be undone.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.clearConversation(phone)
                        showClearConfirm = false
                        chatNotificationToast = "Chat history cleared"
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

    // Block & Report Dialog
    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block & Report Contact?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Block $phone from contacting you? You will no longer receive calls or messages from this number.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirm = false
                        chatNotificationToast = "$phone has been blocked"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Slate950)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun VideoCallDialog(
    contactName: String,
    phone: String,
    onDismiss: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Slate950
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background simulated video frame or dark slate ambient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Slate900, Slate950, Slate900)
                            )
                        )
                )

                // Top bar with call status
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Slate900.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HD Video • Connected 00:24",
                                color = Emerald400,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    ProfileAvatarPlaceholder(name = contactName, size = 96)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(contactName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(phone, color = Cyan400, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }

                // Floating "My Camera" preview picture-in-picture
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 110.dp, end = 20.dp)
                        .size(width = 96.dp, height = 136.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Cyan400)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isCameraOff) {
                            Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Red400, modifier = Modifier.size(28.dp))
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Cyan400, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("You", color = Slate400, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Bottom Call Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Slate900.copy(alpha = 0.95f))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Mute toggle
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Red400 else Slate800)
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMuted) Slate950 else Color.White
                            )
                        }

                        // Camera toggle
                        IconButton(
                            onClick = { isCameraOff = !isCameraOff },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isCameraOff) Red400 else Slate800)
                        ) {
                            Icon(
                                if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "Camera",
                                tint = if (isCameraOff) Slate950 else Color.White
                            )
                        }

                        // Switch Camera
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Slate800)
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                        }

                        // End Call Button (Red)
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Red400)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Slate950, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isMe = message.isFromMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) Cyan950 else Slate900,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(if (isMe) Cyan400.copy(alpha = 0.3f) else Slate800)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = if (isMe) Cyan300 else Slate100,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        color = Slate500,
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = Cyan400,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
