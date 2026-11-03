package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.ActiveCallScreenModal
import com.example.ui.components.AppScreen
import com.example.ui.components.NillBookBottomBar
import com.example.ui.components.NillBookTopBar
import com.example.ui.components.SmartCallOptionDialog
import com.example.ui.screens.*
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.NillBookTheme
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.security.E2EEMessagingManager
import com.example.data.security.PermissionManager
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Android notification channels
        NotificationHelper.initializeChannels(this)

        // Initialize Local Storage & Attachment manager
        LocalStorageManager.initialize(this)

        // Initialize Security & End-to-End Encryption Subsystems
        E2EEMessagingManager.initialize(this)
        PermissionManager.initialize(this)

        // Initialize Android ConnectivityManager NetworkCallback monitor
        NetworkMonitor.startMonitoring(this)

        setContent {
            NillBookTheme {
                val context = LocalContext.current
                val user by NillBookRepository.currentUser.collectAsState()
                val friendships by NillBookRepository.friendships.collectAsState()
                val conversations by NillBookRepository.conversations.collectAsState()
                val activeCallState by NillBookRepository.activeCallState.collectAsState()

                // Automatic network monitoring state
                val networkState by NetworkMonitor.networkState.collectAsState()
                val isOnline = networkState.isOnline

                var currentScreen by remember { mutableStateOf(AppScreen.SEARCH) }
                var showAuthModal by remember { mutableStateOf(false) }

                // In-App Alert banner state
                var inAppAlertText by remember { mutableStateOf<String?>(null) }

                // Global Smart Call Option Dialog Target: Pair(name, phone)
                var globalCallTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

                // Track if viewing another user's profile
                var viewingProfileUserId by remember { mutableStateOf<String?>(null) }

                // Quick add contact pre-filled parameters from search screen
                var pendingAddName by remember { mutableStateOf<String?>(null) }
                var pendingAddPhone by remember { mutableStateOf<String?>(null) }

                // Direct chat launch target from search or contacts
                var activeChatPhone by remember { mutableStateOf<String?>(null) }
                var activeChatName by remember { mutableStateOf<String?>(null) }

                val incomingRequestsCount = remember(friendships, user) {
                    val meId = user?.id ?: ""
                    friendships.count { it.addresseeId == meId && it.status == "PENDING" }
                }

                val unreadMessagesCount = remember(conversations) {
                    conversations.sumOf { it.unreadCount }
                }

                // System Permission Request on First Launch (Native Android OS pop-up only)
                val requestPermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    PermissionManager.markFirstLaunchPromptCompleted(context)
                    results.forEach { (perm, granted) ->
                        if (!granted) {
                            PermissionManager.setPermissionDeniedByUser(context, perm, true)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (!PermissionManager.hasCompletedFirstLaunchPrompt(context)) {
                        requestPermissionsLauncher.launch(
                            PermissionManager.getAllRequiredPermissionsArray()
                        )
                    }
                }

                // Automatic online sync without intrusive top notification banners
                var previousOnlineState by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(isOnline) {
                    if (previousOnlineState != null && previousOnlineState != isOnline) {
                        if (isOnline) {
                            NillBookRepository.syncOfflineData()
                            NillBookRepository.syncOfflineMessagesForCurrentUser()
                        }
                    }
                    previousOnlineState = isOnline
                }

                // Collect in-app alerts (Strictly auto-dismisses in 2 seconds)
                LaunchedEffect(Unit) {
                    NotificationHelper.inAppAlerts.collect { alert ->
                        inAppAlertText = "${alert.title}: ${alert.message}"
                        delay(2000L)
                        inAppAlertText = null
                    }
                }

                val isChatActive = currentScreen == AppScreen.MESSAGES && !activeChatPhone.isNullOrBlank()

                // Handle back press
                BackHandler(
                    enabled = isChatActive ||
                            (isOnline && currentScreen != AppScreen.SEARCH) ||
                            showAuthModal ||
                            viewingProfileUserId != null ||
                            globalCallTarget != null
                ) {
                    if (globalCallTarget != null) {
                        globalCallTarget = null
                    } else if (showAuthModal) {
                        showAuthModal = false
                    } else if (isChatActive) {
                        activeChatPhone = null
                        activeChatName = null
                    } else if (viewingProfileUserId != null) {
                        viewingProfileUserId = null
                    } else if (isOnline) {
                        currentScreen = AppScreen.SEARCH
                    }
                }

                if (user == null) {
                    // Mandatory Authentication Gate: User must either log in or create an account to enter
                    AuthScreen(
                        onDismiss = { /* Must authenticate to enter */ },
                        initialMode = "login"
                    )
                } else {
                    // Dual UI System: Separate Dedicated Online UI and Offline UI (Switch automatically based on connection)
                    Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = isOnline,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "dual_ui_system_switch"
                    ) { isOnlineActive ->
                        if (isOnlineActive) {
                            // Online UI - Full Power Mode (Cyan / Slate, Top Bar, Bottom Bar, 6 Navigation Hubs)
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Slate950,
                                contentWindowInsets = if (isChatActive) WindowInsets(0, 0, 0, 0) else androidx.compose.material3.ScaffoldDefaults.contentWindowInsets,
                                topBar = {
                                    if (!isChatActive) {
                                        NillBookTopBar(
                                            currentScreen = currentScreen,
                                            userName = user?.name ?: "NillBook",
                                            userPhoto = user?.photo,
                                            friendRequestsCount = incomingRequestsCount,
                                            isOnline = true,
                                            networkState = networkState,
                                            onToggleOnlineMode = {
                                                NetworkMonitor.toggleSimulationMode()
                                            },
                                            onProfileClick = {
                                                viewingProfileUserId = null
                                                currentScreen = AppScreen.PROFILE
                                            }
                                        )
                                    }
                                },
                                bottomBar = {
                                    if (!isChatActive) {
                                        NillBookBottomBar(
                                            currentScreen = currentScreen,
                                            friendRequestsCount = incomingRequestsCount,
                                            unreadMessagesCount = unreadMessagesCount,
                                            onScreenSelected = { screen ->
                                                if (screen == AppScreen.PROFILE) {
                                                    viewingProfileUserId = null
                                                }
                                                currentScreen = screen
                                            }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    AnimatedContent(
                                        targetState = currentScreen,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "screen_transition"
                                    ) { screen ->
                                        when (screen) {
                                            AppScreen.SEARCH -> {
                                                SearchScreen(
                                                    onAddContactDirectly = { name, phone ->
                                                        pendingAddName = name
                                                        pendingAddPhone = phone
                                                        currentScreen = AppScreen.CONTACTS
                                                    },
                                                    onOpenChat = { phone, name ->
                                                        activeChatPhone = phone
                                                        activeChatName = name
                                                        currentScreen = AppScreen.MESSAGES
                                                    },
                                                    onViewProfile = { uid ->
                                                        viewingProfileUserId = uid
                                                        currentScreen = AppScreen.PROFILE
                                                    },
                                                    onTriggerCall = { name, phone ->
                                                        globalCallTarget = Pair(name, phone)
                                                    }
                                                )
                                            }
                                            AppScreen.MESSAGES -> {
                                                MessagesScreen(
                                                    initialPhone = activeChatPhone,
                                                    initialName = activeChatName,
                                                    onOpenChat = { phone, name ->
                                                        activeChatPhone = phone
                                                        activeChatName = name
                                                    },
                                                    onClearTarget = {
                                                        activeChatPhone = null
                                                        activeChatName = null
                                                    },
                                                    onViewProfile = { uidOrPhone ->
                                                        viewingProfileUserId = uidOrPhone
                                                        currentScreen = AppScreen.PROFILE
                                                    },
                                                    onTriggerCall = { name, phone ->
                                                        globalCallTarget = Pair(name, phone)
                                                    }
                                                )
                                            }
                                            AppScreen.CONTACTS -> {
                                                ContactsScreen(
                                                    initialAddName = pendingAddName,
                                                    initialAddPhone = pendingAddPhone,
                                                    onOpenChat = { phone, name ->
                                                        activeChatPhone = phone
                                                        activeChatName = name
                                                        currentScreen = AppScreen.MESSAGES
                                                    },
                                                    onViewProfile = { phoneOrUid ->
                                                        viewingProfileUserId = phoneOrUid
                                                        currentScreen = AppScreen.PROFILE
                                                    }
                                                )
                                                LaunchedEffect(pendingAddName, pendingAddPhone) {
                                                    if (pendingAddName != null || pendingAddPhone != null) {
                                                        pendingAddName = null
                                                        pendingAddPhone = null
                                                    }
                                                }
                                            }
                                            AppScreen.FRIENDS -> {
                                                FriendsScreen(
                                                    onOpenChat = { phone, name ->
                                                        activeChatPhone = phone
                                                        activeChatName = name
                                                        currentScreen = AppScreen.MESSAGES
                                                    },
                                                    onViewProfile = { uid ->
                                                        viewingProfileUserId = uid
                                                        currentScreen = AppScreen.PROFILE
                                                    },
                                                    onTriggerCall = { name, phone ->
                                                        globalCallTarget = Pair(name, phone)
                                                    }
                                                )
                                            }
                                            AppScreen.CALLS -> {
                                                CallsScreen(
                                                    onTriggerCall = { name, phone ->
                                                        globalCallTarget = Pair(name, phone)
                                                    },
                                                    onOpenChat = { phone, name ->
                                                        activeChatPhone = phone
                                                        activeChatName = name
                                                        currentScreen = AppScreen.MESSAGES
                                                    },
                                                    onViewProfile = { phoneOrUid ->
                                                        viewingProfileUserId = phoneOrUid
                                                        currentScreen = AppScreen.PROFILE
                                                    }
                                                )
                                            }
                                            AppScreen.PROFILE -> {
                                                ProfileScreen(
                                                    targetUserId = viewingProfileUserId,
                                                    onOpenAuth = { showAuthModal = true },
                                                    onBack = {
                                                        viewingProfileUserId = null
                                                        currentScreen = AppScreen.SEARCH
                                                    },
                                                    onOpenChat = { phone, name ->
                                                        activeChatPhone = phone
                                                        activeChatName = name
                                                        currentScreen = AppScreen.MESSAGES
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Dedicated Offline UI - Amber / Copper SIM & Cellular Mode
                            OfflineDashboardScreen(
                                onTriggerCall = { name, phone ->
                                    globalCallTarget = Pair(name, phone)
                                }
                            )
                        }
                    }

                    // Shared In-App Toast Banner
                    inAppAlertText?.let { alert ->
                        Surface(
                            color = Slate900,
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Cyan400.copy(alpha = 0.6f))
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(alert, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Auth Modal
                    if (showAuthModal) {
                        AuthScreen(
                            onDismiss = { showAuthModal = false }
                        )
                    }


                    // Global Smart Call Dialog: 1. Internet Audio, 2. Internet Video, 3. Normal SIM Call
                    globalCallTarget?.let { (targetName, targetPhone) ->
                        SmartCallOptionDialog(
                            contactName = targetName,
                            phone = targetPhone,
                            isOnline = isOnline,
                            onDismiss = { globalCallTarget = null },
                            onInternetAudioCall = {
                                NillBookRepository.startInternetCall(targetName, targetPhone, CallMode.INTERNET_AUDIO)
                            },
                            onInternetVideoCall = {
                                NillBookRepository.startInternetCall(targetName, targetPhone, CallMode.INTERNET_VIDEO)
                            },
                            onSimCall = { simSlot ->
                                NillBookRepository.recordCall(
                                    name = targetName,
                                    phone = targetPhone,
                                    type = CallType.OUTGOING,
                                    mode = CallMode.SIM_CARRIER,
                                    simSlot = simSlot
                                )
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetPhone"))
                                context.startActivity(dialIntent)
                            }
                        )
                    }

                    // Active Real-Time Internet Call Screen Modal
                    activeCallState?.let { callState ->
                        ActiveCallScreenModal(
                            callState = callState,
                            onMuteToggle = { NillBookRepository.toggleCallMute() },
                            onSpeakerToggle = { NillBookRepository.toggleCallSpeaker() },
                            onVideoToggle = { NillBookRepository.toggleCallVideo() },
                            onEndCall = { NillBookRepository.endActiveCall() }
                        )
                    }
                }
            }
        }
    }
}
}


