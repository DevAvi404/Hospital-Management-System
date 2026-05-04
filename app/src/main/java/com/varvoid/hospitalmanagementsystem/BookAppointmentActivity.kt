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
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val doctorId = intent.getStringExtra("doctorId") ?: ""

        setContent {
            MaterialTheme {
                BookAppointmentScreen(
                    db = db,
                    auth = auth,
                    doctorId = doctorId,
                    context = this,
                    onBack = { finish() },
                    onBookingSuccess = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    doctorId: String,
    context: android.content.Context,
    onBack: () -> Unit,
    onBookingSuccess: () -> Unit
) {
    // Doctor info
    var doctorName by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var visitFee by remember { mutableStateOf("") }
    var followUpFee by remember { mutableStateOf("") }
    var availableDays by remember { mutableStateOf(listOf<String>()) }
    var reportReviewDays by remember { mutableStateOf(listOf<String>()) }
    var reportReviewStartTime by remember { mutableStateOf("") }
    var reportReviewEndTime by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var maxPatients by remember { mutableStateOf("20") }
    var maxReportPatients by remember { mutableStateOf("10") }
    var freeFollowUpDays by remember { mutableStateOf("7") }

    // Booking state
    var selectedDate by remember { mutableStateOf("") }
    var appointmentType by remember { mutableStateOf("new") } // new, followup, report
    var reason by remember { mutableStateOf("") }
    var calculatedFee by remember { mutableStateOf("") }
    var feeNote by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isBooking by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var lastVisitDate by remember { mutableStateOf<Long?>(null) }
    var remainingSlots by remember { mutableStateOf(0) }

    val patientId = auth.currentUser?.uid ?: ""

    // Load doctor info
    LaunchedEffect(Unit) {
        db.collection("users").document(doctorId)
            .get()
            .addOnSuccessListener { document ->
                doctorName = document.getString("name") ?: ""
                specialization = document.getString("specialization") ?: ""
                hospitalName = document.getString("hospitalName") ?: ""
                visitFee = document.getString("visitFee") ?: "0"
                followUpFee = document.getString("followUpFee") ?: "0"
                startTime = document.getString("startTime") ?: ""
                endTime = document.getString("endTime") ?: ""
                maxPatients = document.getString("maxPatients") ?: "20"
                maxReportPatients = document.getString("maxReportPatients") ?: "10"
                freeFollowUpDays = document.getString("freeFollowUpDays") ?: "7"
                reportReviewStartTime = document.getString("reportReviewStartTime") ?: ""
                reportReviewEndTime = document.getString("reportReviewEndTime") ?: ""
                availableDays = (document.get("availableDays") as? List<*>)
                    ?.filterIsInstance<String>() ?: listOf()
                reportReviewDays = (document.get("reportReviewDays") as? List<*>)
                    ?.filterIsInstance<String>() ?: listOf()
                calculatedFee = visitFee
                isLoading = false
            }

        // Load last visit date
        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastVisit = documents.maxByOrNull {
                        it.getLong("createdAt") ?: 0
                    }
                    lastVisitDate = lastVisit?.getLong("createdAt")
                }
            }
    }

    // Calculate fee based on appointment type and last visit
    fun calculateFee() {
        val fullFee = visitFee.toDoubleOrNull() ?: 0.0
        val halfFee = (fullFee / 2).toInt()
        val freeDays = freeFollowUpDays.toIntOrNull() ?: 7

        when (appointmentType) {
            "new" -> {
                calculatedFee = visitFee
                feeNote = "Full visit fee"
            }
            "followup", "report" -> {
                val lastVisit = lastVisitDate
                if (lastVisit == null) {
                    calculatedFee = visitFee
                    feeNote = "No previous visit found — full fee applies"
                } else {
                    val daysPassed = ((System.currentTimeMillis() - lastVisit) / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        daysPassed <= freeDays -> {
                            calculatedFee = "0"
                            feeNote = "✅ Free! Within $freeDays day follow-up period ($daysPassed days since last visit)"
                        }
                        daysPassed <= 30 -> {
                            calculatedFee = halfFee.toString()
                            feeNote = "Half fee — between $freeDays-30 days ($daysPassed days since last visit)"
                        }
                        else -> {
                            calculatedFee = visitFee
                            feeNote = "Full fee — more than 30 days since last visit ($daysPassed days)"
                        }
                    }
                }
            }
        }
    }

    // Load remaining slots when date selected
    fun loadRemainingSlots(date: String) {
        val collection = if (appointmentType == "report") "reportAppointments" else "appointments"
        val maxSlots = if (appointmentType == "report")
            maxReportPatients.toIntOrNull() ?: 10
        else
            maxPatients.toIntOrNull() ?: 20

        db.collection(collection)
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                remainingSlots = maxSlots - documents.size()
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
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = millis
                        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.MONDAY -> "Mon"
                            Calendar.TUESDAY -> "Tue"
                            Calendar.WEDNESDAY -> "Wed"
                            Calendar.THURSDAY -> "Thu"
                            Calendar.FRIDAY -> "Fri"
                            Calendar.SATURDAY -> "Sat"
                            Calendar.SUNDAY -> "Sun"
                            else -> ""
                        }
                        val validDays = if (appointmentType == "report") reportReviewDays else availableDays
                        if (!validDays.contains(dayOfWeek)) {
                            Toast.makeText(
                                context,
                                "Doctor is not available on this day!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                            selectedDate = sdf.format(calendar.time)
                            loadRemainingSlots(selectedDate)
                        }
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
                title = { Text("Book Appointment", fontWeight = FontWeight.Bold) },
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
                // Doctor Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Doctor",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dr. $doctorName",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = specialization,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = hospitalName,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Visit Fee: BDT $visitFee",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Follow-up: BDT $followUpFee",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Normal: ${availableDays.joinToString(", ")} | $startTime - $endTime",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        if (reportReviewDays.isNotEmpty()) {
                            Text(
                                text = "Report Review: ${reportReviewDays.joinToString(", ")} | $reportReviewStartTime - $reportReviewEndTime",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Appointment Type Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏥 Select Appointment Type",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // New Visit
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (appointmentType == "new")
                                    Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                            ),
                            onClick = {
                                appointmentType = "new"
                                selectedDate = ""
                                calculateFee()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appointmentType == "new",
                                    onClick = {
                                        appointmentType = "new"
                                        selectedDate = ""
                                        calculateFee()
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "New Visit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "First time or new problem",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    "BDT $visitFee",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Follow-up Visit
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (appointmentType == "followup")
                                    Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                            ),
                            onClick = {
                                appointmentType = "followup"
                                selectedDate = ""
                                calculateFee()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appointmentType == "followup",
                                    onClick = {
                                        appointmentType = "followup"
                                        selectedDate = ""
                                        calculateFee()
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Follow-up Visit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "Same problem, returning patient",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    if (calculatedFee == "0" && appointmentType == "followup") "FREE"
                                    else if (appointmentType == "followup") "BDT $calculatedFee"
                                    else "BDT $followUpFee",
                                    fontWeight = FontWeight.Bold,
                                    color = if (calculatedFee == "0" && appointmentType == "followup")
                                        Color(0xFF4CAF50) else Color(0xFF1565C0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Report Review
                        if (reportReviewDays.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (appointmentType == "report")
                                        Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                                ),
                                onClick = {
                                    appointmentType = "report"
                                    selectedDate = ""
                                    calculateFee()
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = appointmentType == "report",
                                        onClick = {
                                            appointmentType = "report"
                                            selectedDate = ""
                                            calculateFee()
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Report Review",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "Submit test reports ($reportReviewStartTime - $reportReviewEndTime)",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        if (calculatedFee == "0" && appointmentType == "report") "FREE"
                                        else if (appointmentType == "report") "BDT $calculatedFee"
                                        else "Auto",
                                        fontWeight = FontWeight.Bold,
                                        color = if (calculatedFee == "0" && appointmentType == "report")
                                            Color(0xFF4CAF50) else Color(0xFF1565C0)
                                    )
                                }
                            }
                        }

                        // Fee Note
                        if (feeNote.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F4FF)
                                )
                            ) {
                                Text(
                                    text = feeNote,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1565C0),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Select Date Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📅 Select Date",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = selectedDate,
                            onValueChange = {},
                            label = { Text("Appointment Date") },
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
                        if (selectedDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (remainingSlots > 5)
                                        Color(0xFF4CAF50) else Color(0xFFF57F17)
                                )
                            ) {
                                Text(
                                    text = "$remainingSlots slots remaining",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Reason Card
                if (selectedDate.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📝 Reason for Visit",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text("Describe your symptoms or reason") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 5
                            )
                        }
                    }

                    // Booking Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📋 Booking Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailRow(label = "Doctor", value = "Dr. $doctorName")
                            DetailRow(label = "Type", value = when(appointmentType) {
                                "followup" -> "Follow-up Visit"
                                "report" -> "Report Review"
                                else -> "New Visit"
                            })
                            DetailRow(label = "Date", value = selectedDate)
                            DetailRow(
                                label = "Fee",
                                value = if (calculatedFee == "0") "FREE" else "BDT $calculatedFee"
                            )
                            DetailRow(label = "Remaining Slots", value = "$remainingSlots")
                        }
                    }

                    // Confirm Button
                    Button(
                        onClick = {
                            if (reason.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please enter reason for visit",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            if (remainingSlots <= 0) {
                                Toast.makeText(
                                    context,
                                    "No slots available for this date!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            isBooking = true
                            val collection = if (appointmentType == "report")
                                "reportAppointments" else "appointments"

                            db.collection("users").document(patientId)
                                .get()
                                .addOnSuccessListener { patientDoc ->
                                    val patientName = patientDoc.getString("name") ?: ""

                                    db.collection(collection)
                                        .whereEqualTo("doctorId", doctorId)
                                        .whereEqualTo("date", selectedDate)
                                        .get()
                                        .addOnSuccessListener { appointments ->
                                            val serialNumber = appointments.size() + 1

                                            val appointment = hashMapOf(
                                                "patientId" to patientId,
                                                "patientName" to patientName,
                                                "doctorId" to doctorId,
                                                "doctorName" to doctorName,
                                                "date" to selectedDate,
                                                "reason" to reason,
                                                "appointmentType" to appointmentType,
                                                "fee" to calculatedFee,
                                                "status" to "pending",
                                                "serialNumber" to serialNumber,
                                                "createdAt" to System.currentTimeMillis()
                                            )

                                            db.collection(collection)
                                                .add(appointment)
                                                .addOnSuccessListener {
                                                    // ✅ Add Notification for Doctor
                                                    db.collection("notifications").add(mapOf(
                                                        "userId" to doctorId,
                                                        "title" to "📅 New Appointment Booked",
                                                        "message" to "Patient $patientName has booked a ${appointmentType} appointment for $selectedDate (Serial: #$serialNumber)",
                                                        "read" to false,
                                                        "createdAt" to System.currentTimeMillis()
                                                    ))

                                                    isBooking = false
                                                    Toast.makeText(
                                                        context,
                                                        "Appointment booked! Your serial: #$serialNumber",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    onBookingSuccess()
                                                }
                                                .addOnFailureListener {
                                                    isBooking = false
                                                    Toast.makeText(
                                                        context,
                                                        "Booking failed!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        enabled = !isBooking
                    ) {
                        if (isBooking) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Confirm",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Confirm Booking",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}