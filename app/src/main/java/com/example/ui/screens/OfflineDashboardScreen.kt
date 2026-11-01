package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    // Dialog state for internet-only feature unavailable notice
    var showInternetFeatureNotice by remember { mutableStateOf<String?>(null) }

    // Carrier SIM call confirmation dialog state
    var simCallConfirmationTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Compose SMS sheet/modal state
    var composingSmsTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OfflineTopBar(
                networkState = networkState,
                pendingCount = pendingSyncItems.size,
                onToggleSimulation = { NetworkMonitor.toggleSimulationMode() }
            )
        },
        bottomBar = {
            OfflineBottomBar(
                currentTab = activeOfflineTab,
                pendingCount = pendingSyncItems.size,
                onTabSelected = { activeOfflineTab = it }
            )
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
                            composingSmsTarget = contact.name to contact.phone
                        },
                        onInternetFeatureAttempt = { feature ->
                            showInternetFeatureNotice = feature
                        }
                    )
                }
                OfflineScreen.SMS_MMS -> {
                    OfflineSmsTab(
                        conversations = conversations,
                        activeDraft = activeDraft,
                        onUpdateDraft = { NillBookRepository.setActiveDraft(it) },
                        onSendSms = { targetPhone, text, simSlot ->
                            NillBookRepository.sendMessage(
                                phone = targetPhone,
                                contactName = targetPhone,
                                text = text,
                                channel = MessageChannel.SMS
                            )
                            NillBookRepository.setActiveDraft("")
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
                OfflineScreen.PENDING_SYNC -> {
                    OfflinePendingSyncTab(
                        pendingItems = pendingSyncItems,
                        onRetrySync = {
                            val count = NillBookRepository.syncOfflineData()
                            NotificationHelper.showNotification(
                                context,
                                "Sync Attempt",
                                if (count > 0) "Synchronized $count offline items." else "All offline items are already synchronized."
                            )
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
                label = "SIM Calls",
                isSelected = currentTab == OfflineScreen.SIM_CALLS,
                onClick = { onTabSelected(OfflineScreen.SIM_CALLS) },
                testTag = "offline_tab_calls"
            )

            OfflineNavItem(
                icon = Icons.Default.SyncProblem,
                label = "Sync Queue",
                badgeCount = pendingCount,
                isSelected = currentTab == OfflineScreen.PENDING_SYNC,
                onClick = { onTabSelected(OfflineScreen.PENDING_SYNC) },
                testTag = "offline_tab_sync"
            )

            OfflineNavItem(
                icon = Icons.Default.PersonOutline,
                label = "Local Info",
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
    activeDraft: String,
    onUpdateDraft: (String) -> Unit,
    onSendSms: (phone: String, text: String, simSlot: String) -> Unit
) {
    val context = LocalContext.current
    var targetPhone by remember { mutableStateOf("+8801700000000") }
    var smsText by remember { mutableStateOf(activeDraft) }
    var selectedSim by remember { mutableStateOf("SIM 1") }

    // Synchronize draft
    LaunchedEffect(activeDraft) {
        if (smsText.isEmpty() && activeDraft.isNotEmpty()) {
            smsText = activeDraft
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // SMS Compose Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Carrier SMS / MMS",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Amber400.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "SIM Network Active",
                                color = Amber400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetPhone,
                        onValueChange = { targetPhone = it },
                        label = { Text("Recipient Phone Number", color = Slate400, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = smsText,
                        onValueChange = {
                            smsText = it
                            onUpdateDraft(it)
                        },
                        label = { Text("SMS Message Text", color = Slate400, fontSize = 12.sp) },
                        placeholder = { Text("Draft preserved across modes...", color = Slate600) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Character count & SIM slot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${smsText.length} / 160 chars (${if (smsText.length <= 160) 1 else (smsText.length / 153) + 1} SMS)",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SIM 1", "SIM 2").forEach { slot ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSim = slot },
                                    color = if (selectedSim == slot) Amber400 else Slate800
                                ) {
                                    Text(
                                        slot,
                                        color = if (selectedSim == slot) Slate950 else Slate300,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Send SMS Button
                    Button(
                        onClick = {
                            if (smsText.isNotBlank()) {
                                onSendSms(targetPhone, smsText, selectedSim)
                                smsText = ""
                                NotificationHelper.showNotification(
                                    context,
                                    "Carrier SMS Dispatched",
                                    "Sent to $targetPhone via $selectedSim"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950),
                        enabled = smsText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send via $selectedSim (Standard Carrier Rates)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Local Message History",
                color = Slate300,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // List of conversations
        items(conversations) { conv ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatarPlaceholder(name = conv.contactName, size = 42)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(conv.contactName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(conv.lastTimestamp, color = Slate500, fontSize = 11.sp)
                        }
                        Text(conv.lastMessage, color = Slate400, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineSimCallsTab(
    callLogs: List<CallLogEntry>,
    onDialNumber: (String) -> Unit
) {
    var dialDigits by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Dialpad Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display digits
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dialDigits.ifEmpty { "Enter phone number" },
                            color = if (dialDigits.isEmpty()) Slate600 else Color.White,
                            fontSize = 24.sp,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialpad 3x4 grid
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
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { (digit, sub) ->
                                Surface(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable { dialDigits += digit },
                                    color = Slate800
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(digit, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        if (sub.isNotEmpty()) {
                                            Text(sub, color = Slate400, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Call Button
                    Button(
                        onClick = {
                            if (dialDigits.isNotBlank()) {
                                onDialNumber(dialDigits)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950),
                        shape = RoundedCornerShape(25.dp),
                        enabled = dialDigits.isNotBlank()
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call via Cellular SIM", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Carrier Calls",
                color = Slate300,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Carrier call logs
        items(callLogs) { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (log.type == CallType.MISSED) Red500.copy(alpha = 0.2f) else Emerald400.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (log.type) {
                                CallType.INCOMING -> Icons.Default.CallReceived
                                CallType.OUTGOING -> Icons.Default.CallMade
                                CallType.MISSED -> Icons.Default.CallMissed
                            },
                            contentDescription = null,
                            tint = if (log.type == CallType.MISSED) Red500 else Emerald400,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(log.phone, color = Slate400, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("${log.simSlot} • ${log.timestamp}", color = Slate500, fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = { onDialNumber(log.phone) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Amber400)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Redial", tint = Slate950, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OfflinePendingSyncTab(
    pendingItems: List<PendingSyncItem>,
    onRetrySync: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SyncProblem, contentDescription = null, tint = Amber400)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pending Cloud Synchronization",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The following items were created or modified while in Offline SIM Mode. Nill Book will automatically synchronize them to the cloud when validated internet connectivity is restored.",
                        color = Slate300,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onRetrySync() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Synchronization Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Queued Items (${pendingItems.size})",
                color = Slate300,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (pendingItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All Offline Data Synced", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("No queued changes waiting for upload.", color = Slate400, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(pendingItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Amber400.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (item.type) {
                                    SyncItemType.CONTACT -> Icons.Default.PersonAdd
                                    SyncItemType.MESSAGE -> Icons.AutoMirrored.Filled.Send
                                    SyncItemType.PROFILE_UPDATE -> Icons.Default.Edit
                                    SyncItemType.FRIEND_ACTION -> Icons.Default.GroupAdd
                                },
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(item.subtitle, color = Slate400, fontSize = 12.sp)
                            Text("Queued: ${item.timestamp}", color = Slate500, fontSize = 10.sp)
                        }

                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "QUEUED",
                                color = Amber200,
                                fontSize = 9.sp,
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
            // Local Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatarPlaceholder(name = user?.name ?: "User", size = 54)
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
