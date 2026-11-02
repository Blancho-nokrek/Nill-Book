package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.data.LanguageManager
import com.example.ui.theme.*

enum class AppScreen(val key: String, val icon: ImageVector) {
    SEARCH("search", Icons.Default.Search),
    MESSAGES("chats", Icons.AutoMirrored.Filled.Chat),
    CONTACTS("contacts", Icons.Default.Contacts),
    FRIENDS("friends", Icons.Default.Group),
    CALLS("calls", Icons.Default.PhoneCallback),
    PROFILE("profile", Icons.Default.Person)
}

@Composable
fun NillBookTopBar(
    currentScreen: AppScreen,
    userName: String,
    friendRequestsCount: Int,
    isOnline: Boolean,
    onToggleOnlineMode: () -> Unit,
    onProfileClick: () -> Unit,
    userPhoto: String? = null,
    onLanguageClick: (() -> Unit)? = null,
    networkState: NetworkState? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Slate950.copy(alpha = 0.95f),
        border = null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("app_brand_logo")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "NILLBOOK Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NILLBOOK",
                            color = Cyan400,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (currentScreen == AppScreen.MESSAGES) {
                            Text(
                                text = " • CHATS",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Action Controls on Right: Profile with User Photo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profile Avatar icon button (Displays real profile photo in top corner)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onProfileClick)
                            .testTag("topbar_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatarPlaceholder(
                            name = userName,
                            size = 38,
                            photoUrl = userPhoto
                        )
                        if (friendRequestsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Cyan400)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Slate700.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

@Composable
fun NillBookBottomBar(
    currentScreen: AppScreen,
    friendRequestsCount: Int,
    unreadMessagesCount: Int = 0,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomScreens = remember {
        listOf(
            AppScreen.SEARCH,
            AppScreen.MESSAGES,
            AppScreen.CONTACTS,
            AppScreen.FRIENDS,
            AppScreen.CALLS
        )
    }

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        containerColor = Slate950.copy(alpha = 0.98f),
        contentColor = Slate400,
        tonalElevation = 0.dp
    ) {
        bottomScreens.forEach { screen ->
            val isSelected = currentScreen == screen
            val label = LanguageManager.t(screen.key)

            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == AppScreen.FRIENDS && friendRequestsCount > 0) {
                                Badge(
                                    containerColor = Cyan400,
                                    contentColor = Slate950
                                ) {
                                    Text(friendRequestsCount.toString())
                                }
                            } else if (screen == AppScreen.MESSAGES && unreadMessagesCount > 0) {
                                Badge(
                                    containerColor = Emerald400,
                                    contentColor = Slate950
                                ) {
                                    Text(unreadMessagesCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Cyan400,
                    selectedTextColor = Cyan400,
                    unselectedIconColor = Slate500,
                    unselectedTextColor = Slate500,
                    indicatorColor = Cyan950.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
            )
        }
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "N" }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Cyan400.copy(alpha = 0.12f))
            .border(1.dp, Cyan400.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Cyan400,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.6).sp
        )
    }
}
