package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FriendItem
import com.example.data.NillBookRepository
import com.example.ui.components.InitialsAvatar
import com.example.ui.theme.*

@Composable
fun FriendsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendships by NillBookRepository.friendships.collectAsState()
    val allFriends = remember(friendships) { NillBookRepository.getFriendsList() }

    val connected = allFriends.filter { it.status == "friends" }
    val incoming = allFriends.filter { it.status == "pending_in" }
    val outgoing = allFriends.filter { it.status == "pending_out" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "NETWORK & FRIENDS",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Friends can view each other's full verified caller number",
                    color = Slate400,
                    fontSize = 13.sp
                )
            }
        }

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
                    onAccept = { item.requestId?.let { NillBookRepository.acceptFriendRequest(it) } },
                    onDecline = { item.requestId?.let { NillBookRepository.declineFriendRequest(it) } }
                )
            }
        }

        // Connected Friends
        item {
            Text(
                text = "CONNECTED FRIENDS (${connected.size})",
                color = Slate400,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (connected.isEmpty()) {
            item {
                Surface(
                    color = Slate900.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Slate600, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No connected friends yet", color = Slate400, fontSize = 14.sp)
                        Text("Search names in directory and send friend requests to connect.", color = Slate500, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(connected, key = { it.userId }) { friend ->
                ConnectedFriendCard(
                    item = friend,
                    onCall = {
                        friend.phone?.let {
                            if (!it.contains("•")) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                                context.startActivity(dialIntent)
                            }
                        }
                    },
                    onRemove = { NillBookRepository.removeFriend(friend.userId) }
                )
            }
        }

        // Outgoing requests
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
                    onAccept = {},
                    onDecline = { item.requestId?.let { NillBookRepository.declineFriendRequest(it) } }
                )
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
            InitialsAvatar(name = item.name, size = 44)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = if (isIncoming) "Wants to connect with you" else "Request pending",
                    color = if (isIncoming) Cyan400 else Slate500,
                    fontSize = 12.sp
                )
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
    onRemove: () -> Unit,
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
            InitialsAvatar(name = item.name, size = 46)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                item.phone?.let {
                    Text(
                        text = it,
                        color = Cyan400,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald400, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove Friend", tint = Slate500, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
