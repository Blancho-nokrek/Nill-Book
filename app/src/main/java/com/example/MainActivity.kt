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

                var currentScreen by remember { mutableStateOf(AppScreen.SEARCH) }
                var showAuthModal by remember { mutableStateOf(false) }

                // Quick add contact pre-filled parameters from search screen
                var pendingAddName by remember { mutableStateOf<String?>(null) }
                var pendingAddPhone by remember { mutableStateOf<String?>(null) }

                val incomingRequestsCount = remember(friendships, user) {
                    val meId = user?.id ?: ""
                    friendships.count { it.addresseeId == meId && it.status == "PENDING" }
                }

                // Handle back press to return to Search if on secondary screen
                BackHandler(enabled = currentScreen != AppScreen.SEARCH || showAuthModal) {
                    if (showAuthModal) {
                        showAuthModal = false
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
                            onProfileClick = { currentScreen = AppScreen.PROFILE }
                        )
                    },
                    bottomBar = {
                        NillBookBottomBar(
                            currentScreen = currentScreen,
                            friendRequestsCount = incomingRequestsCount,
                            onScreenSelected = { screen ->
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
                                        }
                                    )
                                }
                                AppScreen.CONTACTS -> {
                                    ContactsScreen(
                                        initialAddName = pendingAddName,
                                        initialAddPhone = pendingAddPhone
                                    )
                                    // Reset pending after passing to ContactsScreen
                                    LaunchedEffect(Unit) {
                                        pendingAddName = null
                                        pendingAddPhone = null
                                    }
                                }
                                AppScreen.FRIENDS -> {
                                    FriendsScreen()
                                }
                                AppScreen.PROFILE -> {
                                    ProfileScreen(
                                        onOpenAuth = { showAuthModal = true }
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
