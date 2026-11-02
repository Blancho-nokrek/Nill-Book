package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.theme.*

@Composable
fun OfflineDashboardScreen(
    onTriggerCall: (name: String, phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val networkState by NetworkMonitor.networkState.collectAsState()
    val userContacts by NillBookRepository.userContacts.collectAsState()
    val conversations by NillBookRepository.conversations.collectAsState()
    val callLogs by NillBookRepository.callLogs.collectAsState()
    val pendingSyncItems by NillBookRepository.pendingSyncItems.collectAsState()
    val currentUser by NillBookRepository.currentUser.collectAsState()
    val activeDraft by NillBookRepository.activeDraftText.collectAsState()

    var activeOfflineTab by remember { mutableStateOf(OfflineScreen.CONTACTS) }
    var offlineActiveChatPhone by remember { mutableStateOf<String?>(null) }
    var offlineActiveChatName by remember { mutableStateOf<String?>(null) }

    val isOfflineChatActive = activeOfflineTab == OfflineScreen.SMS_MMS && !offlineActiveChatPhone.isNullOrBlank()

    BackHandler(enabled = isOfflineChatActive) {
        offlineActiveChatPhone = null
        offlineActiveChatName = null
    }

    // Dialog state for internet-only feature unavailable notice
    var showInternetFeatureNotice by remember { mutableStateOf<String?>(null) }

    // Carrier SIM call confirmation dialog state
    var simCallConfirmationTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Compose SMS sheet/modal state
    var composingSmsTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = if (isOfflineChatActive) WindowInsets(0, 0, 0, 0) else androidx.compose.material3.ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (!isOfflineChatActive) {
                OfflineTopBar(
                    networkState = networkState,
                    pendingCount = pendingSyncItems.size,
                    onToggleSimulation = { NetworkMonitor.toggleSimulationMode() }
                )
            }
        },
        bottomBar = {
            if (!isOfflineChatActive) {
                OfflineBottomBar(
                    currentTab = activeOfflineTab,
                    pendingCount = pendingSyncItems.size,
                    onTabSelected = { activeOfflineTab = it }
                )
            }
        },
        containerColor = Slate950
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeOfflineTab) {
                OfflineScreen.CONTACTS -> {
                    OfflineContactsTab(
                        contacts = userContacts,
                        onCallContact = { contact ->
                            simCallConfirmationTarget = contact.name to contact.phone
                        },
                        onSmsContact = { contact ->
                            offlineActiveChatPhone = contact.phone
                            offlineActiveChatName = contact.name
                            activeOfflineTab = OfflineScreen.SMS_MMS
                        },
                        onInternetFeatureAttempt = { feature ->
                            showInternetFeatureNotice = feature
                        }
                    )
                }
                OfflineScreen.SMS_MMS -> {
                    OfflineSmsTab(
                        conversations = conversations,
                        contacts = userContacts,
                        activePhone = offlineActiveChatPhone,
                        activeName = offlineActiveChatName,
                        onOpenChat = { phone, name ->
                            offlineActiveChatPhone = phone
                            offlineActiveChatName = name
                        },
                        onClearTarget = {
                            offlineActiveChatPhone = null
                            offlineActiveChatName = null
                        },
                        onTriggerCall = { name, phone ->
                            simCallConfirmationTarget = name to phone
                        }
                    )
                }
                OfflineScreen.SIM_CALLS -> {
                    OfflineSimCallsTab(
                        callLogs = callLogs,
                        onDialNumber = { number ->
                            simCallConfirmationTarget = "Carrier Call" to number
                        }
                    )
                }
                OfflineScreen.SAVED_FRIENDS -> {
                    OfflineSavedFriendsTab(
                        onCallFriend = { name, phone ->
                            simCallConfirmationTarget = name to phone
                        },
                        onSmsFriend = { name, phone ->
                            offlineActiveChatPhone = phone
                            offlineActiveChatName = name
                            activeOfflineTab = OfflineScreen.SMS_MMS
                        }
                    )
                }
                OfflineScreen.LOCAL_PROFILE -> {
                    OfflineProfileTab(
                        user = currentUser,
                        cachedContactsCount = userContacts.size,
                        pendingCount = pendingSyncItems.size,
                        onToggleSimulation = { NetworkMonitor.toggleSimulationMode() }
                    )
                }
            }

            // Internet Feature Unavailable Dialog
            showInternetFeatureNotice?.let { featureName ->
                AlertDialog(
                    onDismissRequest = { showInternetFeatureNotice = null },
                    containerColor = Slate900,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Amber400)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Internet Feature Unavailable", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            "$featureName requires an active internet connection. You are currently in Offline SIM Mode. You can make cellular SIM calls or send SMS through your mobile network instead.",
                            color = Slate300,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showInternetFeatureNotice = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950)
                        ) {
                            Text("Understood", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showInternetFeatureNotice = null
                                NetworkMonitor.setSimulationOverride(true)
                            }
                        ) {
                            Text("Simulate Online", color = Cyan400)
                        }
                    }
                )
            }

            // SIM Call Confirmation Dialog
            simCallConfirmationTarget?.let { (name, phone) ->
                AlertDialog(
                    onDismissRequest = { simCallConfirmationTarget = null },
                    containerColor = Slate900,
                    icon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Amber400) },
                    title = {
                        Text("Initiate SIM / Carrier Call", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text("Call $name ($phone) using your device's cellular SIM card?", color = Slate200, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Amber400.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Notice: Standard carrier voice minutes or rates apply according to your telecom provider (e.g. Grameenphone, Robi, Banglalink, Teletalk, Airtel).",
                                    color = Amber200,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val target = phone
                                simCallConfirmationTarget = null
                                NillBookRepository.recordCall(
                                    name = name,
                                    phone = target,
                                    type = CallType.OUTGOING,
                                    mode = CallMode.SIM_CARRIER,
                                    simSlot = "SIM 1",
                                    duration = "Active"
                                )
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${Uri.encode(target)}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(dialIntent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950)
                        ) {
                            Text("Call via SIM", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { simCallConfirmationTarget = null }) {
                            Text("Cancel", color = Slate400)
                        }
                    }
                )
            }

            // Compose SMS Modal
            composingSmsTarget?.let { (name, phone) ->
                var smsBody by remember { mutableStateOf(activeDraft.ifBlank { "" }) }
                var selectedSlot by remember { mutableStateOf("SIM 1") }

                AlertDialog(
                    onDismissRequest = { composingSmsTarget = null },
                    containerColor = Slate900,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Amber400)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Carrier SMS to $name", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text("Recipient: $phone", color = Slate400, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = smsBody,
                                onValueChange = {
                                    smsBody = it
                                    NillBookRepository.setActiveDraft(it)
                                },
                                placeholder = { Text("Type SMS text...", color = Slate500) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Amber400,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${smsBody.length} / 160 chars (${if (smsBody.length <= 160) 1 else (smsBody.length / 153) + 1} SMS)",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("SIM 1", "SIM 2").forEach { slot ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { selectedSlot = slot },
                                            color = if (selectedSlot == slot) Amber400 else Slate800
                                        ) {
                                            Text(
                                                slot,
                                                color = if (selectedSlot == slot) Slate950 else Slate400,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Standard carrier SMS rates apply. Sent directly through your phone's cellular SIM.",
                                color = Amber200,
                                fontSize = 11.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (smsBody.isNotBlank()) {
                                    NillBookRepository.sendMessage(
                                        contactName = name,
                                        phone = phone,
                                        text = smsBody,
                                        channel = MessageChannel.SMS
                                    )
                                    NillBookRepository.setActiveDraft("")
                                    composingSmsTarget = null
                                    NotificationHelper.showNotification(
                                        context,
                                        "SMS Sent",
                                        "Message dispatched via $selectedSlot to $phone"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950),
                            enabled = smsBody.isNotBlank()
                        ) {
                            Text("Send via $selectedSlot", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { composingSmsTarget = null }) {
                            Text("Cancel", color = Slate400)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineTopBar(
    networkState: NetworkState,
    pendingCount: Int,
    onToggleSimulation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Slate900,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Amber400),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Slate950, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = LanguageManager.t("app_name"),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "OFFLINE SIM MODE",
                            color = Amber400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Action button: Switch to Online Simulator
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleSimulation() }
                        .testTag("simulate_online_button"),
                    color = Cyan400.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Cyan400, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Simulate Online",
                            color = Cyan400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Offline Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Amber950.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Amber400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No Internet Connection • Cellular SIM & Local Mode Active",
                            color = Amber200,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (pendingCount > 0) {
                        Surface(
                            color = Amber400,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$pendingCount pending",
                                color = Slate950,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineBottomBar(
    currentTab: OfflineScreen,
    pendingCount: Int,
    onTabSelected: (OfflineScreen) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Slate900,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OfflineNavItem(
                icon = Icons.Default.Contacts,
                label = "Contacts",
                isSelected = currentTab == OfflineScreen.CONTACTS,
                onClick = { onTabSelected(OfflineScreen.CONTACTS) },
                testTag = "offline_tab_contacts"
            )

            OfflineNavItem(
                icon = Icons.AutoMirrored.Filled.Message,
                label = "SMS / MMS",
                isSelected = currentTab == OfflineScreen.SMS_MMS,
                onClick = { onTabSelected(OfflineScreen.SMS_MMS) },
                testTag = "offline_tab_sms"
            )

            OfflineNavItem(
                icon = Icons.Default.Dialpad,
                label = "Calls",
                isSelected = currentTab == OfflineScreen.SIM_CALLS,
                onClick = { onTabSelected(OfflineScreen.SIM_CALLS) },
                testTag = "offline_tab_calls"
            )

            OfflineNavItem(
                icon = Icons.Default.People,
                label = "Friends",
                badgeCount = 0,
                isSelected = currentTab == OfflineScreen.SAVED_FRIENDS,
                onClick = { onTabSelected(OfflineScreen.SAVED_FRIENDS) },
                testTag = "offline_tab_friends"
            )

            OfflineNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == OfflineScreen.LOCAL_PROFILE,
                onClick = { onTabSelected(OfflineScreen.LOCAL_PROFILE) },
                testTag = "offline_tab_profile"
            )
        }
    }
}

@Composable
fun OfflineNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Amber400 else Slate400,
                modifier = Modifier.size(24.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Amber400),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Slate950,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Amber400 else Slate400,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun OfflineContactsTab(
    contacts: List<Contact>,
    onCallContact: (Contact) -> Unit,
    onSmsContact: (Contact) -> Unit,
    onInternetFeatureAttempt: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val filteredContacts = remember(contacts, searchQuery, showOnlyFavorites) {
        contacts.filter { contact ->
            val matchesSearch = searchQuery.isBlank() ||
                    contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phone.contains(searchQuery)
            val matchesFav = !showOnlyFavorites || contact.isFavorite
            matchesSearch && matchesFav
        }.sortedBy { it.name }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Search & Filter header
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_contact_search"),
                placeholder = { Text("Search local contacts by name or number...", color = Slate500, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Amber400) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Amber400,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Filters & Internet VoIP Notice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showOnlyFavorites = false },
                        color = if (!showOnlyFavorites) Amber400 else Slate800
                    ) {
                        Text(
                            "All (${contacts.size})",
                            color = if (!showOnlyFavorites) Slate950 else Slate300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showOnlyFavorites = true },
                        color = if (showOnlyFavorites) Amber400 else Slate800
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = if (showOnlyFavorites) Slate950 else Amber400, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Favorites",
                                color = if (showOnlyFavorites) Slate950 else Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Internet call hint
                TextButton(onClick = { onInternetFeatureAttempt("Internet Video & Audio Calls") }) {
                    Text("Internet Calls?", color = Slate500, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Contact Items
        items(filteredContacts, key = { it.id }) { contact ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatarPlaceholder(name = contact.name, size = 44)

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (contact.isFavorite) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = Amber400, modifier = Modifier.size(13.dp))
                            }
                        }
                        Text(
                            text = contact.phone,
                            color = Slate400,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (contact.syncStatus == SyncStatus.PENDING_SYNC) {
                            Text(
                                text = "• Saved locally (Pending cloud sync)",
                                color = Amber200,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Action buttons: SIM Call and SMS
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onSmsContact(contact) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Slate800)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS", tint = Amber400, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { onCallContact(contact) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Amber400)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "SIM Call", tint = Slate950, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineSmsTab(
    conversations: List<ChatConversation>,
    contacts: List<Contact>,
    activePhone: String?,
    activeName: String?,
    onOpenChat: (phone: String, name: String) -> Unit,
    onClearTarget: () -> Unit,
    onTriggerCall: (name: String, phone: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showNewSmsDialog by remember { mutableStateOf(false) }

    val activeConv = remember(conversations, activePhone) {
        conversations.find { it.phone == activePhone }
    }

    val filteredConversations = remember(conversations, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) conversations
        else conversations.filter {
            it.contactName.lowercase().contains(q) || it.phone.contains(q) || it.lastMessage.lowercase().contains(q)
        }
    }

    if (activePhone != null) {
        ChatDetailView(
            contactName = activeName ?: activePhone,
            phone = activePhone,
            messages = activeConv?.messages ?: emptyList(),
            isOnline = false,
            onBack = onClearTarget,
            onViewProfile = { /* View profile */ },
            onTriggerCallDialog = {
                onTriggerCall(activeName ?: activePhone, activePhone)
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
            ) {
                // Pinned Search Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search carrier SMS messages...", color = Slate500, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Amber400, modifier = Modifier.size(18.dp))
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
                            focusedBorderColor = Amber400,
                            unfocusedBorderColor = Slate800,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_sms_search_box")
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredConversations.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Slate600, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No carrier messages yet", color = Slate300, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap the message button to compose a carrier SMS", color = Slate500, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(filteredConversations, key = { it.phone }) { conv ->
                            EnhancedConversationCard(
                                conversation = conv,
                                onClick = { onOpenChat(conv.phone, conv.contactName) },
                                onAvatarClick = { onOpenChat(conv.phone, conv.contactName) },
                                onTogglePin = { NillBookRepository.togglePinConversation(conv.phone) },
                                onToggleMute = { NillBookRepository.toggleMuteConversation(conv.phone) }
                            )
                        }
                    }
                }
            }

            // WhatsApp-style Floating Action Button for Offline SMS
            FloatingActionButton(
                onClick = { showNewSmsDialog = true },
                containerColor = Amber400,
                contentColor = Slate950,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
                    .size(56.dp)
                    .testTag("offline_btn_new_sms")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "New Carrier SMS",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (showNewSmsDialog) {
            AlertDialog(
                onDismissRequest = { showNewSmsDialog = false },
                title = { Text("Compose Carrier SMS", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Select a contact to message via SIM:", color = Slate400, fontSize = 13.sp)
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
                                            showNewSmsDialog = false
                                            onOpenChat(c.phone, c.name)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatarPlaceholder(name = c.name, size = 38)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(c.phone, color = Amber400, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Amber400, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNewSmsDialog = false }) {
                        Text("Close", color = Amber400)
                    }
                },
                containerColor = Slate900
            )
        }
    }
}

@Composable
fun OfflineSimCallsTab(
    callLogs: List<CallLogEntry>,
    onDialNumber: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var dialDigits by remember { mutableStateOf("") }

    val filteredLogs = remember(callLogs, selectedFilter) {
        when (selectedFilter) {
            "MISSED" -> callLogs.filter { it.type == CallType.MISSED }
            "INCOMING" -> callLogs.filter { it.type == CallType.INCOMING }
            "OUTGOING" -> callLogs.filter { it.type == CallType.OUTGOING }
            "SIM" -> callLogs.filter { it.mode == CallMode.SIM_CARRIER }
            else -> callLogs
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            // Pinned Section: Header & Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate950)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CALLS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (callLogs.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Red400),
                            modifier = Modifier.testTag("offline_clear_calls_btn")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear History", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pinned Filter Chips (All, Missed, Incoming, Outgoing, SIM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All (${callLogs.size})",
                        "MISSED" to "Missed",
                        "INCOMING" to "Incoming",
                        "OUTGOING" to "Outgoing",
                        "SIM" to "SIM"
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Amber400,
                                selectedLabelColor = Slate950,
                                containerColor = Slate900,
                                labelColor = Slate300
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Amber400 else Slate800
                            )
                        )
                    }
                }
            }

            // Call log items
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhoneMissed, contentDescription = null, tint = Slate600, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No calls in this category", color = Slate300, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    items(filteredLogs, key = { it.id }) { log ->
                        val photoUrl = remember(log.phone) {
                            NillBookRepository.getPhotoForPhone(log.phone)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileAvatarPlaceholder(
                                    name = log.name,
                                    size = 42,
                                    photoUrl = photoUrl
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            log.name,
                                            color = if (log.type == CallType.MISSED) Red400 else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Call mode badge
                                        val (modeText, modeBg, modeFg) = when (log.mode) {
                                            CallMode.INTERNET_AUDIO -> Triple("Internet", Emerald950, Emerald400)
                                            CallMode.INTERNET_VIDEO -> Triple("Video", Cyan950, Cyan400)
                                            CallMode.SIM_CARRIER -> Triple(log.simSlot.ifEmpty { "SIM 1" }, Amber950, Amber400)
                                        }

                                        Surface(
                                            color = modeBg,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                modeText,
                                                color = modeFg,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val (icon, tint) = when (log.type) {
                                            CallType.INCOMING -> Icons.Default.CallReceived to Emerald400
                                            CallType.OUTGOING -> Icons.Default.CallMade to Cyan400
                                            CallType.MISSED -> Icons.Default.CallMissed to Red500
                                        }
                                        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(log.phone, color = Slate400, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(log.timestamp, color = Slate500, fontSize = 11.sp)
                                        if (log.duration.isNotEmpty()) {
                                            Text(" • ${log.duration}", color = Slate500, fontSize = 11.sp)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onDialNumber(log.phone) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Amber400)
                                        .testTag("offline_redial_${log.id}")
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Redial", tint = Slate950, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to toggle Dialpad
        FloatingActionButton(
            onClick = { showDialpad = !showDialpad },
            containerColor = Amber400,
            contentColor = Slate950,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .size(56.dp)
                .testTag("offline_toggle_dialpad")
        ) {
            Icon(Icons.Default.Dialpad, contentDescription = "Dialpad", modifier = Modifier.size(26.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear SIM Call History?", color = Color.White) },
            text = { Text("This will permanently remove your offline SIM call logs from this device.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.clearCallLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel", color = Slate400) }
            },
            containerColor = Slate900
        )
    }

    if (showDialpad) {
        AlertDialog(
            onDismissRequest = { showDialpad = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SIM Dialer", color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showDialpad = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dialDigits.ifEmpty { "Enter phone number" },
                            color = if (dialDigits.isEmpty()) Slate600 else Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        if (dialDigits.isNotEmpty()) {
                            IconButton(onClick = { dialDigits = dialDigits.dropLast(1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Backspace", tint = Slate400)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val keypad = listOf(
                        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                        listOf("*" to "", "0" to "+", "#" to "")
                    )
                    keypad.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { (digit, sub) ->
                                Surface(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .clickable { dialDigits += digit },
                                    color = Slate800
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(digit, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        if (sub.isNotEmpty()) Text(sub, color = Slate400, fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialDigits.isNotBlank()) {
                            showDialpad = false
                            onDialNumber(dialDigits)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950),
                    enabled = dialDigits.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call via Cellular SIM", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun OfflineSavedFriendsTab(
    onCallFriend: (name: String, phone: String) -> Unit,
    onSmsFriend: (name: String, phone: String) -> Unit
) {
    val friendships by NillBookRepository.friendships.collectAsState()
    val friendsList = remember(friendships) { NillBookRepository.getFriendsList().filter { it.status == "friends" } }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFriends = remember(friendsList, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) friendsList
        else friendsList.filter {
            it.name.lowercase().contains(q) || (it.phone != null && it.phone.contains(q))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Amber400.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Amber400, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Saved Friends (Offline Cache)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Cached from your NillBook online account. You can send SIM SMS or make cellular calls.", color = Slate400, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_friends_search"),
                placeholder = { Text("Search saved friends...", color = Slate500, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Amber400) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Amber400,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        if (filteredFriends.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.GroupOff, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (searchQuery.isEmpty()) "No saved friends in cache" else "No friends matching \"$searchQuery\"",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredFriends, key = { it.userId }) { friend ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatarPlaceholder(name = friend.name, size = 42)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friend.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(friend.phone ?: "Phone hidden", color = Amber400, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Cached Friend • NillBook Account", color = Slate500, fontSize = 10.sp)
                        }

                        if (!friend.phone.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onSmsFriend(friend.name, friend.phone) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Slate800)
                                        .testTag("offline_friend_sms_${friend.userId}")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS", tint = Amber400, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onCallFriend(friend.name, friend.phone) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Amber400)
                                        .testTag("offline_friend_call_${friend.userId}")
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Slate950, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineProfileTab(
    user: User?,
    cachedContactsCount: Int,
    pendingCount: Int,
    onToggleSimulation: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            // Connection & Operation Mode Switch (Online / Offline)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("offline_connection_mode_card"),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CONNECTION & OPERATION MODE",
                        color = Amber400,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Amber400.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = Amber400,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Offline SIM Mode Active",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Switch ON to connect to Internet and activate Online Mode.",
                                    color = Slate400,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = false,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    NetworkMonitor.setSimulationOverride(true)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Slate950,
                                checkedTrackColor = Cyan400,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            ),
                            modifier = Modifier.testTag("offline_profile_online_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { NetworkMonitor.setSimulationOverride(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("offline_switch_to_online_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch to Online Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            // Local Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatarPlaceholder(
                            name = user?.name ?: "User",
                            size = 54,
                            photoUrl = user?.photo
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(user?.name ?: "User", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(user?.phone ?: "", color = Amber400, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("Locally Cached Profile", color = Slate400, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate800)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Device SIM & Hardware Status", color = Slate200, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SIM Slot 1", color = Slate400, fontSize = 12.sp)
                        Text("Active (Cellular Voice + SMS Ready)", color = Emerald400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SIM Slot 2", color = Slate400, fontSize = 12.sp)
                        Text("Ready (Dual SIM Supported)", color = Slate300, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate800)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Local Offline Storage", color = Slate200, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cached Contacts", color = Slate400, fontSize = 12.sp)
                        Text("$cachedContactsCount items", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pending Sync Changes", color = Slate400, fontSize = 12.sp)
                        Text("$pendingCount items", color = if (pendingCount > 0) Amber400 else Slate400, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulation Switcher Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = Cyan400)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dual UI System Simulator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Test automatic interface switching between the full Online UI and the SIM Offline UI in real-time.",
                        color = Slate300,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onToggleSimulation() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch to Online UI (Simulate Internet)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
