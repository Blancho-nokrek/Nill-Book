package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onDismiss: () -> Unit,
    initialMode: String = "login",
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(initialMode) } // "login" or "register"

    // Log In state
    var loginMethod by remember { mutableStateOf("phone") } // "phone" or "email"
    var loginPhone by remember { mutableStateOf("") }
    var loginEmail by remember { mutableStateOf("") }
    var loginCountry by remember { mutableStateOf("BD") }
    var isLoginCountryMenuOpen by remember { mutableStateOf(false) }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Registration state (matching user screenshot layout)
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPhone by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("BD") }
    var isCountryMenuOpen by remember { mutableStateOf(false) }

    var registerGender by remember { mutableStateOf("") }
    var isGenderMenuOpen by remember { mutableStateOf(false) }

    var selectedDay by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }
    var isDayMenuOpen by remember { mutableStateOf(false) }
    var isMonthMenuOpen by remember { mutableStateOf(false) }
    var isYearMenuOpen by remember { mutableStateOf(false) }

    var registerPassword by remember { mutableStateOf("") }
    var showRegisterPassword by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Password strength rules for registration
    val hasMinLength = registerPassword.length >= 8
    val hasLetter = registerPassword.any { it.isLetter() }
    val hasNumber = registerPassword.any { it.isDigit() }
    val isPasswordValid = hasMinLength && hasLetter && hasNumber

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Cyan400,
        unfocusedBorderColor = Slate800,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Slate900,
        unfocusedContainerColor = Slate900,
        cursorColor = Cyan400
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 16.dp,
            bottom = 32.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Branding Header (Clean centered header without any close/cross icon)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "NILLBOOK Logo",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "NILLBOOK",
                    color = Cyan400,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CONNECT • IDENTIFY • COMMUNICATE",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Mode Switcher Tabs (Seamless identical theme)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (mode == "login") Cyan400 else Color.Transparent,
                    onClick = {
                        mode = "login"
                        errorMessage = null
                    }
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Log In",
                            color = if (mode == "login") Slate950 else Slate400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (mode == "register") Cyan400 else Color.Transparent,
                    onClick = {
                        mode = "register"
                        errorMessage = null
                    }
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Create Account",
                            color = if (mode == "register") Slate950 else Slate400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Section Title & Subtitle
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (mode == "register") "Create account" else "Log In",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (mode == "register") "Fill in your details to get started." else "Welcome back! Enter your details to continue.",
                    color = Slate400,
                    fontSize = 14.sp
                )
            }
        }

        if (mode == "login") {
            // ================= LOG IN MODE =================
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Method toggle chip (Phone vs Email)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (loginMethod == "phone") Slate800 else Slate900,
                            border = BorderStroke(1.dp, if (loginMethod == "phone") Cyan400 else Slate800),
                            onClick = { loginMethod = "phone"; errorMessage = null }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Phone Number",
                                    color = if (loginMethod == "phone") Cyan400 else Slate400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (loginMethod == "email") Slate800 else Slate900,
                            border = BorderStroke(1.dp, if (loginMethod == "email") Cyan400 else Slate800),
                            onClick = { loginMethod = "email"; errorMessage = null }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Email Address",
                                    color = if (loginMethod == "email") Cyan400 else Slate400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (loginMethod == "phone") {
                        // Phone input with Country Selector (identical component style)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Phone number", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(114.dp)) {
                                    val currentCountry = PhoneUtils.COUNTRIES.find { it.code == loginCountry }
                                        ?: PhoneUtils.COUNTRIES.first()

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isLoginCountryMenuOpen = true }
                                            .testTag("login_country_selector"),
                                        color = Slate900,
                                        border = BorderStroke(1.dp, Slate800),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${currentCountry.flag} ${currentCountry.dialCode}",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Select country",
                                                tint = Slate400,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isLoginCountryMenuOpen,
                                        onDismissRequest = { isLoginCountryMenuOpen = false },
                                        modifier = Modifier
                                            .background(Slate900)
                                            .heightIn(max = 280.dp)
                                    ) {
                                        PhoneUtils.COUNTRIES.forEach { c ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${c.flag}  ${c.label}",
                                                        color = if (c.code == loginCountry) Cyan400 else Slate200,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (c.code == loginCountry) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    loginCountry = c.code
                                                    isLoginCountryMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = loginPhone,
                                    onValueChange = { loginPhone = it },
                                    placeholder = { Text("Enter phone number", color = Slate500, fontSize = 13.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("login_phone_input")
                                )
                            }
                        }
                    } else {
                        // Email input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Email", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                placeholder = { Text("Enter your email", color = Slate500, fontSize = 14.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input")
                            )
                        }
                    }

                    // Password input (with identical Show/Hide toggle)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Password", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            placeholder = { Text("Enter password", color = Slate500, fontSize = 14.sp) },
                            visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                    Text(
                                        text = if (showLoginPassword) "Hide" else "Show",
                                        color = Cyan400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )
                    }
                }
            }
        } else {
            // ================= CREATE ACCOUNT MODE =================
            // Field 1: Full name
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Full name", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = registerName,
                        onValueChange = { registerName = it },
                        placeholder = { Text("Enter your full name", color = Slate500, fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_name_input")
                    )
                }
            }

            // Field 2: Email
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Email", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = registerEmail,
                        onValueChange = { registerEmail = it },
                        placeholder = { Text("Enter your email", color = Slate500, fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_email_input")
                    )
                }
            }

            // Field 3: Phone number (Country Code selector + Phone number input)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Phone number", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code Dropdown
                        Box(modifier = Modifier.width(114.dp)) {
                            val currentCountry = PhoneUtils.COUNTRIES.find { it.code == selectedCountry }
                                ?: PhoneUtils.COUNTRIES.first()

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isCountryMenuOpen = true }
                                    .testTag("country_code_selector"),
                                color = Slate900,
                                border = BorderStroke(1.dp, Slate800),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${currentCountry.flag} ${currentCountry.dialCode}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select country",
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isCountryMenuOpen,
                                onDismissRequest = { isCountryMenuOpen = false },
                                modifier = Modifier
                                    .background(Slate900)
                                    .heightIn(max = 280.dp)
                            ) {
                                PhoneUtils.COUNTRIES.forEach { c ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${c.flag}  ${c.label}",
                                                color = if (c.code == selectedCountry) Cyan400 else Slate200,
                                                fontSize = 13.sp,
                                                fontWeight = if (c.code == selectedCountry) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedCountry = c.code
                                            isCountryMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Phone Number Input
                        OutlinedTextField(
                            value = registerPhone,
                            onValueChange = { registerPhone = it },
                            placeholder = { Text("Enter phone number", color = Slate500, fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_phone_input")
                        )
                    }
                }
            }

            // Field 4: Gender dropdown
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Gender", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isGenderMenuOpen = true }
                                .testTag("gender_selector"),
                            color = Slate900,
                            border = BorderStroke(1.dp, Slate800),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (registerGender.isNotBlank()) registerGender else "Select gender",
                                    color = if (registerGender.isNotBlank()) Color.White else Slate500,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select gender",
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isGenderMenuOpen,
                            onDismissRequest = { isGenderMenuOpen = false },
                            modifier = Modifier
                                .background(Slate900)
                                .fillMaxWidth(0.9f)
                        ) {
                            listOf("Male", "Female", "Other", "Prefer not to say").forEach { g ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = g,
                                            color = if (registerGender == g) Cyan400 else Slate200,
                                            fontSize = 14.sp,
                                            fontWeight = if (registerGender == g) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        registerGender = g
                                        isGenderMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Field 5: Date of birth (Day, Month, Year dropdowns)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Date of birth", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isDayMenuOpen = true }
                                    .testTag("dob_day_selector"),
                                color = Slate900,
                                border = BorderStroke(1.dp, Slate800),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (selectedDay.isNotEmpty()) selectedDay else "Day",
                                        color = if (selectedDay.isNotEmpty()) Color.White else Slate500,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isDayMenuOpen,
                                onDismissRequest = { isDayMenuOpen = false },
                                modifier = Modifier
                                    .background(Slate900)
                                    .heightIn(max = 240.dp)
                            ) {
                                (1..31).forEach { d ->
                                    val dayStr = d.toString().padStart(2, '0')
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = dayStr,
                                                color = if (selectedDay == dayStr) Cyan400 else Slate200,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedDay = dayStr
                                            isDayMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Month
                        Box(modifier = Modifier.weight(1.35f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isMonthMenuOpen = true }
                                    .testTag("dob_month_selector"),
                                color = Slate900,
                                border = BorderStroke(1.dp, Slate800),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (selectedMonth.isNotEmpty()) selectedMonth else "Month",
                                        color = if (selectedMonth.isNotEmpty()) Color.White else Slate500,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMonthMenuOpen,
                                onDismissRequest = { isMonthMenuOpen = false },
                                modifier = Modifier
                                    .background(Slate900)
                                    .heightIn(max = 240.dp)
                            ) {
                                PhoneUtils.MONTHS.forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = m,
                                                color = if (selectedMonth == m) Cyan400 else Slate200,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedMonth = m
                                            isMonthMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year
                        Box(modifier = Modifier.weight(1.1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isYearMenuOpen = true }
                                    .testTag("dob_year_selector"),
                                color = Slate900,
                                border = BorderStroke(1.dp, Slate800),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (selectedYear.isNotEmpty()) selectedYear else "Year",
                                        color = if (selectedYear.isNotEmpty()) Color.White else Slate500,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isYearMenuOpen,
                                onDismissRequest = { isYearMenuOpen = false },
                                modifier = Modifier
                                    .background(Slate900)
                                    .heightIn(max = 240.dp)
                            ) {
                                (2015 downTo 1940).forEach { y ->
                                    val yStr = y.toString()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = yStr,
                                                color = if (selectedYear == yStr) Cyan400 else Slate200,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedYear = yStr
                                            isYearMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Field 6: Password (with Show/Hide and bullet list)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Password", color = Slate300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = registerPassword,
                        onValueChange = { registerPassword = it },
                        placeholder = { Text("Enter password", color = Slate500, fontSize = 14.sp) },
                        visualTransformation = if (showRegisterPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                                Text(
                                    text = if (showRegisterPassword) "Hide" else "Show",
                                    color = Cyan400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password requirements checklist
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PasswordRequirementItem(ok = hasMinLength, label = "At least 8 characters")
                        PasswordRequirementItem(ok = hasLetter, label = "At least one letter")
                        PasswordRequirementItem(ok = hasNumber, label = "At least one number")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tip text
                    Text(
                        text = "Tip: mix upper and lower case letters, add a symbol like ! and use 12 or more characters for a stronger password.",
                        color = Slate400,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Error & Success Feedback
        if (errorMessage != null) {
            item {
                Surface(
                    color = Red400.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Red400.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Red400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage ?: "", color = Red400, fontSize = 13.sp)
                    }
                }
            }
        }

        if (successMessage != null) {
            item {
                Surface(
                    color = Emerald400.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(successMessage ?: "", color = Emerald400, fontSize = 13.sp)
                    }
                }
            }
        }

        // Submit Button (Log In or Create Account)
        item {
            Button(
                onClick = {
                    errorMessage = null
                    if (mode == "login") {
                        val identifier = if (loginMethod == "phone") loginPhone.trim() else loginEmail.trim()
                        if (identifier.isBlank()) {
                            errorMessage = if (loginMethod == "phone") "Please enter your phone number" else "Please enter your email"
                            return@Button
                        }
                        if (loginPassword.isBlank()) {
                            errorMessage = "Please enter your password"
                            return@Button
                        }

                        val ok = NillBookRepository.login(identifier, loginCountry)
                        if (ok) {
                            onDismiss()
                        } else {
                            errorMessage = "Account not found. Please create an account."
                        }
                    } else {
                        // Registration validations
                        if (registerName.isBlank()) {
                            errorMessage = "Please enter your full name"
                            return@Button
                        }
                        if (registerEmail.isBlank() || !registerEmail.contains("@")) {
                            errorMessage = "Please enter a valid email address"
                            return@Button
                        }
                        if (registerPhone.isBlank()) {
                            errorMessage = "Please enter your phone number"
                            return@Button
                        }
                        if (!isPasswordValid) {
                            errorMessage = "Password must have at least 8 characters, a letter and a number"
                            return@Button
                        }

                        val selectedCountryObj = PhoneUtils.COUNTRIES.find { it.code == selectedCountry }
                            ?: PhoneUtils.COUNTRIES.first()
                        val formattedPhone = PhoneUtils.toE164(registerPhone, selectedCountry)
                            ?: (selectedCountryObj.dialCode + registerPhone.trimStart('0').replace(Regex("[^0-9]"), ""))

                        val monthIndex = PhoneUtils.MONTHS.indexOf(selectedMonth)
                        val birthdayString = if (selectedYear.isNotEmpty() && monthIndex >= 0 && selectedDay.isNotEmpty()) {
                            "$selectedYear-${(monthIndex + 1).toString().padStart(2, '0')}-${selectedDay.padStart(2, '0')}"
                        } else {
                            "1998-01-01"
                        }

                        val genderValue = if (registerGender.isNotBlank()) registerGender else "Prefer not to say"

                        val registered = NillBookRepository.register(
                            name = registerName,
                            email = registerEmail,
                            phone = formattedPhone,
                            gender = genderValue,
                            birthday = birthdayString
                        )

                        if (registered) {
                            onDismiss()
                        } else {
                            errorMessage = "Could not create account. Please try again."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button")
            ) {
                Text(
                    text = if (mode == "register") "Create account" else "Log In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Bottom link to toggle mode
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == "register") "Already have an account? " else "Don't have an account? ",
                    color = Slate400,
                    fontSize = 13.sp
                )
                Text(
                    text = if (mode == "register") "Log In" else "Create account",
                    color = Cyan400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        mode = if (mode == "register") "login" else "register"
                        errorMessage = null
                    }
                )
            }
        }

        // Generous bottom space so typing password, viewing requirements, and clicking submit have plenty of free space above keyboard
        item {
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
fun PasswordRequirementItem(ok: Boolean, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = if (ok) "●" else "○",
            color = if (ok) Emerald400 else Slate400,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (ok) Slate200 else Slate400,
            fontSize = 13.sp
        )
    }
}
