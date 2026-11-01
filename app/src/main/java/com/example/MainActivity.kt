package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.NillBookRepository
import com.example.ui.components.AppScreen
import com.example.ui.components.NillBookBottomBar
import com.example.ui.components.NillBookTopBar
import com.example.ui.screens.*
import com.example.ui.theme.NillBookTheme
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NillBookTheme {
                val user by NillBookRepository.currentUser.collectAsState()
                val friendships by NillBookRepository.friendships.collectAsState()
                val conversations by NillBookRepository.conversations.collectAsState()

                var currentScreen by remember { mutableStateOf(AppScreen.SEARCH) }
                var showAuthModal by remember { mutableStateOf(false) }

                // Track if viewing another user's profile or own profile
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

                // Handle back press to always return to Search screen or exit profile detail
                BackHandler(enabled = currentScreen != AppScreen.SEARCH || showAuthModal || viewingProfileUserId != null) {
                    if (showAuthModal) {
                        showAuthModal = false
                    } else if (viewingProfileUserId != null) {
                        viewingProfileUserId = null
                    } else {
                        currentScreen = AppScreen.SEARCH
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Slate950,
                    topBar = {
                        NillBookTopBar(
                            currentScreen = currentScreen,
                            userName = user?.name ?: "NillBook",
                            friendRequestsCount = incomingRequestsCount,
                            onProfileClick = {
                                viewingProfileUserId = null
                                currentScreen = AppScreen.PROFILE
                            }
                        )
                    },
                    bottomBar = {
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
                                        }
                                    )
                                }
                                AppScreen.MESSAGES -> {
                                    MessagesScreen(
                                        initialPhone = activeChatPhone,
                                        initialName = activeChatName,
                                        onClearTarget = {
                                            activeChatPhone = null
                                            activeChatName = null
                                        },
                                        onViewProfile = { uidOrPhone ->
                                            viewingProfileUserId = uidOrPhone
                                            currentScreen = AppScreen.PROFILE
                                        }
                                    )
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

                        if (showAuthModal) {
                            AuthScreen(
                                onDismiss = { showAuthModal = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
