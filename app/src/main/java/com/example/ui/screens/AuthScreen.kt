package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf("login") } // "login" or "register"
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf("BD") }
    var isCountryMenuOpen by remember { mutableStateOf(false) }

    // Registration extras
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPhone by remember { mutableStateOf("") }
    var registerGender by remember { mutableStateOf("Male") }
    var registerBirthday by remember { mutableStateOf("1996-01-01") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val isPhoneIdentifier = identifier.isNotBlank() && !identifier.contains("@")

    // Password strength check (ported from register/page.tsx)
    val hasMinLength = password.length >= 8
    val hasLetter = password.any { it.isLetter() }
    val hasNumber = password.any { it.isDigit() }
    val isPasswordValid = hasMinLength && hasLetter && hasNumber

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                }
            }
        }

        // Branding
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "NILLBOOK Logo",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("NILLBOOK", color = Cyan400, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    text = "SMART CONTACT & CALLER NETWORK",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Tabs
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

        // Form fields
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (mode == "login") {
                        Text("Log In to NILLBOOK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Enter your registered email or phone number.", color = Slate400, fontSize = 12.sp)

                        if (isPhoneIdentifier) {
                            ExposedDropdownMenuBox(
                                expanded = isCountryMenuOpen,
                                onExpandedChange = { isCountryMenuOpen = !isCountryMenuOpen },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val currentCountry = PhoneUtils.COUNTRIES.find { it.code == selectedCountry } ?: PhoneUtils.COUNTRIES.first()
                                OutlinedTextField(
                                    value = currentCountry.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Country", color = Slate400) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryMenuOpen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Cyan400,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Slate950,
                                        unfocusedContainerColor = Slate950
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
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

                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Email or Phone Number", color = Slate400) },
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
                            modifier = Modifier.fillMaxWidth().testTag("auth_identifier_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Slate400) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Cyan400
                                    )
                                }
                            },
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
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                        )
                    } else {
                        // Registration mode
                        Text("Create Your Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)

                        OutlinedTextField(
                            value = registerName,
                            onValueChange = { registerName = it },
                            label = { Text("Full Name *", color = Slate400) },
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
                            modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                        )

                        OutlinedTextField(
                            value = registerEmail,
                            onValueChange = { registerEmail = it },
                            label = { Text("Email Address *", color = Slate400) },
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
                            value = registerPhone,
                            onValueChange = { registerPhone = it },
                            label = { Text("Phone Number *", color = Slate400) },
                            placeholder = { Text("+8801...", color = Slate600) },
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
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password *", color = Slate400) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Cyan400
                                    )
                                }
                            },
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

                        // Password checklist rules
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            PasswordRuleItem(ok = hasMinLength, text = "At least 8 characters")
                            PasswordRuleItem(ok = hasLetter, text = "Contains letters")
                            PasswordRuleItem(ok = hasNumber, text = "Contains numbers")
                        }

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password *", color = Slate400) },
                            visualTransformation = PasswordVisualTransformation(),
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
                    }

                    if (errorMessage != null) {
                        Text(errorMessage ?: "", color = Red400, fontSize = 13.sp)
                    }
                    if (successMessage != null) {
                        Text(successMessage ?: "", color = Emerald400, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            if (mode == "login") {
                                if (identifier.isBlank()) {
                                    errorMessage = "Please enter email or phone"
                                    return@Button
                                }
                                val ok = NillBookRepository.login(identifier, selectedCountry)
                                if (ok) {
                                    onDismiss()
                                } else {
                                    errorMessage = "Invalid email or phone"
                                }
                            } else {
                                if (registerName.isBlank() || registerEmail.isBlank() || registerPhone.isBlank()) {
                                    errorMessage = "Please fill in all required fields"
                                    return@Button
                                }
                                if (!isPasswordValid) {
                                    errorMessage = "Password does not meet requirements"
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    return@Button
                                }
                                val ok = NillBookRepository.register(
                                    registerName,
                                    registerEmail,
                                    registerPhone,
                                    registerGender,
                                    registerBirthday
                                )
                                if (ok) {
                                    onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = if (mode == "login") "Log In" else "Create Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Quick Demo Accounts to switch easily
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "QUICK SWITCH TO DEMO USER:",
                        color = Slate500,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                NillBookRepository.login("+8801700000000", "BD")
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rahim Bhai", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                NillBookRepository.login("+8801819876543", "BD")
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Farhana", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                NillBookRepository.login("+8801712345678", "BD")
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Blancho", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordRuleItem(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (ok) Emerald400 else Slate600,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = if (ok) Emerald400 else Slate500, fontSize = 11.sp)
    }
}
