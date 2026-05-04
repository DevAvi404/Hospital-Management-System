package com.varvoid.hospitalmanagementsystem

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

data class DoctorRequest(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val specialization: String = "",
    val qualifications: String = "",
    val hospitalName: String = "",
    val experience: String = ""
)

class PendingDoctorsActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                PendingDoctorsScreen(
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
fun PendingDoctorsScreen(
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var pendingDoctors by remember { mutableStateOf(listOf<DoctorRequest>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load pending doctors
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .whereEqualTo("isApproved", false)
            .get()
            .addOnSuccessListener { documents ->
                pendingDoctors = documents.map { doc ->
                    DoctorRequest(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        specialization = doc.getString("specialization") ?: "Not set",
                        qualifications = doc.getString("qualifications") ?: "Not set",
                        hospitalName = doc.getString("hospitalName") ?: "Not set",
                        experience = doc.getString("experience") ?: "Not set"
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Approvals", fontWeight = FontWeight.Bold) },
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
        } else if (pendingDoctors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "No pending",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Pending Approvals!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "All doctors have been reviewed",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${pendingDoctors.size} doctor(s) waiting for approval",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(pendingDoctors) { doctor ->
                    PendingDoctorCard(
                        doctor = doctor,
                        onApprove = {
                            db.collection("users").document(doctor.userId)
                                .update("isApproved", true)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "${doctor.name} approved!", Toast.LENGTH_SHORT).show()
                                    pendingDoctors = pendingDoctors.filter { it.userId != doctor.userId }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to approve", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onReject = {
                            db.collection("users").document(doctor.userId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "${doctor.name} rejected & removed!", Toast.LENGTH_SHORT).show()
                                    pendingDoctors = pendingDoctors.filter { it.userId != doctor.userId }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to reject", Toast.LENGTH_SHORT).show()
                                }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PendingDoctorCard(
    doctor: DoctorRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
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
                Column {
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
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Details
            DoctorInfoRow(label = "Specialization", value = doctor.specialization)
            DoctorInfoRow(label = "Qualifications", value = doctor.qualifications)
            DoctorInfoRow(label = "Hospital", value = doctor.hospitalName)
            DoctorInfoRow(label = "Experience", value = "${doctor.experience} years")

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reject Button
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Reject", tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", color = Color.Red)
                }

                // Approve Button
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Approve", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DoctorInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1565C0)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Color.DarkGray
        )
    }
}