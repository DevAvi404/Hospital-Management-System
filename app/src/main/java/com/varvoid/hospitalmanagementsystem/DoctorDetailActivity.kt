package com.varvoid.hospitalmanagementsystem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

class DoctorDetailActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        val doctorId = intent.getStringExtra("doctorId") ?: ""

        setContent {
            MaterialTheme {
                DoctorDetailScreen(
                    db = db,
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
fun DoctorDetailScreen(
    db: FirebaseFirestore,
    doctorId: String,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    // Fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var specializationTags by remember { mutableStateOf("") }
    var qualifications by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var medicalSchool by remember { mutableStateOf("") }
    var numberOfPapers by remember { mutableStateOf("") }
    var researchAreas by remember { mutableStateOf("") }
    var papers by remember { mutableStateOf(listOf<ResearchPaper>()) }
    var visitFee by remember { mutableStateOf("") }
    var followUpFee by remember { mutableStateOf("") }
    var availableDays by remember { mutableStateOf(listOf<String>()) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var maxPatients by remember { mutableStateOf("") }
    var telemedicine by remember { mutableStateOf(false) }
    var emergencyAvailable by remember { mutableStateOf(false) }
    var onlinePrescription by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf("") }
    var chamberAddress by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isApproved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("users").document(doctorId)
            .get()
            .addOnSuccessListener { document ->
                name = document.getString("name") ?: ""
                email = document.getString("email") ?: ""
                phone = document.getString("phone") ?: ""
                gender = document.getString("gender") ?: ""
                dateOfBirth = document.getString("dateOfBirth") ?: ""
                age = document.getString("age") ?: ""
                specialization = document.getString("specialization") ?: ""
                specializationTags = document.getString("specializationTags") ?: ""
                qualifications = document.getString("qualifications") ?: ""
                experience = document.getString("experience") ?: ""
                licenseNumber = document.getString("licenseNumber") ?: ""
                hospitalName = document.getString("hospitalName") ?: ""
                medicalSchool = document.getString("medicalSchool") ?: ""
                numberOfPapers = document.getString("numberOfPapers") ?: ""
                researchAreas = document.getString("researchAreas") ?: ""
                visitFee = document.getString("visitFee") ?: ""
                followUpFee = document.getString("followUpFee") ?: ""
                startTime = document.getString("startTime") ?: ""
                endTime = document.getString("endTime") ?: ""
                maxPatients = document.getString("maxPatients") ?: ""
                telemedicine = document.getBoolean("telemedicine") ?: false
                emergencyAvailable = document.getBoolean("emergencyAvailable") ?: false
                onlinePrescription = document.getBoolean("onlinePrescription") ?: false
                city = document.getString("city") ?: ""
                chamberAddress = document.getString("chamberAddress") ?: ""
                bio = document.getString("bio") ?: ""
                isApproved = document.getBoolean("isApproved") ?: false
                val papersList = (document.get("papers") as? List<*>)
                    ?.filterIsInstance<Map<*, *>>()
                    ?.map { ResearchPaper(it["title"] as? String ?: "", it["link"] as? String ?: "") }
                if (!papersList.isNullOrEmpty()) papers = papersList
                availableDays = (document.get("availableDays") as? List<*>)
                    ?.filterIsInstance<String>() ?: listOf()
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load doctor", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Details", fontWeight = FontWeight.Bold) },
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
                            contentDescription = "Doctor",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dr. $name",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = specialization,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isApproved) Color(0xFF4CAF50) else Color(0xFFF57F17)
                            )
                        ) {
                            Text(
                                text = if (isApproved) "✓ Approved" else "⏳ Pending",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Personal Info
                DetailCard(title = "👤 Personal Information") {
                    DetailRow(label = "Email", value = email)
                    DetailRow(label = "Phone", value = phone)
                    DetailRow(label = "Gender", value = gender)
                    DetailRow(label = "Date of Birth", value = dateOfBirth)
                    DetailRow(label = "Age", value = "$age years")
                }

                // Professional Info
                DetailCard(title = "🏥 Professional Information") {
                    DetailRow(label = "Specialization", value = specialization)
                    DetailRow(label = "Tags", value = specializationTags)
                    DetailRow(label = "Qualifications", value = qualifications)
                    DetailRow(label = "Experience", value = "$experience years")
                    DetailRow(label = "License No.", value = licenseNumber)
                    DetailRow(label = "Hospital", value = hospitalName)
                    DetailRow(label = "Medical School", value = medicalSchool)
                }

                // Research
                if (numberOfPapers.isNotEmpty()) {
                    DetailCard(title = "📚 Research & Publications") {
                        DetailRow(label = "Papers Published", value = numberOfPapers)
                        DetailRow(label = "Research Areas", value = researchAreas)
                        if (papers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Publications:",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1565C0),
                                fontSize = 14.sp
                            )
                            papers.forEachIndexed { index, paper ->
                                if (paper.title.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val localContext = LocalContext.current
                                    Text(
                                        text = "${index + 1}. ${paper.title}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF1565C0),
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable {
                                            if (paper.link.isNotEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paper.link))
                                                localContext.startActivity(intent)
                                            } else {
                                                Toast.makeText(localContext, "No link available", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Consultation
                DetailCard(title = "💰 Consultation Fees") {
                    DetailRow(label = "Visit Fee", value = "BDT $visitFee")
                    DetailRow(label = "Follow-up Fee", value = "BDT $followUpFee")
                }

                // Availability
                DetailCard(title = "📅 Availability") {
                    DetailRow(label = "Available Days", value = availableDays.joinToString(", "))
                    DetailRow(label = "Time", value = "$startTime - $endTime")
                    DetailRow(label = "Max Patients/Day", value = maxPatients)
                }

                // Services
                DetailCard(title = "⚙️ Services") {
                    ServiceRow(label = "Telemedicine", available = telemedicine)
                    ServiceRow(label = "Emergency Available", available = emergencyAvailable)
                    ServiceRow(label = "Online Prescription", available = onlinePrescription)
                }

                // Address
                DetailCard(title = "📍 Address") {
                    DetailRow(label = "City", value = city)
                    DetailRow(label = "Chamber", value = chamberAddress)
                }

                // About
                if (bio.isNotEmpty()) {
                    DetailCard(title = "📝 About") {
                        Text(
                            text = bio,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    if (value.isNotEmpty() && value != "null") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "$label: ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1565C0),
                modifier = Modifier.width(120.dp)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ServiceRow(label: String, available: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.DarkGray)
        Icon(
            imageVector = if (available) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = label,
            tint = if (available) Color(0xFF4CAF50) else Color.Red,
            modifier = Modifier.size(20.dp)
        )
    }
}