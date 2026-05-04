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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class DoctorOption(
    val id: String = "",
    val name: String = ""
)

class CreateReceptionistActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                CreateReceptionistScreen(
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
fun CreateReceptionistScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedDoctor by remember { mutableStateOf<DoctorOption?>(null) }
    var doctors by remember { mutableStateOf(listOf<DoctorOption>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // Load approved doctors
    LaunchedEffect(Unit) {
        isLoading = true
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .whereEqualTo("isApproved", true)
            .get()
            .addOnSuccessListener { docs ->
                doctors = docs.map { doc ->
                    DoctorOption(
                        id = doc.id,
                        name = doc.getString("name") ?: ""
                    )
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Receptionist", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color(0xFF1565C0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create a receptionist account and assign them to a doctor.",
                        fontSize = 13.sp,
                        color = Color(0xFF1565C0)
                    )
                }
            }

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Receptionist Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )

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
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    // Doctor Selection Dropdown
                    Text("Assign to Doctor", fontSize = 14.sp, color = Color.Gray)

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDoctor?.name ?: "Select a Doctor",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.LocalHospital, contentDescription = "Doctor")
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (isLoading) {
                                DropdownMenuItem(
                                    text = { Text("Loading doctors...") },
                                    onClick = {}
                                )
                            } else if (doctors.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No approved doctors found") },
                                    onClick = {}
                                )
                            } else {
                                doctors.forEach { doctor ->
                                    DropdownMenuItem(
                                        text = { Text("Dr. ${doctor.name}") },
                                        onClick = {
                                            selectedDoctor = doctor
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Create Button
            Button(
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedDoctor == null) {
                        Toast.makeText(context, "Please select a doctor!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isCreating = true

                    // Save current admin user
                    val currentAdmin = auth.currentUser

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val receptionistId = auth.currentUser!!.uid
                                val receptionistData = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "role" to "receptionist",
                                    "assignedDoctorId" to selectedDoctor!!.id,
                                    "assignedDoctorName" to selectedDoctor!!.name,
                                    "isApproved" to true,
                                    "createdAt" to System.currentTimeMillis()
                                )

                                db.collection("users").document(receptionistId)
                                    .set(receptionistData)
                                    .addOnSuccessListener {
                                        isCreating = false
                                        Toast.makeText(
                                            context,
                                            "Receptionist account created successfully!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        // Clear form
                                        name = ""
                                        email = ""
                                        password = ""
                                        selectedDoctor = null
                                    }
                                    .addOnFailureListener {
                                        isCreating = false
                                        Toast.makeText(context, "Failed to save data!", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                isCreating = false
                                Toast.makeText(
                                    context,
                                    "Failed: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Create", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Receptionist Account", fontSize = 16.sp)
                }
            }
        }
    }
}