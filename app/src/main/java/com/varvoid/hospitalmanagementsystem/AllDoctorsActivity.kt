package com.varvoid.hospitalmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

data class DoctorItem(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val specialization: String = "",
    val hospitalName: String = "",
    val experience: String = "",
    val isApproved: Boolean = false
)

class AllDoctorsActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                AllDoctorsScreen(
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
fun AllDoctorsScreen(
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var doctors by remember { mutableStateOf(listOf<DoctorItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedDoctor by remember { mutableStateOf<DoctorItem?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { documents ->
                doctors = documents.map { doc ->
                    DoctorItem(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        specialization = doc.getString("specialization") ?: "Not set",
                        hospitalName = doc.getString("hospitalName") ?: "Not set",
                        experience = doc.getString("experience") ?: "0",
                        isApproved = doc.getBoolean("isApproved") ?: false
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedDoctor != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Doctor") },
            text = { Text("Are you sure you want to remove Dr. ${selectedDoctor!!.name}? This action cannot be undone!") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("users").document(selectedDoctor!!.userId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "${selectedDoctor!!.name} removed!", Toast.LENGTH_SHORT).show()
                                doctors = doctors.filter { it.userId != selectedDoctor!!.userId }
                                showDeleteDialog = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Doctors", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search doctors...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredDoctors = doctors.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.specialization.contains(searchQuery, ignoreCase = true) ||
                            it.hospitalName.contains(searchQuery, ignoreCase = true)
                }

                Text(
                    text = "${filteredDoctors.size} doctor(s) found",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (filteredDoctors.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No doctors found",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredDoctors) { doctor ->
                            DoctorItemCard(
                                doctor = doctor,
                                onViewDetails = {
                                    val intent = Intent(context, DoctorDetailActivity::class.java)
                                    intent.putExtra("doctorId", doctor.userId)
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    selectedDoctor = doctor
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorItemCard(
    doctor: DoctorItem,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Doctor",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dr. ${doctor.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = doctor.email,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (doctor.isApproved) Color(0xFF4CAF50) else Color(0xFFF57F17)
                    )
                ) {
                    Text(
                        text = if (doctor.isApproved) "Approved" else "Pending",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            DoctorInfoRow(label = "Specialization", value = doctor.specialization)
            DoctorInfoRow(label = "Hospital", value = doctor.hospitalName)
            DoctorInfoRow(label = "Experience", value = "${doctor.experience} years")

            Spacer(modifier = Modifier.height(12.dp))

            // View Details Button
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
            ) {
                Icon(Icons.Filled.Visibility, contentDescription = "View", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full Profile", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remove Button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove Doctor", color = Color.Red)
            }
        }
    }
}