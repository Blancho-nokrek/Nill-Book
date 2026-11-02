package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.components.SmartCallOptionDialog
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    targetUserId: String? = null,
    onOpenAuth: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenChat: ((phone: String, name: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by NillBookRepository.currentUser.collectAsState()
    val friendships by NillBookRepository.friendships.collectAsState()
    val isOnline by NillBookRepository.isOnline.collectAsState()

    val isOwnProfile = remember(targetUserId, currentUser) {
        targetUserId == null || targetUserId == currentUser?.id
    }

    val displayedUser = remember(targetUserId, currentUser, friendships) {
        if (isOwnProfile) {
            currentUser
        } else {
            NillBookRepository.getUserById(targetUserId!!) ?: User(
                id = targetUserId,
                name = "Directory User",
                email = "user@nillbook.com",
                phone = "+8801700000000",
                bio = "NILLBOOK Community Member"
            )
        }
    }

    val friendshipStatus = remember(displayedUser, currentUser, friendships) {
        if (displayedUser == null || currentUser == null || isOwnProfile) "none"
        else {
            val me = currentUser!!.id
            val other = displayedUser.id
            val f = friendships.find {
                (it.requesterId == me && it.addresseeId == other) ||
                (it.requesterId == other && it.addresseeId == me)
            }
            when {
                f == null -> "none"
                f.status == "ACCEPTED" -> "friends"
                f.requesterId == me -> "pending_out"
                else -> "pending_in"
            }
        }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var callDialogTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            NillBookRepository.updateProfilePhoto(uri.toString())
            toastMessage = "Profile picture updated!"
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            NillBookRepository.updateCoverPhoto(uri.toString())
            toastMessage = "Cover photo updated!"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate900)
                                .size(38.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = if (isOwnProfile) LanguageManager.t("profile") else "Caller Profile",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isOwnProfile && displayedUser != null) {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate900)
                            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                            .size(40.dp)
                            .testTag("profile_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile Settings",
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (displayedUser == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Not signed in", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Sign in to manage your identity, photo, and privacy settings", color = Slate400, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Log In or Register", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val u = displayedUser

            toastMessage?.let { msg ->
                item {
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Slate200, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Profile Card Header with Cover & Avatar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(contentAlignment = Alignment.BottomEnd) {
                            ProfileAvatarPlaceholder(
                                name = u.name,
                                size = 88,
                                photoUrl = u.photo,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(2.5.dp, Cyan400, CircleShape)
                            )

                            if (isOwnProfile) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Cyan400)
                                        .border(2.dp, Slate900, CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("upload_profile_photo_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload Profile Picture",
                                        tint = Slate950,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // User Name with Verified Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = u.name,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Profile",
                                tint = Cyan400,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val phoneToDisplay = remember(u, isOwnProfile, friendshipStatus) {
                            if (isOwnProfile || u.showNumberPublic || friendshipStatus == "friends") {
                                u.phone
                            } else {
                                PhoneUtils.maskNumber(u.phone)
                            }
                        }
                        Text(
                            text = phoneToDisplay,
                            color = Cyan400,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = u.bio,
                            color = Slate300,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Call, Message, Friend Actions
                        if (!isOwnProfile) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        callDialogTarget = Pair(u.name, u.phone)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Slate950),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenChat?.invoke(u.phone, u.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Message", fontWeight = FontWeight.Bold)
                                }

                                when (friendshipStatus) {
                                    "friends" -> {
                                        FilledTonalButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Cyan300)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    "pending_out" -> {
                                        FilledTonalButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Slate400)
                                        ) {
                                            Text("Sent", fontSize = 12.sp)
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = {
                                                NillBookRepository.sendFriendRequest(u.id)
                                                toastMessage = "Friend request sent to ${u.name}!"
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Cyan400)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("change_photo_btn")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showEditProfileDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Slate100),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("edit_profile_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // About & Identity Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "IDENTITY & PERSONAL INFO",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        ProfileInfoRow("Username", "@${u.username}")
                        ProfileInfoRow("About", u.about)
                        ProfileInfoRow("Location", u.location)
                        ProfileInfoRow("Gender", u.gender)
                        ProfileInfoRow("Birthday", PhoneUtils.formatBirthday(u.birthday))
                        ProfileInfoRow("Presence", if (isOnline) "Online Now" else "Offline Mode")
                        ProfileInfoRow("Two-Factor Auth", if (u.twoFactorEnabled) "Active (Secure)" else "Disabled")
                    }
                }
            }

            // Connection & Dual Mode Controls (Online / Offline Switch)
            if (isOwnProfile) {
                item {
                    val networkState by NetworkMonitor.networkState.collectAsState()
                    val isAppOnline = networkState.isOnline

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_connection_mode_card"),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isAppOnline) Cyan400.copy(alpha = 0.4f) else Amber400.copy(alpha = 0.4f))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "CONNECTION & OPERATION MODE",
                                color = if (isAppOnline) Cyan400 else Amber400,
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
                                            .background(if (isAppOnline) Cyan400.copy(alpha = 0.15f) else Amber400.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAppOnline) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                                            contentDescription = null,
                                            tint = if (isAppOnline) Cyan400 else Amber400,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isAppOnline) "Online Mode Active" else "Offline SIM Mode Active",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isAppOnline) "Internet messaging, VoIP audio/video calls enabled." else "Cellular voice calls & carrier SMS enabled.",
                                            color = Slate400,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = isAppOnline,
                                    onCheckedChange = { isChecked ->
                                        NetworkMonitor.setSimulationOverride(isChecked)
                                        toastMessage = if (isChecked) "Switched to Online Mode" else "Switched to Offline SIM Mode"
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Slate950,
                                        checkedTrackColor = Cyan400,
                                        uncheckedThumbColor = Slate400,
                                        uncheckedTrackColor = Slate800
                                    ),
                                    modifier = Modifier.testTag("profile_screen_online_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Privacy & Account Controls
            if (isOwnProfile) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CALLER PRIVACY CONTROLS",
                                color = Slate400,
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Public Caller Number",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Allow search results to display your unmasked phone number publicly.",
                                        color = Slate400,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = u.showNumberPublic,
                                    onCheckedChange = { isChecked ->
                                        NillBookRepository.updateProfile(
                                            name = u.name,
                                            phone = u.phone,
                                            gender = u.gender,
                                            birthday = u.birthday,
                                            bio = u.bio,
                                            location = u.location,
                                            showPublic = isChecked
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Slate950,
                                        checkedTrackColor = Cyan400,
                                        uncheckedThumbColor = Slate400,
                                        uncheckedTrackColor = Slate800
                                    ),
                                    modifier = Modifier.testTag("privacy_public_number_switch")
                                )
                            }
                        }
                    }
                }

                // Account Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAuth,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch User")
                        }

                        OutlinedButton(
                            onClick = { NillBookRepository.logout() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Log Out")
                        }
                    }
                }
            }
        }
    }

    // Smart Call Dialog
    callDialogTarget?.let { (name, phone) ->
        SmartCallOptionDialog(
            contactName = name,
            phone = phone,
            isOnline = isOnline,
            onDismiss = { callDialogTarget = null },
            onInternetAudioCall = {
                NillBookRepository.startInternetCall(name, phone, CallMode.INTERNET_AUDIO)
            },
            onInternetVideoCall = {
                NillBookRepository.startInternetCall(name, phone, CallMode.INTERNET_VIDEO)
            },
            onSimCall = { simSlot ->
                NillBookRepository.recordCall(name, phone, CallType.OUTGOING, CallMode.SIM_CARRIER, simSlot)
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        )
    }

    // Edit Profile Details Dialog
    if (showEditProfileDialog && displayedUser != null) {
        val u = displayedUser
        EditProfileDialog(
            user = u,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, bio, location, gender, birthday ->
                NillBookRepository.updateProfile(
                    name = name,
                    phone = phone,
                    gender = gender,
                    birthday = birthday,
                    bio = bio,
                    location = location,
                    showPublic = u.showNumberPublic
                )
                toastMessage = "Profile details saved!"
                showEditProfileDialog = false
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog && displayedUser != null) {
        val u = displayedUser
        ProfileSettingsDialog(
            user = u,
            onDismiss = { showSettingsDialog = false },
            onUpdateVisibility = { isChecked ->
                NillBookRepository.updateProfile(
                    name = u.name,
                    phone = u.phone,
                    gender = u.gender,
                    birthday = u.birthday,
                    bio = u.bio,
                    location = u.location,
                    showPublic = isChecked
                )
            },
            onOpenDeleteAccount = {
                showDeleteAccountConfirm = true
            },
            onToast = { toastMessage = it }
        )
    }

    // Account Deletion Confirmation Dialog
    if (showDeleteAccountConfirm) {
        var keepLocalData by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Red400)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This will permanently delete your online Nill Book caller ID profile, directory contributions, and friend connections from the cloud.",
                        color = Slate300,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = keepLocalData,
                                onCheckedChange = { keepLocalData = it },
                                colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Retain local device phonebook", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Keep saved device contacts and SMS drafts on this phone", color = Slate400, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.deleteAccount()
                        if (!keepLocalData) {
                            LocalStorageManager.clearLocalMediaCache(context)
                        }
                        showDeleteAccountConfirm = false
                        toastMessage = "Account deleted successfully."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Slate950)
                ) {
                    Text("Confirm Permanent Deletion", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Slate400, fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2)
    }
}

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, bio: String, location: String, gender: String, birthday: String) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var bio by remember { mutableStateOf(user.bio) }
    var location by remember { mutableStateOf(user.location) }
    var gender by remember { mutableStateOf(user.gender) }
    var birthday by remember { mutableStateOf(user.birthday) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Customize Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Status", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, phone, bio, location, gender, birthday) },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsDialog(
    user: User,
    onDismiss: () -> Unit,
    onUpdateVisibility: (Boolean) -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    var showNumberPublic by remember { mutableStateOf(user.showNumberPublic) }
    var spamProtection by remember { mutableStateOf(true) }
    var readReceipts by remember { mutableStateOf(true) }
    var showMediaManager by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val lastBackup by LocalStorageManager.lastBackup.collectAsState()
    val totalStorageBytes = remember(showMediaManager) { LocalStorageManager.getTotalStorageSizeBytes(context) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Cyan400, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LanguageManager.t("settings"),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Connection & Operation Mode Switch (Online / Offline)
                    item {
                        Text("CONNECTION & OPERATION MODE", color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        val netState by NetworkMonitor.networkState.collectAsState()
                        val isOnlineMode = netState.isOnline
                        SettingToggleRow(
                            icon = if (isOnlineMode) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                            title = if (isOnlineMode) "Online Mode" else "Offline SIM Mode",
                            subtitle = if (isOnlineMode) "Connected to internet. VoIP calls & instant messaging active." else "Cellular SIM calls & carrier SMS active.",
                            checked = isOnlineMode,
                            onCheckedChange = { checked ->
                                NetworkMonitor.setSimulationOverride(checked)
                                onToast(if (checked) "Switched to Online Mode" else "Switched to Offline SIM Mode")
                            }
                        )
                    }

                    // Privacy & Caller ID
                    item {
                        Text("PRIVACY & CALLER ID", color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        SettingToggleRow(
                            icon = Icons.Default.Public,
                            title = "Public Directory Listing",
                            subtitle = "Allow others to discover your name in caller ID search",
                            checked = showNumberPublic,
                            onCheckedChange = {
                                showNumberPublic = it
                                onUpdateVisibility(it)
                                onToast(if (it) "Number is now public" else "Number hidden")
                            }
                        )
                    }

                    // Local Storage & Media Management
                    item {
                        Text("STORAGE & MEDIA ON DEVICE", color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Cyan400, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Stored Media & Docs", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Total: ${LocalStorageManager.formatBytes(totalStorageBytes)} on device", color = Slate400, fontSize = 11.sp)
                                        }
                                    }
                                    TextButton(onClick = { showMediaManager = true }) {
                                        Text("Manage", color = Cyan400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Clear media cache files", color = Slate400, fontSize = 11.sp)
                                    TextButton(onClick = {
                                        val count = LocalStorageManager.clearLocalMediaCache(context)
                                        onToast("Cleared $count cached media files.")
                                    }) {
                                        Text("Clear Cache", color = Red400, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Local Backup & Restore
                    item {
                        Text("LOCAL BACKUP & RESTORE", color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Backup contacts, call history, and message drafts locally to device storage.",
                                    color = Slate300,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (lastBackup != null) {
                                    Text(
                                        "Last Backup: ${lastBackup!!.timestamp} (${lastBackup!!.fileSizeFormatted})",
                                        color = Emerald400,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val file = LocalStorageManager.createLocalBackup(context)
                                            if (file != null) {
                                                onToast("Local backup saved successfully!")
                                            } else {
                                                onToast("Failed to create local backup.")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Backup Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showRestoreConfirm = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Account Management & Deletion
                    item {
                        Text("ACCOUNT MANAGEMENT", color = Cyan400, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Delete Account", color = Red400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Permanently delete cloud profile & directory listings", color = Slate500, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenDeleteAccount()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Red400.copy(alpha = 0.2f), contentColor = Red400),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Version info
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("NILL BOOK v2.1.0", color = Slate500, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("Verified Communication & Contact Management", color = Slate600, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Media Manager Dialog
    if (showMediaManager) {
        LocalMediaManagerDialog(
            onDismiss = { showMediaManager = false },
            onToast = onToast
        )
    }

    // Restore Backup Confirmation Dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Local Backup?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will restore your contacts, call logs, and conversations from the latest local device backup file. Existing contacts will be merged safely without creating duplicates.",
                    color = Slate300,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (c, calls, convs) = LocalStorageManager.restoreLocalBackup(context)
                        showRestoreConfirm = false
                        onToast("Restored $c contacts, $calls call logs, and $convs conversations.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                ) {
                    Text("Confirm Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun LocalMediaManagerDialog(
    onDismiss: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val storedMedia by LocalStorageManager.storedMedia.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Device Stored Files", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${storedMedia.size} items stored on phone", color = Cyan400, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (storedMedia.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Slate600, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No stored media files on device", color = Slate400, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(storedMedia.size) { index ->
                            val item = storedMedia[index]
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate950),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (item.fileType) {
                                            "image" -> Icons.Default.Image
                                            "video" -> Icons.Default.Videocam
                                            "pdf" -> Icons.Default.PictureAsPdf
                                            "audio" -> Icons.Default.Audiotrack
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = Cyan400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.fileName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text("${LocalStorageManager.formatBytes(item.sizeBytes)} • ${item.timestamp}", color = Slate400, fontSize = 10.sp)
                                    }
                                    IconButton(
                                        onClick = { LocalStorageManager.openLocalFile(context, item.localPath, item.mimeType) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = Slate300, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { LocalStorageManager.shareLocalFile(context, item.localPath, item.mimeType) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Slate300, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            LocalStorageManager.deleteMediaFile(context, item.fileName)
                                            onToast("Deleted ${item.fileName}")
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red400, modifier = Modifier.size(16.dp))
                                    }
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
fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate950),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Cyan400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Slate500, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Slate950,
                    checkedTrackColor = Cyan400,
                    uncheckedThumbColor = Slate400,
                    uncheckedTrackColor = Slate800
                )
            )
        }
    }
}
