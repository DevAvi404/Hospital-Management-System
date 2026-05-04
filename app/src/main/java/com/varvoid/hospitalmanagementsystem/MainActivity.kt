package com.varvoid.hospitalmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: "patient"
                    when (role) {
                        "admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                        "doctor" -> startActivity(Intent(this, DoctorDashboardActivity::class.java))
                        "receptionist" -> startActivity(Intent(this, ReceptionistDashboardActivity::class.java))
                        else -> startActivity(Intent(this, PatientDashboardActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    showLoginScreen()
                }
            return
        }

        showLoginScreen()
    }

    private fun showLoginScreen() {
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginSuccess = { role ->
                        when (role) {
                            "admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                            "doctor" -> startActivity(Intent(this, DoctorDashboardActivity::class.java))
                            "receptionist" -> startActivity(Intent(this, ReceptionistDashboardActivity::class.java))
                            else -> startActivity(Intent(this, PatientDashboardActivity::class.java))
                        }
                        finish()
                    },
                    onSignUpClick = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                    },
                    auth = auth,
                    db = db,
                    context = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onSignUpClick: () -> Unit,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("Patient") }
    var roleExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotDone by remember { mutableStateOf(false) }
    var isSendingReset by remember { mutableStateOf(false) }

    val roles = listOf(
        Pair("Patient", Icons.Filled.Person),
        Pair("Doctor", Icons.Filled.MedicalServices),
        Pair("Receptionist", Icons.Filled.SupportAgent),
        Pair("Admin", Icons.Filled.AdminPanelSettings)
    )

    // ── Forgot Password Dialog ──
    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingReset) {
                    showForgotPassword = false
                    forgotEmail = ""
                    forgotDone = false
                }
            },
            title = {
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (forgotDone) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Done",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Reset link sent!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Text(
                                    "Check your inbox and spam folder.",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    } else {
                        Text(
                            "Enter your registered email address. We'll send a password reset link.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isSendingReset,
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Email,
                                    contentDescription = "Email",
                                    tint = Color(0xFF1565C0)
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                if (!forgotDone) {
                    Button(
                        onClick = {
                            if (forgotEmail.isEmpty() || !forgotEmail.contains("@")) {
                                Toast.makeText(context, "Enter a valid email!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSendingReset = true
                            auth.sendPasswordResetEmail(forgotEmail)
                                .addOnSuccessListener {
                                    isSendingReset = false
                                    forgotDone = true
                                }
                                .addOnFailureListener {
                                    isSendingReset = false
                                    Toast.makeText(context, "No account found with this email.", Toast.LENGTH_LONG).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = !isSendingReset
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...")
                        } else {
                            Text("Send Reset Link")
                        }
                    }
                } else {
                    TextButton(onClick = {
                        showForgotPassword = false
                        forgotEmail = ""
                        forgotDone = false
                    }) { Text("Close") }
                }
            },
            dismissButton = {
                if (!forgotDone && !isSendingReset) {
                    TextButton(onClick = {
                        showForgotPassword = false
                        forgotEmail = ""
                    }) { Text("Cancel") }
                }
            }
        )
    }

    // ── Main Login UI ──
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "🏥 Hospital Management",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
                Text("Sign in to continue", fontSize = 14.sp, color = Color.Gray)

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = "Email", tint = Color(0xFF1565C0))
                    }
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Password")
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle",
                                tint = Color.Gray
                            )
                        }
                    }
                )

                // Role dropdown
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Login As") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                roles.find { it.first == selectedRole }?.second ?: Icons.Filled.Person,
                                contentDescription = selectedRole,
                                tint = Color(0xFF1565C0)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        roles.forEach { (role, icon) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(icon, contentDescription = role, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                                        Text(role)
                                    }
                                },
                                onClick = { selectedRole = role; roleExpanded = false }
                            )
                        }
                    }
                }

                // Login Button
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!email.contains("@")) {
                            Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser!!.uid
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            isLoading = false
                                            if (!doc.exists()) {
                                                Toast.makeText(context, "Account not found!", Toast.LENGTH_LONG).show()
                                                auth.signOut()
                                                return@addOnSuccessListener
                                            }
                                            val role = doc.getString("role") ?: "patient"
                                            if (role != selectedRole.lowercase()) {
                                                Toast.makeText(context, "Wrong role! You are registered as: $role", Toast.LENGTH_LONG).show()
                                                auth.signOut()
                                                return@addOnSuccessListener
                                            }
                                            onLoginSuccess(role)
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Failed to load profile", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Login failed: Wrong email or password", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Login", fontSize = 16.sp)
                    }
                }

                TextButton(onClick = { showForgotPassword = true }) {
                    Text("Forgot Password?", color = Color(0xFF1565C0), fontSize = 13.sp)
                }

                TextButton(onClick = onSignUpClick) {
                    Text("Don't have an account? Sign Up", color = Color(0xFF1565C0))
                }
            }
        }
    }
}