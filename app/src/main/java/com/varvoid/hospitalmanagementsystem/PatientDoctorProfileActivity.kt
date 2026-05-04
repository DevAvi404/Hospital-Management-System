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

class PatientDoctorProfileActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        val doctorId = intent.getStringExtra("doctorId") ?: ""

        setContent {
            MaterialTheme {
                PatientDoctorProfileScreen(
                    db = db,
                    doctorId = doctorId,
                    context = this,
                    onBack = { finish() },
                    onBookAppointment = {
                        val intent = Intent(this, BookAppointmentActivity::class.java)
                        intent.putExtra("doctorId", doctorId)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDoctorProfileScreen(
    db: FirebaseFirestore,
    doctorId: String,
    context: android.content.Context,
    onBack: () -> Unit,
    onBookAppointment: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var specializationTags by remember { mutableStateOf("") }
    var qualifications by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var visitFee by remember { mutableStateOf("") }
    var followUpFee by remember { mutableStateOf("") }
    var availableDays by remember { mutableStateOf(listOf<String>()) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var reportReviewDays by remember { mutableStateOf(listOf<String>()) }
    var reportReviewStartTime by remember { mutableStateOf("") }
    var reportReviewEndTime by remember { mutableStateOf("") }
    var telemedicine by remember { mutableStateOf(false) }
    var emergencyAvailable by remember { mutableStateOf(false) }
    var onlinePrescription by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf("") }
    var chamberAddress by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var numberOfPapers by remember { mutableStateOf("") }
    var researchAreas by remember { mutableStateOf("") }
    var papers by remember { mutableStateOf(listOf<ResearchPaper>()) }
    var freeFollowUpDays by remember { mutableStateOf("7") }

    LaunchedEffect(Unit) {
        db.collection("users").document(doctorId)
            .get()
            .addOnSuccessListener { document ->
                name = document.getString("name") ?: ""
                specialization = document.getString("specialization") ?: ""
                specializationTags = document.getString("specializationTags") ?: ""
                qualifications = document.getString("qualifications") ?: ""
                experience = document.getString("experience") ?: ""
                hospitalName = document.getString("hospitalName") ?: ""
                visitFee = document.getString("visitFee") ?: ""
                followUpFee = document.getString("followUpFee") ?: ""
                startTime = document.getString("startTime") ?: ""
                endTime = document.getString("endTime") ?: ""
                reportReviewStartTime = document.getString("reportReviewStartTime") ?: ""
                reportReviewEndTime = document.getString("reportReviewEndTime") ?: ""
                telemedicine = document.getBoolean("telemedicine") ?: false
                emergencyAvailable = document.getBoolean("emergencyAvailable") ?: false
                onlinePrescription = document.getBoolean("onlinePrescription") ?: false
                city = document.getString("city") ?: ""
                chamberAddress = document.getString("chamberAddress") ?: ""
                bio = document.getString("bio") ?: ""
                numberOfPapers = document.getString("numberOfPapers") ?: ""
                researchAreas = document.getString("researchAreas") ?: ""
                freeFollowUpDays = document.getString("freeFollowUpDays") ?: "7"
                availableDays = (document.get("availableDays") as? List<*>)?.filterIsInstance<String>() ?: listOf()
                reportReviewDays = (document.get("reportReviewDays") as? List<*>)?.filterIsInstance<String>() ?: listOf()
                val papersList = (document.get("papers") as? List<*>)
                    ?.filterIsInstance<Map<*, *>>()
                    ?.map { ResearchPaper(it["title"] as? String ?: "", it["link"] as? String ?: "") }
                if (!papersList.isNullOrEmpty()) papers = papersList
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load doctor profile", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Profile", fontWeight = FontWeight.Bold) },
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
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onBookAppointment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = "Book", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book Appointment", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
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
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Dr. $name", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(specialization, fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f))
                        Text(hospitalName, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(12.dp))
                        // Tags
                        if (specializationTags.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                specializationTags.split(",").take(3).forEach { tag ->
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            tag.trim(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickInfoCard(modifier = Modifier.weight(1f), icon = Icons.Filled.WorkHistory, title = "Experience", value = "$experience yrs")
                    QuickInfoCard(modifier = Modifier.weight(1f), icon = Icons.Filled.School, title = "Qualification", value = qualifications.split(",").firstOrNull()?.trim() ?: "N/A")
                }

                // Fees Card
                DetailCard(title = "💰 Consultation Fees") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Visit Fee", fontSize = 12.sp, color = Color.Gray)
                            Text("BDT $visitFee", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Follow-up Fee", fontSize = 12.sp, color = Color.Gray)
                            Text("BDT $followUpFee", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text(
                            "✅ Free follow-up within $freeFollowUpDays days | Half fee within 30 days",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Availability Card
                DetailCard(title = "📅 Availability") {
                    Text("Normal Appointments", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableDays.forEach { day ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                            ) {
                                Text(day, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                    Text("Time: $startTime - $endTime", fontSize = 13.sp, color = Color.DarkGray)

                    if (reportReviewDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Report Review", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            reportReviewDays.forEach { day ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26C6DA))
                                ) {
                                    Text(day, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                        Text("Time: $reportReviewStartTime - $reportReviewEndTime", fontSize = 13.sp, color = Color.DarkGray)
                    }
                }

                // Services Card
                DetailCard(title = "⚙️ Services") {
                    ServiceRow(label = "Telemedicine Available", available = telemedicine)
                    ServiceRow(label = "Emergency Available", available = emergencyAvailable)
                    ServiceRow(label = "Online Prescription", available = onlinePrescription)
                }

                // Location Card
                if (city.isNotEmpty() || chamberAddress.isNotEmpty()) {
                    DetailCard(title = "📍 Location") {
                        DetailRow(label = "City", value = city)
                        DetailRow(label = "Chamber", value = chamberAddress)
                    }
                }

                // Research Card
                if (numberOfPapers.isNotEmpty() && numberOfPapers != "0") {
                    DetailCard(title = "📚 Research & Publications") {
                        DetailRow(label = "Papers Published", value = numberOfPapers)
                        DetailRow(label = "Research Areas", value = researchAreas)
                        if (papers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Publications:", fontWeight = FontWeight.Medium, color = Color(0xFF1565C0), fontSize = 14.sp)
                            papers.forEachIndexed { index, paper ->
                                if (paper.title.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
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
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // About Card
                if (bio.isNotEmpty()) {
                    DetailCard(title = "📝 About") {
                        Text(bio, fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun QuickInfoCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFF1565C0), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }
    }
}