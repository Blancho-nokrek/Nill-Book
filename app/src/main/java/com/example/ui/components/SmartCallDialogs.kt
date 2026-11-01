package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SmartCallOptionDialog(
    contactName: String,
    phone: String,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onInternetAudioCall: () -> Unit,
    onInternetVideoCall: () -> Unit,
    onSimCall: (simSlot: String) -> Unit
) {
    var selectedSim by remember { mutableStateOf("SIM 1") }
    var showSimDisclaimer by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Cyan400.copy(alpha = 0.5f), Slate800))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("smart_call_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = LanguageManager.t("call_dialog_title"),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$contactName ($phone)",
                            color = Cyan400,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(color = Slate800)

                // OPTION A: INTERNET CALLS
                Text(
                    text = "OPTION A: INTERNET CALLS (DATA / WI-FI)",
                    color = Cyan400,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Internet Audio Call Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = isOnline) {
                            onDismiss()
                            onInternetAudioCall()
                        }
                        .testTag("option_internet_audio_call"),
                    colors = CardDefaults.cardColors(containerColor = if (isOnline) Slate950 else Slate950.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isOnline) Emerald400.copy(alpha = 0.4f) else Slate800))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Emerald400.copy(alpha = 0.2f) else Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = if (isOnline) Emerald400 else Slate600, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(LanguageManager.t("call_option_internet_audio"), color = if (isOnline) Color.White else Slate500, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (isOnline) "High-Definition VoIP • Free over internet" else "Offline • Requires internet connection", color = if (isOnline) Slate400 else Red400, fontSize = 11.sp)
                        }
                        if (isOnline) {
                            Surface(color = Emerald950, shape = RoundedCornerShape(6.dp)) {
                                Text("FREE", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                // Internet Video Call Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = isOnline) {
                            onDismiss()
                            onInternetVideoCall()
                        }
                        .testTag("option_internet_video_call"),
                    colors = CardDefaults.cardColors(containerColor = if (isOnline) Slate950 else Slate950.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isOnline) Cyan400.copy(alpha = 0.4f) else Slate800))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Cyan400.copy(alpha = 0.2f) else Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = if (isOnline) Cyan400 else Slate600, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(LanguageManager.t("call_option_internet_video"), color = if (isOnline) Color.White else Slate500, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (isOnline) "Real-time HD video call • Free over data" else "Offline • Requires internet connection", color = if (isOnline) Slate400 else Red400, fontSize = 11.sp)
                        }
                        if (isOnline) {
                            Surface(color = Cyan950, shape = RoundedCornerShape(6.dp)) {
                                Text("HD", color = Cyan400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate800)

                // OPTION B: SIM / CARRIER CALL
                Text(
                    text = "OPTION B: SIM / CARRIER CALL (CELLULAR)",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate950),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Slate800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SimCard, contentDescription = null, tint = Amber400, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(LanguageManager.t("call_option_sim"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Uses phone mobile network", color = Slate500, fontSize = 11.sp)
                                }
                            }

                            // SIM Selector chips
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("SIM 1", "SIM 2").forEach { sim ->
                                    val isSelected = selectedSim == sim
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedSim = sim },
                                        color = if (isSelected) Amber400 else Slate800
                                    ) {
                                        Text(
                                            text = sim,
                                            color = if (isSelected) Slate950 else Slate300,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Mandatory Carrier Charges Disclaimer
                        Surface(
                            color = Amber950.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Amber400.copy(alpha = 0.4f)))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Amber400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = LanguageManager.t("sim_charges_disclaimer"),
                                    color = Amber200,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onSimCall(selectedSim)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Slate950),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_sim_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm & Dial via $selectedSim", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// Active In-App Call Screen Modal with Interactive Controls
@Composable
fun ActiveCallScreenModal(
    callState: ActiveCallState,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    var callSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val formattedDuration = remember(callSeconds) {
        val mins = callSeconds / 60
        val secs = callSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Slate950
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (callState.mode == CallMode.INTERNET_VIDEO) Slate900 else Slate950,
                                Slate950,
                                if (callState.mode == CallMode.INTERNET_VIDEO) Slate900 else Slate950
                            )
                        )
                    )
            ) {
                // Top header
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(20.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Emerald400.copy(alpha = 0.5f)))
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
                                text = if (callState.mode == CallMode.INTERNET_VIDEO) "HD Internet Video • Connected" else "HD Internet Audio • Connected",
                                color = Emerald400,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    ProfileAvatarPlaceholder(name = callState.contactName, size = 110)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = callState.contactName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = callState.phone,
                        color = Cyan400,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formattedDuration,
                        color = Slate300,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                // If Video Call: Picture-in-picture simulated camera
                if (callState.mode == CallMode.INTERNET_VIDEO) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 130.dp, end = 20.dp)
                            .size(width = 100.dp, height = 140.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Cyan400))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (callState.isVideoDisabled) {
                                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Red400, modifier = Modifier.size(32.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = Cyan400, modifier = Modifier.size(38.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Self Camera", color = Slate400, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Bottom Call Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 54.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Slate900.copy(alpha = 0.95f))
                            .border(1.dp, Slate800, RoundedCornerShape(32.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Mute toggle
                        IconButton(
                            onClick = onMuteToggle,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (callState.isMuted) Red400 else Slate800)
                                .testTag("call_mute_toggle")
                        ) {
                            Icon(
                                imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (callState.isMuted) Slate950 else Color.White
                            )
                        }

                        // Speakerphone toggle
                        IconButton(
                            onClick = onSpeakerToggle,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (callState.isSpeakerOn) Cyan400 else Slate800)
                                .testTag("call_speaker_toggle")
                        ) {
                            Icon(
                                imageVector = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Speaker",
                                tint = if (callState.isSpeakerOn) Slate950 else Color.White
                            )
                        }

                        // Video toggle (if video call)
                        if (callState.mode == CallMode.INTERNET_VIDEO) {
                            IconButton(
                                onClick = onVideoToggle,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (callState.isVideoDisabled) Red400 else Slate800)
                                    .testTag("call_video_toggle")
                            ) {
                                Icon(
                                    imageVector = if (callState.isVideoDisabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                    contentDescription = "Camera",
                                    tint = if (callState.isVideoDisabled) Slate950 else Color.White
                                )
                            }

                            // Switch Camera (Front / Back)
                            var isFrontCam by remember { mutableStateOf(true) }
                            IconButton(
                                onClick = { isFrontCam = !isFrontCam },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isFrontCam) Slate800 else Cyan950)
                                    .testTag("call_switch_camera")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = Cyan400
                                )
                            }
                        }

                        // End Call (Red)
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Red400)
                                .testTag("end_active_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Slate950,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
