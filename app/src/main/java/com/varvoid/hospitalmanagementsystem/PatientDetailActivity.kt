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

class PatientDetailActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        val patientId = intent.getStringExtra("patientId") ?: ""

        setContent {
            MaterialTheme {
                PatientDetailScreen(
                    db = db,
                    patientId = patientId,
                    context = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    db: FirebaseFirestore,
    patientId: String,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    // Personal
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }

    // Medical
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var chronicDiseases by remember { mutableStateOf("") }
    var currentMedications by remember { mutableStateOf("") }

    // Emergency
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var emergencyRelation by remember { mutableStateOf("") }

    // Address
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").document(patientId)
            .get()
            .addOnSuccessListener { document ->
                name = document.getString("name") ?: ""
                email = document.getString("email") ?: ""
                phone = document.getString("phone") ?: ""
                gender = document.getString("gender") ?: ""
                dateOfBirth = document.getString("dateOfBirth") ?: ""
                age = document.getString("age") ?: ""
                bloodGroup = document.getString("bloodGroup") ?: ""
                weight = document.getString("weight") ?: ""
                height = document.getString("height") ?: ""
                allergies = document.getString("allergies") ?: ""
                chronicDiseases = document.getString("chronicDiseases") ?: ""
                currentMedications = document.getString("currentMedications") ?: ""
                emergencyName = document.getString("emergencyName") ?: ""
                emergencyPhone = document.getString("emergencyPhone") ?: ""
                emergencyRelation = document.getString("emergencyRelation") ?: ""
                city = document.getString("city") ?: ""
                address = document.getString("address") ?: ""
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load patient", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Details", fontWeight = FontWeight.Bold) },
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
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
                        Text(
                            text = name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = email,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Personal Info
                DetailCard(title = "👤 Personal Information") {
                    DetailRow(label = "Phone", value = phone)
                    DetailRow(label = "Gender", value = gender)
                    DetailRow(label = "Date of Birth", value = dateOfBirth)
                    DetailRow(label = "Age", value = "$age years")
                    DetailRow(label = "Blood Group", value = bloodGroup)
                }

                // Medical Info
                DetailCard(title = "🏥 Medical Information") {
                    DetailRow(label = "Weight", value = "$weight kg")
                    DetailRow(label = "Height", value = "$height cm")
                    DetailRow(label = "Allergies", value = allergies)
                    DetailRow(label = "Chronic Diseases", value = chronicDiseases)
                    DetailRow(label = "Medications", value = currentMedications)
                }

                // Emergency Contact
                DetailCard(title = "🚨 Emergency Contact") {
                    DetailRow(label = "Name", value = emergencyName)
                    DetailRow(label = "Phone", value = emergencyPhone)
                    DetailRow(label = "Relation", value = emergencyRelation)
                }

                // Address
                DetailCard(title = "📍 Address") {
                    DetailRow(label = "City", value = city)
                    DetailRow(label = "Address", value = address)
                }
            }
        }
    }
}