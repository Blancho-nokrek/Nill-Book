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
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.example.data.CallType
import com.example.data.FriendItem
import com.example.data.FriendSuggestion
import com.example.data.NillBookRepository
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.theme.*

enum class FriendsTab {
    REQUESTS,
    SUGGESTIONS,
    MY_FRIENDS
}

@Composable
fun FriendsScreen(
    onOpenChat: ((phone: String, name: String) -> Unit)? = null,
    onViewProfile: ((userId: String) -> Unit)? = null,
    onTriggerCall: ((name: String, phone: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendships by NillBookRepository.friendships.collectAsState()
    val allFriends = remember(friendships) { NillBookRepository.getFriendsList() }
    val suggestions = remember(friendships) { NillBookRepository.getFriendSuggestions() }

    val connected = remember(allFriends) { allFriends.filter { it.status == "friends" } }
    val incoming = remember(allFriends) { allFriends.filter { it.status == "pending_in" } }
    val outgoing = remember(allFriends) { allFriends.filter { it.status == "pending_out" } }

    var activeTab by remember { mutableStateOf(FriendsTab.REQUESTS) }
    var suggestionSearchQuery by remember { mutableStateOf("") }
    var friendSearchQuery by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val filteredSuggestions = remember(suggestions, suggestionSearchQuery) {
        val q = suggestionSearchQuery.trim().lowercase()
        if (q.isEmpty()) suggestions
        else suggestions.filter {
            it.name.lowercase().contains(q) || it.phone.contains(q)
        }
    }

    val filteredConnected = remember(connected, friendSearchQuery) {
        val q = friendSearchQuery.trim().lowercase()
        if (q.isEmpty()) connected
        else connected.filter {
            it.name.lowercase().contains(q) || (it.phone != null && it.phone.contains(q))
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header
            item {
                Column {
                    Text(
                        text = "FRIENDS",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Friend requests, directory suggestions & verified connections",
                        color = Slate400,
                        fontSize = 13.sp
                    )
                }
            }

            // 3-Way Segmented Tab Switcher: Requests | Suggestions | My Friends (to the right of suggestions)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 1. Requests Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTab = FriendsTab.REQUESTS }
                            .testTag("tab_requests"),
                        color = if (activeTab == FriendsTab.REQUESTS) Cyan400 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (incoming.isNotEmpty()) "Requests (${incoming.size})" else "Requests",
                                    color = if (activeTab == FriendsTab.REQUESTS) Slate950 else Slate400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // 2. Suggestions Tab (in the middle)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTab = FriendsTab.SUGGESTIONS }
                            .testTag("tab_suggestions"),
                        color = if (activeTab == FriendsTab.SUGGESTIONS) Cyan400 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Suggestions",
                                    color = if (activeTab == FriendsTab.SUGGESTIONS) Slate950 else Slate400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // 3. My Friends Tab (separately on the right of Suggestions!)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTab = FriendsTab.MY_FRIENDS }
                            .testTag("tab_my_friends"),
                        color = if (activeTab == FriendsTab.MY_FRIENDS) Cyan400 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "My Friends (${connected.size})",
                                    color = if (activeTab == FriendsTab.MY_FRIENDS) Slate950 else Slate400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Feedback Banner
            if (feedbackMessage != null) {
                item {
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feedbackMessage ?: "", color = Slate200, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ================== TAB 1: REQUESTS ==================
            if (activeTab == FriendsTab.REQUESTS) {
                // Incoming requests section
                if (incoming.isNotEmpty()) {
                    item {
                        Text(
                            text = "INCOMING REQUESTS (${incoming.size})",
                            color = Cyan400,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    items(incoming, key = { it.requestId ?: it.userId }) { item ->
                        RequestCard(
                            item = item,
                            isIncoming = true,
                            onViewProfile = { onViewProfile?.invoke(item.userId) },
                            onAccept = {
                                item.requestId?.let { NillBookRepository.acceptFriendRequest(it) }
                                feedbackMessage = "Friend request accepted! ${item.name} is now in My Friends."
                            },
                            onDecline = {
                                item.requestId?.let { NillBookRepository.declineFriendRequest(it) }
                                feedbackMessage = "Friend request declined."
                            }
                        )
                    }
                }

                // Outgoing Sent Requests Section
                if (outgoing.isNotEmpty()) {
                    item {
                        Text(
                            text = "SENT REQUESTS (${outgoing.size})",
                            color = Slate500,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    items(outgoing, key = { it.requestId ?: it.userId }) { item ->
                        RequestCard(
                            item = item,
                            isIncoming = false,
                            onViewProfile = { onViewProfile?.invoke(item.userId) },
                            onAccept = {},
                            onDecline = {
                                item.requestId?.let { NillBookRepository.declineFriendRequest(it) }
                                feedbackMessage = "Friend request cancelled."
                            }
                        )
                    }
                }

                // If no incoming and no sent requests
                if (incoming.isEmpty() && outgoing.isEmpty()) {
                    item {
                        Surface(
                            color = Slate900.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.MailOutline, contentDescription = null, tint = Slate600, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No pending friend requests",
                                    color = Slate300,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Check the Suggestions tab to find people you may know.",
                                    color = Slate500,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ================== TAB 2: SUGGESTIONS ==================
            if (activeTab == FriendsTab.SUGGESTIONS) {
                item {
                    OutlinedTextField(
                        value = suggestionSearchQuery,
                        onValueChange = { suggestionSearchQuery = it },
                        placeholder = { Text("Search suggestions by name...", color = Slate500, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Cyan400, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (suggestionSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { suggestionSearchQuery = "" }) {
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
                        modifier = Modifier.fillMaxWidth().testTag("suggestions_search_box")
                    )
                }

                item {
                    Text(
                        text = "PEOPLE YOU MAY KNOW (${filteredSuggestions.size})",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (filteredSuggestions.isEmpty()) {
                    item {
                        Surface(
                            color = Slate900.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Slate600, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (suggestionSearchQuery.isEmpty()) "No suggestions available" else "No users matching \"$suggestionSearchQuery\"",
                                    color = Slate300,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All verified callers are already in your requests or friends list.",
                                    color = Slate500,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSuggestions, key = { it.userId }) { suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            onViewProfile = { onViewProfile?.invoke(suggestion.userId) },
                            onAddFriend = {
                                NillBookRepository.sendFriendRequest(suggestion.userId)
                                feedbackMessage = "Friend request sent to ${suggestion.name}!"
                            }
                        )
                    }
                }
            }

            // ================== TAB 3: MY FRIENDS (Separate Tab to the Right) ==================
            if (activeTab == FriendsTab.MY_FRIENDS) {
                // Search box for connected friends
                item {
                    OutlinedTextField(
                        value = friendSearchQuery,
                        onValueChange = { friendSearchQuery = it },
                        placeholder = { Text("Search friends by name or number...", color = Slate500, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (friendSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { friendSearchQuery = "" }) {
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
                        modifier = Modifier.fillMaxWidth().testTag("friends_search_box")
                    )
                }

                item {
                    Text(
                        text = "CONNECTED FRIENDS (${filteredConnected.size})",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (filteredConnected.isEmpty()) {
                    item {
                        Surface(
                            color = Slate900.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = Slate600, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (friendSearchQuery.isEmpty()) "No connected friends yet" else "No matching friends",
                                    color = Slate300,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Open the Suggestions tab to find and add callers to your network.",
                                    color = Slate500,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredConnected, key = { it.userId }) { friend ->
                        ConnectedFriendCard(
                            item = friend,
                            onViewProfile = { onViewProfile?.invoke(friend.userId) },
                            onCall = {
                                friend.phone?.let {
                                    if (!it.contains("•")) {
                                        if (onTriggerCall != null) {
                                            onTriggerCall(friend.name, it)
                                        } else {
                                            NillBookRepository.recordCall(friend.name, it, CallType.OUTGOING)
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                                            context.startActivity(dialIntent)
                                        }
                                    }
                                }
                            },
                            onMessage = {
                                friend.phone?.let {
                                    onOpenChat?.invoke(it, friend.name)
                                }
                            },
                            onRemove = {
                                NillBookRepository.removeFriend(friend.userId)
                                feedbackMessage = "Removed ${friend.name} from friends."
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: FriendSuggestion,
    onAddFriend: () -> Unit,
    onViewProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("suggestion_${suggestion.userId}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onViewProfile != null) { onViewProfile?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatarPlaceholder(name = suggestion.name, size = 46)
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = suggestion.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = suggestion.phone,
                        color = Cyan400,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Emerald400)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${suggestion.mutualFriendsCount} mutual connections",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddFriend,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_add_friend_${suggestion.userId}")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RequestCard(
    item: FriendItem,
    isIncoming: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onViewProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onViewProfile != null) { onViewProfile?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialsAvatar(name = item.name, size = 44)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        text = if (isIncoming) "Wants to connect with you" else "Request sent • Waiting for response",
                        color = if (isIncoming) Cyan400 else Slate500,
                        fontSize = 12.sp
                    )
                }
            }

            if (isIncoming) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledIconButton(
                        onClick = onAccept,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Cyan400, contentColor = Slate950),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(18.dp))
                    }
                    FilledTonalIconButton(
                        onClick = onDecline,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Slate800, contentColor = Red400),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                TextButton(onClick = onDecline) {
                    Text("Cancel", color = Slate400, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ConnectedFriendCard(
    item: FriendItem,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onRemove: () -> Unit,
    onViewProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onViewProfile != null) { onViewProfile?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialsAvatar(name = item.name, size = 46)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(15.dp))
                    }
                    item.phone?.let {
                        Text(
                            text = it,
                            color = Cyan400,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCall, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald400, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onMessage, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Cyan400, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove Friend", tint = Slate500, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
