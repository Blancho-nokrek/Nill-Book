package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.SearchBar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAddContactDirectly: (name: String, phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchMode by remember { mutableStateOf("number") } // "number" or "name"
    var selectedCountry by remember { mutableStateOf("BD") }
    var query by remember { mutableStateOf("") }
    var isCountryMenuOpen by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    fun performSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        isSearching = true
        message = null

        if (searchMode == "number") {
            val result = NillBookRepository.searchByNumber(q, selectedCountry)
            if (result != null) {
                searchResults = listOf(result)
            } else {
                searchResults = emptyList()
                val e164 = PhoneUtils.toE164(q, selectedCountry) ?: q
                message = "No name found for $e164"
            }
        } else {
            if (q.length < 3) {
                message = "Type at least 3 characters to search by name."
                searchResults = emptyList()
            } else {
                val list = NillBookRepository.searchByName(q)
                searchResults = list
                if (list.isEmpty()) {
                    message = "No match found. Check the spelling of the name."
                }
            }
        }
        isSearching = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero title & Tagline
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NILLBOOK",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SMART CALLER DIRECTORY & SEARCH",
                    color = Cyan400,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Mode switch tabs: Number vs Name
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            searchMode = "number"
                            query = ""
                            message = null
                            searchResults = emptyList()
                        }
                        .testTag("tab_number"),
                    color = if (searchMode == "number") Cyan400 else Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search by Number",
                            color = if (searchMode == "number") Slate950 else Slate400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            searchMode = "name"
                            query = ""
                            message = null
                            searchResults = emptyList()
                        }
                        .testTag("tab_name"),
                    color = if (searchMode == "name") Cyan400 else Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search by Name",
                            color = if (searchMode == "name") Slate950 else Slate400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Input controls
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Slate900.copy(alpha = 0.8f))
                    .border(1.dp, Slate800, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (searchMode == "number") {
                    // Country Picker Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCountryMenuOpen,
                        onExpandedChange = { isCountryMenuOpen = !isCountryMenuOpen },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentCountry = PhoneUtils.COUNTRIES.find { it.code == selectedCountry }
                            ?: PhoneUtils.COUNTRIES.first()
                        OutlinedTextField(
                            value = currentCountry.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Country Calling Code", color = Slate400) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryMenuOpen) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan400,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Slate300,
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("country_picker")
                        )

                        ExposedDropdownMenu(
                            expanded = isCountryMenuOpen,
                            onDismissRequest = { isCountryMenuOpen = false },
                            modifier = Modifier.background(Slate900)
                        ) {
                            PhoneUtils.COUNTRIES.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.label, color = Slate200) },
                                    onClick = {
                                        selectedCountry = c.code
                                        isCountryMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Core Caller Directory Search Bar component
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { performSearch() },
                    placeholder = "Search contacts or numbers",
                    isLoading = isSearching,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { performSearch() },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("search_submit_button")
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Slate950)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Directory", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Message banner
        if (message != null) {
            item {
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Cyan400)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(message ?: "", color = Slate300, fontSize = 14.sp)
                    }
                }
            }
        }

        // Search Results Header
        if (searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "SEARCH RESULTS (${searchResults.size})",
                    color = Slate400,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Search Result Items
        items(searchResults, key = { "${it.number}|${it.name}" }) { result ->
            SearchResultCard(
                result = result,
                onCall = {
                    if (result.numberVisible && !result.number.contains("•")) {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${result.number}"))
                        context.startActivity(dialIntent)
                    }
                },
                onAddContact = {
                    onAddContactDirectly(result.name, result.number)
                },
                onFriendAction = {
                    result.userId?.let { uid ->
                        if (result.friendStatus == "none") {
                            NillBookRepository.sendFriendRequest(uid)
                            performSearch()
                        } else if (result.friendStatus == "pending_in") {
                            result.requestId?.let { NillBookRepository.acceptFriendRequest(it) }
                            performSearch()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    onCall: () -> Unit,
    onAddContact: () -> Unit,
    onFriendAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("result_card_${result.name.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                InitialsAvatar(name = result.name, size = 52)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.number,
                        color = Cyan400,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (result.registered) {
                        Surface(
                            color = Emerald950.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp),
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Emerald400)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Registered on NILLBOOK",
                                    color = Emerald400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "saved by ${result.count} ${if (result.count == 1) "person" else "people"}",
                            color = Slate500,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Slate800, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.numberVisible && !result.number.contains("•")) {
                    OutlinedButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald400),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Emerald400.copy(alpha = 0.5f))),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("action_call")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onAddContact,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("action_save_contact")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", fontSize = 12.sp)
                }

                if (result.registered && result.userId != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    when (result.friendStatus) {
                        "friends" -> {
                            FilledTonalButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Cyan950,
                                    contentColor = Cyan300
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Friends", fontSize = 12.sp)
                            }
                        }
                        "pending_out" -> {
                            FilledTonalButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Request Sent", fontSize = 12.sp)
                            }
                        }
                        "pending_in" -> {
                            Button(
                                onClick = onFriendAction,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Accept Request", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "none" -> {
                            Button(
                                onClick = onFriendAction,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("action_add_friend")
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Friend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
