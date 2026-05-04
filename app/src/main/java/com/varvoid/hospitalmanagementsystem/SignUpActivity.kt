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

class SignUpActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                SignUpScreen(
                    onSignUpSuccess = { role ->
                        when (role) {
                            "doctor" -> startActivity(Intent(this, DoctorDashboardActivity::class.java))
                            else -> startActivity(Intent(this, PatientDashboardActivity::class.java))
                        }
                        finish()
                    },
                    onLoginClick = { finish() },
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
fun SignUpScreen(
    onSignUpSuccess: (String) -> Unit,
    onLoginClick: () -> Unit,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("patient") }
    var isLoading by remember { mutableStateOf(false) }

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
                    "Create Account",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
                Text("Join us today!", fontSize = 14.sp, color = Color.Gray)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email", tint = Color(0xFF1565C0)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
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

                Text("Sign up as:", fontSize = 14.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == "patient",
                        onClick = { selectedRole = "patient" },
                        label = { Text("Patient") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == "doctor",
                        onClick = { selectedRole = "doctor" },
                        label = { Text("Doctor") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!email.contains("@")) {
                            Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser!!.uid
                                    val userMap = hashMapOf<String, Any>(
                                        "name" to name,
                                        "email" to email,
                                        "role" to selectedRole,
                                        "isApproved" to (selectedRole == "patient")
                                    )
                                    db.collection("users").document(uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            isLoading = false

                                            // ✅ If doctor signed up, notify all admins
                                            if (selectedRole == "doctor") {
                                                notifyAdminsOfNewDoctor(db = db, doctorName = name)
                                            }

                                            Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                            onSignUpSuccess(selectedRole)
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Failed to save data", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Sign Up Failed: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
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
                        Text("Sign Up", fontSize = 16.sp)
                    }
                }

                TextButton(onClick = onLoginClick) {
                    Text("Already have an account? Login", color = Color(0xFF1565C0))
                }
            }
        }
    }
}
fun notifyAdminsOfNewDoctor(db: FirebaseFirestore, doctorName: String) {
    // Find all admin accounts
    db.collection("users")
        .whereEqualTo("role", "admin")
        .get()
        .addOnSuccessListener { admins ->
            admins.documents.forEach { adminDoc ->
                val adminId = adminDoc.id
                db.collection("notifications").add(
                    mapOf(
                        "userId" to adminId,
                        "title" to "🆕 New Doctor Registration",
                        "message" to "Dr. $doctorName has signed up and is waiting for your approval. Go to Pending Approvals to review.",
                        "read" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            }
        }
}