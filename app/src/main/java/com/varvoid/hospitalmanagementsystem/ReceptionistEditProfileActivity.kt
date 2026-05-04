package com.varvoid.hospitalmanagementsystem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.Info

class ReceptionistEditProfileActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                ReceptionistEditProfileScreen(
                    auth = auth,
                    db = db,
                    context = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionistEditProfileScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var assignedDoctorName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val userId = auth.currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                phone = doc.getString("phone") ?: ""
                gender = doc.getString("gender") ?: "Female"
                city = doc.getString("city") ?: ""
                address = doc.getString("address") ?: ""
                assignedDoctorName = doc.getString("assignedDoctorName") ?: ""
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Assignment Info (read only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Assigned to: Dr. $assignedDoctorName",
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Personal Info
                ProfileCard(title = "👤 Personal Information") {
                    ProfileTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                    ProfileTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")
                    Text("Gender", fontSize = 14.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { option ->
                            FilterChip(
                                selected = gender == option,
                                onClick = { gender = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                // Address
                ProfileCard(title = "📍 Address") {
                    ProfileTextField(value = city, onValueChange = { city = it }, label = "City")
                    ProfileTextField(value = address, onValueChange = { address = it }, label = "Full Address")
                }

                // Save Button
                Button(
                    onClick = {
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        val updates = hashMapOf<String, Any>(
                            "name" to name,
                            "phone" to phone,
                            "gender" to gender,
                            "city" to city,
                            "address" to address
                        )
                        db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                isSaving = false
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Failed to update!", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Save Changes", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}