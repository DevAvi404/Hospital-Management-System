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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorEditProfileActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                DoctorEditProfileScreen(
                    auth = auth,
                    db = db,
                    context = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

data class ResearchPaper(
    val title: String = "",
    val link: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorEditProfileScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    // Personal
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var dateOfBirth by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    // Professional
    var specialization by remember { mutableStateOf("") }
    var specializationTags by remember { mutableStateOf("") }
    var qualifications by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var medicalSchool by remember { mutableStateOf("") }
    var graduationYear by remember { mutableStateOf("") }

    // Research
    var numberOfPapers by remember { mutableStateOf("") }
    var researchAreas by remember { mutableStateOf("") }
    var papers by remember { mutableStateOf(listOf(ResearchPaper())) }

    // Consultation Fees
    var visitFee by remember { mutableStateOf("") }
    var followUpFee by remember { mutableStateOf("") }

    // Normal Availability
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var maxPatients by remember { mutableStateOf("") }

    // Report Review
    var reportReviewStartTime by remember { mutableStateOf("") }
    var reportReviewEndTime by remember { mutableStateOf("") }
    var maxReportPatients by remember { mutableStateOf("") }
    var reportReviewDays by remember { mutableStateOf(setOf<String>()) }

    // Follow-up Policy
    var freeFollowUpDays by remember { mutableStateOf("7") }
    var halfFeeFollowUpDays by remember { mutableStateOf("30") }

    // Services
    var telemedicine by remember { mutableStateOf(false) }
    var emergencyAvailable by remember { mutableStateOf(false) }
    var onlinePrescription by remember { mutableStateOf(false) }

    // Address
    var city by remember { mutableStateOf("") }
    var chamberAddress by remember { mutableStateOf("") }

    // About
    var bio by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val userId = auth.currentUser?.uid

    // Load existing data
    LaunchedEffect(Unit) {
        isLoading = true
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    name = document.getString("name") ?: ""
                    phone = document.getString("phone") ?: ""
                    gender = document.getString("gender") ?: "Male"
                    dateOfBirth = document.getString("dateOfBirth") ?: ""
                    age = document.getString("age") ?: ""
                    specialization = document.getString("specialization") ?: ""
                    specializationTags = document.getString("specializationTags") ?: ""
                    qualifications = document.getString("qualifications") ?: ""
                    experience = document.getString("experience") ?: ""
                    licenseNumber = document.getString("licenseNumber") ?: ""
                    hospitalName = document.getString("hospitalName") ?: ""
                    medicalSchool = document.getString("medicalSchool") ?: ""
                    graduationYear = document.getString("graduationYear") ?: ""
                    numberOfPapers = document.getString("numberOfPapers") ?: ""
                    researchAreas = document.getString("researchAreas") ?: ""
                    visitFee = document.getString("visitFee") ?: ""
                    followUpFee = document.getString("followUpFee") ?: ""
                    startTime = document.getString("startTime") ?: ""
                    endTime = document.getString("endTime") ?: ""
                    maxPatients = document.getString("maxPatients") ?: ""
                    reportReviewStartTime = document.getString("reportReviewStartTime") ?: ""
                    reportReviewEndTime = document.getString("reportReviewEndTime") ?: ""
                    maxReportPatients = document.getString("maxReportPatients") ?: ""
                    freeFollowUpDays = document.getString("freeFollowUpDays") ?: "7"
                    halfFeeFollowUpDays = document.getString("halfFeeFollowUpDays") ?: "30"
                    telemedicine = document.getBoolean("telemedicine") ?: false
                    emergencyAvailable = document.getBoolean("emergencyAvailable") ?: false
                    onlinePrescription = document.getBoolean("onlinePrescription") ?: false
                    city = document.getString("city") ?: ""
                    chamberAddress = document.getString("chamberAddress") ?: ""
                    bio = document.getString("bio") ?: ""
                    selectedDays = (document.get("availableDays") as? List<*>)
                        ?.filterIsInstance<String>()?.toSet() ?: setOf()
                    reportReviewDays = (document.get("reportReviewDays") as? List<*>)
                        ?.filterIsInstance<String>()?.toSet() ?: setOf()
                    val papersList = (document.get("papers") as? List<*>)
                        ?.filterIsInstance<Map<*, *>>()
                        ?.map { ResearchPaper(it["title"] as? String ?: "", it["link"] as? String ?: "") }
                    if (!papersList.isNullOrEmpty()) papers = papersList
                    isLoading = false
                }
        }
    }

    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = millis
                        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        val month = calendar.get(java.util.Calendar.MONTH) + 1
                        val year = calendar.get(java.util.Calendar.YEAR)
                        dateOfBirth = "%02d-%02d-%04d".format(day, month, year)
                        val today = java.util.Calendar.getInstance()
                        var calculatedAge = today.get(java.util.Calendar.YEAR) - year
                        if (today.get(java.util.Calendar.MONTH) + 1 < month ||
                            (today.get(java.util.Calendar.MONTH) + 1 == month &&
                                    today.get(java.util.Calendar.DAY_OF_MONTH) < day)
                        ) calculatedAge--
                        age = calculatedAge.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
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
                // 👤 Personal Information
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
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = {},
                        label = { Text("Date of Birth") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = Color(0xFF1565C0)
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = if (age.isEmpty()) "" else "$age years old",
                        onValueChange = {},
                        label = { Text("Age (auto calculated)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Gray,
                            disabledBorderColor = Color.LightGray,
                            disabledLabelColor = Color.Gray
                        )
                    )
                }

                // 🏥 Professional Information
                ProfileCard(title = "🏥 Professional Information") {
                    ProfileTextField(value = specialization, onValueChange = { specialization = it }, label = "Specialization (e.g. Cardiologist)")
                    ProfileTextField(value = specializationTags, onValueChange = { specializationTags = it }, label = "Specialization Tags (e.g. Heart, Bypass)")
                    ProfileTextField(value = qualifications, onValueChange = { qualifications = it }, label = "Qualifications (e.g. MBBS, MD)")
                    ProfileTextField(value = experience, onValueChange = { experience = it }, label = "Experience (years)")
                    ProfileTextField(value = licenseNumber, onValueChange = { licenseNumber = it }, label = "License Number")
                    ProfileTextField(value = hospitalName, onValueChange = { hospitalName = it }, label = "Hospital/Clinic Name")
                    ProfileTextField(value = medicalSchool, onValueChange = { medicalSchool = it }, label = "Medical School")
                    ProfileTextField(value = graduationYear, onValueChange = { graduationYear = it }, label = "Graduation Year")
                }

                // 📚 Research & Publications
                ProfileCard(title = "📚 Research & Publications") {
                    ProfileTextField(value = numberOfPapers, onValueChange = { numberOfPapers = it }, label = "Number of Papers Published")
                    ProfileTextField(value = researchAreas, onValueChange = { researchAreas = it }, label = "Research Areas (e.g. Cardiology, Diabetes)")
                    Text("Papers", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    papers.forEachIndexed { index, paper ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Paper ${index + 1}", fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
                                    if (papers.size > 1) {
                                        IconButton(onClick = {
                                            papers = papers.toMutableList().also { it.removeAt(index) }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = paper.title,
                                    onValueChange = {
                                        papers = papers.toMutableList().also { list -> list[index] = list[index].copy(title = it) }
                                    },
                                    label = { Text("Paper Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = paper.link,
                                    onValueChange = {
                                        papers = papers.toMutableList().also { list -> list[index] = list[index].copy(link = it) }
                                    },
                                    label = { Text("Paper Link (URL)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { papers = papers + ResearchPaper() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Research Paper")
                    }
                }

                // 💰 Consultation Fees
                ProfileCard(title = "💰 Consultation Fees") {
                    ProfileTextField(value = visitFee, onValueChange = { visitFee = it }, label = "Visit Fee (BDT)")
                    ProfileTextField(value = followUpFee, onValueChange = { followUpFee = it }, label = "Follow-up Fee (BDT)")
                }

                // 🔄 Follow-up Policy
                ProfileCard(title = "🔄 Follow-up Policy") {
                    Text(
                        "Free Follow-up Period",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Patient visits for free within this many days",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    ProfileTextField(
                        value = freeFollowUpDays,
                        onValueChange = { freeFollowUpDays = it },
                        label = "Free Follow-up Days (e.g. 7)"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Half Fee Follow-up Period",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Patient pays half fee within this many days (fixed 30)",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = "30 days (fixed)",
                        onValueChange = {},
                        label = { Text("Half Fee Days") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Gray,
                            disabledBorderColor = Color.LightGray,
                            disabledLabelColor = Color.Gray
                        )
                    )
                }

                // 📅 Normal Appointment Availability
                ProfileCard(title = "📅 Normal Appointment Schedule") {
                    Text("Available Days", fontSize = 14.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysOfWeek.forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(day))
                                        selectedDays - day else selectedDays + day
                                },
                                label = { Text(day, fontSize = 11.sp) }
                            )
                        }
                    }
                    ProfileTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = "Start Time (e.g. 06:00 PM)"
                    )
                    ProfileTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = "End Time (e.g. 11:00 PM)"
                    )
                    ProfileTextField(
                        value = maxPatients,
                        onValueChange = { maxPatients = it },
                        label = "Max Patients Per Day"
                    )
                }

                // 🔬 Report Review Schedule
                ProfileCard(title = "🔬 Report Review Schedule") {
                    Text(
                        "Separate time slot for patients submitting test reports",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Available Days for Report Review", fontSize = 14.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysOfWeek.forEach { day ->
                            FilterChip(
                                selected = reportReviewDays.contains(day),
                                onClick = {
                                    reportReviewDays = if (reportReviewDays.contains(day))
                                        reportReviewDays - day else reportReviewDays + day
                                },
                                label = { Text(day, fontSize = 11.sp) }
                            )
                        }
                    }
                    ProfileTextField(
                        value = reportReviewStartTime,
                        onValueChange = { reportReviewStartTime = it },
                        label = "Report Review Start Time (e.g. 05:00 PM)"
                    )
                    ProfileTextField(
                        value = reportReviewEndTime,
                        onValueChange = { reportReviewEndTime = it },
                        label = "Report Review End Time (e.g. 06:00 PM)"
                    )
                    ProfileTextField(
                        value = maxReportPatients,
                        onValueChange = { maxReportPatients = it },
                        label = "Max Report Patients Per Day"
                    )
                }

                // ⚙️ Services
                ProfileCard(title = "⚙️ Services") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Telemedicine Available", fontSize = 15.sp)
                        Switch(checked = telemedicine, onCheckedChange = { telemedicine = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Emergency Available", fontSize = 15.sp)
                        Switch(checked = emergencyAvailable, onCheckedChange = { emergencyAvailable = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Online Prescription", fontSize = 15.sp)
                        Switch(checked = onlinePrescription, onCheckedChange = { onlinePrescription = it })
                    }
                }

                // 📍 Address
                ProfileCard(title = "📍 Address") {
                    ProfileTextField(value = city, onValueChange = { city = it }, label = "City")
                    ProfileTextField(value = chamberAddress, onValueChange = { chamberAddress = it }, label = "Chamber Address")
                }

                // 📝 About
                ProfileCard(title = "📝 About") {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Short Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        val papersList = papers.map { mapOf("title" to it.title, "link" to it.link) }
                        val updates = hashMapOf<String, Any>(
                            "name" to name,
                            "phone" to phone,
                            "gender" to gender,
                            "dateOfBirth" to dateOfBirth,
                            "age" to age,
                            "specialization" to specialization,
                            "specializationTags" to specializationTags,
                            "qualifications" to qualifications,
                            "experience" to experience,
                            "licenseNumber" to licenseNumber,
                            "hospitalName" to hospitalName,
                            "medicalSchool" to medicalSchool,
                            "graduationYear" to graduationYear,
                            "numberOfPapers" to numberOfPapers,
                            "researchAreas" to researchAreas,
                            "papers" to papersList,
                            "visitFee" to visitFee,
                            "followUpFee" to followUpFee,
                            "freeFollowUpDays" to freeFollowUpDays,
                            "halfFeeFollowUpDays" to halfFeeFollowUpDays,
                            "availableDays" to selectedDays.toList(),
                            "startTime" to startTime,
                            "endTime" to endTime,
                            "maxPatients" to maxPatients,
                            "reportReviewDays" to reportReviewDays.toList(),
                            "reportReviewStartTime" to reportReviewStartTime,
                            "reportReviewEndTime" to reportReviewEndTime,
                            "maxReportPatients" to maxReportPatients,
                            "telemedicine" to telemedicine,
                            "emergencyAvailable" to emergencyAvailable,
                            "onlinePrescription" to onlinePrescription,
                            "city" to city,
                            "chamberAddress" to chamberAddress,
                            "bio" to bio
                        )
                        if (userId != null) {
                            db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener {
                                    isSaving = false
                                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
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