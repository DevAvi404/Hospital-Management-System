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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

class DoctorPatientDetailsActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        val patientId = intent.getStringExtra("patientId") ?: ""
        val doctorId = intent.getStringExtra("doctorId") ?: ""

        setContent {
            MaterialTheme {
                DoctorPatientDetailsScreen(
                    db = db,
                    patientId = patientId,
                    doctorId = doctorId,
                    context = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientDetailsScreen(
    db: FirebaseFirestore,
    patientId: String,
    doctorId: String,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var chronicDiseases by remember { mutableStateOf("") }
    var currentMedications by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var emergencyRelation by remember { mutableStateOf("") }
    var previousAppointments by remember { mutableStateOf(listOf<QueueAppointment>()) }

    LaunchedEffect(Unit) {
        // Load patient info
        db.collection("users").document(patientId)
            .get()
            .addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                email = doc.getString("email") ?: ""
                phone = doc.getString("phone") ?: ""
                gender = doc.getString("gender") ?: ""
                age = doc.getString("age") ?: ""
                bloodGroup = doc.getString("bloodGroup") ?: ""
                weight = doc.getString("weight") ?: ""
                height = doc.getString("height") ?: ""
                allergies = doc.getString("allergies") ?: ""
                chronicDiseases = doc.getString("chronicDiseases") ?: ""
                currentMedications = doc.getString("currentMedications") ?: ""
                emergencyName = doc.getString("emergencyName") ?: ""
                emergencyPhone = doc.getString("emergencyPhone") ?: ""
                emergencyRelation = doc.getString("emergencyRelation") ?: ""
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load patient", Toast.LENGTH_SHORT).show()
            }

        // Load previous appointments with this doctor
        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { docs ->
                previousAppointments = docs.map { doc ->
                    QueueAppointment(
                        id = doc.id,
                        patientName = doc.getString("patientName") ?: "",
                        serialNumber = (doc.getLong("serialNumber") ?: 0).toInt(),
                        appointmentType = doc.getString("appointmentType") ?: "new",
                        reason = doc.getString("reason") ?: "",
                        fee = doc.getString("fee") ?: "0",
                        status = doc.getString("status") ?: "pending"
                    )
                }.sortedByDescending { it.serialNumber }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Details", fontWeight = FontWeight.Bold) },
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
                // Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Patient",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                // Personal Info
                DetailCard(title = "👤 Personal Information") {
                    DetailRow(label = "Phone", value = phone)
                    DetailRow(label = "Gender", value = gender)
                    DetailRow(label = "Age", value = "$age years")
                    DetailRow(label = "Blood Group", value = bloodGroup)
                }

                // Medical Info
                DetailCard(title = "🏥 Medical Information") {
                    DetailRow(label = "Weight", value = "$weight kg")
                    DetailRow(label = "Height", value = "$height cm")
                    DetailRow(label = "Allergies", value = allergies)
                    DetailRow(label = "Chronic Diseases", value = chronicDiseases)
                    DetailRow(label = "Current Medications", value = currentMedications)
                }

                // Emergency Contact
                DetailCard(title = "🚨 Emergency Contact") {
                    DetailRow(label = "Name", value = emergencyName)
                    DetailRow(label = "Phone", value = emergencyPhone)
                    DetailRow(label = "Relation", value = emergencyRelation)
                }

                // Previous Appointments
                if (previousAppointments.isNotEmpty()) {
                    DetailCard(title = "📅 Previous Appointments (${previousAppointments.size})") {
                        previousAppointments.forEach { appt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = when (appt.appointmentType) {
                                                "followup" -> "Follow-up"
                                                "report" -> "Report Review"
                                                else -> "New Visit"
                                            },
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = appt.reason,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = when (appt.status) {
                                                "completed" -> Color(0xFF4CAF50)
                                                "cancelled" -> Color.Red
                                                else -> Color(0xFFF57F17)
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = appt.status.replaceFirstChar { it.uppercase() },
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}