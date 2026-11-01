package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.ProfileAvatarPlaceholder
import com.example.ui.theme.*

@Composable
fun CallsScreen(
    onTriggerCall: (name: String, phone: String) -> Unit,
    onOpenChat: (phone: String, name: String) -> Unit,
    onViewProfile: (phoneOrId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val callLogs by NillBookRepository.callLogs.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(callLogs, selectedFilter) {
        when (selectedFilter) {
            "MISSED" -> callLogs.filter { it.type == CallType.MISSED }
            "INTERNET" -> callLogs.filter { it.mode == CallMode.INTERNET_AUDIO || it.mode == CallMode.INTERNET_VIDEO }
            "SIM" -> callLogs.filter { it.mode == CallMode.SIM_CARRIER }
            else -> callLogs
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            // PINNED SECTION: Header & Filter Chips stay pinned while scrolling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate950)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Header (Clean title without extra subtitle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.t("calls"),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (callLogs.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Red400),
                            modifier = Modifier.testTag("clear_calls_btn")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(LanguageManager.t("clear_history"), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pinned Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All (${callLogs.size})",
                        "MISSED" to "Missed",
                        "INTERNET" to "Internet Calls",
                        "SIM" to "SIM / Carrier"
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400,
                                selectedLabelColor = Slate950,
                                containerColor = Slate900,
                                labelColor = Slate300
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Cyan400 else Slate800
                            )
                        )
                    }
                }
            }

            // Scrollable Call Logs list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

            // Empty state
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
                        Text("No call activity recorded", color = Slate300, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Initiate internet or cellular calls using the smart call button", color = Slate500, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    AdvancedCallLogCard(
                        log = log,
                        onCall = { onTriggerCall(log.name, log.phone) },
                        onMessage = { onOpenChat(log.phone, log.name) },
                        onViewProfile = { onViewProfile(log.phone) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Call Logs?", color = Color.White) },
            text = { Text("This will permanently clear all recorded internet and cellular call history.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        NillBookRepository.clearCallLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Slate950)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
    }
}

@Composable
fun AdvancedCallLogCard(
    log: CallLogEntry,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("call_entry_${log.id}"),
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
            ProfileAvatarPlaceholder(
                name = log.name,
                size = 46,
                modifier = Modifier.clickable { onViewProfile() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onViewProfile() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.name,
                        color = if (log.type == CallType.MISSED) Red400 else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Channel Mode Badge (VoIP vs Video vs SIM)
                    val (modeText, modeBg, modeFg) = when (log.mode) {
                        CallMode.INTERNET_AUDIO -> Triple("Internet", Emerald950, Emerald400)
                        CallMode.INTERNET_VIDEO -> Triple("Video", Cyan950, Cyan400)
                        CallMode.SIM_CARRIER -> Triple(log.simSlot, Amber950, Amber400)
                    }

                    Surface(
                        color = modeBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = modeText,
                            color = modeFg,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (log.type) {
                        CallType.MISSED -> Icons.Default.CallMissed to Red400
                        CallType.INCOMING -> Icons.Default.CallReceived to Emerald400
                        CallType.OUTGOING -> Icons.Default.CallMade to Cyan400
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = log.phone, color = Slate400, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.timestamp,
                    color = Slate500,
                    fontSize = 11.sp
                )
            }

            // Quick Actions: Smart Call Back and Open Message
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Emerald400.copy(alpha = 0.15f))
                        .testTag("call_back_${log.id}")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call Back", tint = Emerald400, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onMessage,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Cyan400.copy(alpha = 0.15f))
                        .testTag("call_msg_${log.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Cyan400, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
