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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// ✅ FIX 1: Missing data class that was causing almost all "Unresolved reference" errors
data class QueueAppointment(
    val id: String = "",
    val patientName: String = "",
    val patientId: String = "",
    val serialNumber: Int = 0,
    val appointmentType: String = "new",
    val reason: String = "",
    val fee: String = "0",
    val status: String = "pending"
)


class TodayQueueActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                TodayQueueScreen(
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
fun TodayQueueScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var appointments by remember { mutableStateOf(listOf<QueueAppointment>()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentSerial by remember { mutableStateOf(0) }
    var assignedDoctorId by remember { mutableStateOf("") }
    var showAddWalkIn by remember { mutableStateOf(false) }
    var walkInName by remember { mutableStateOf("") }
    var walkInReason by remember { mutableStateOf("") }

    val today = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    fun loadQueue() {
        if (assignedDoctorId.isEmpty()) return
        db.collection("appointments")
            .whereEqualTo("doctorId", assignedDoctorId)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                appointments = documents.map { doc ->
                    QueueAppointment(
                        id = doc.id,
                        patientName = doc.getString("patientName") ?: "",
                        patientId = doc.getString("patientId") ?: "",
                        serialNumber = (doc.getLong("serialNumber") ?: 0).toInt(),
                        appointmentType = doc.getString("appointmentType") ?: "new",
                        reason = doc.getString("reason") ?: "",
                        fee = doc.getString("fee") ?: "0",
                        status = doc.getString("status") ?: "pending"
                    )
                }.sortedBy { it.serialNumber }
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                assignedDoctorId = doc.getString("assignedDoctorId") ?: ""
                db.collection("queue").document(assignedDoctorId)
                    .get()
                    .addOnSuccessListener { queueDoc ->
                        // ✅ Only use today's serial
                        val queueDate = queueDoc.getString("date") ?: ""
                        currentSerial = if (queueDate == today) {
                            (queueDoc.getLong("currentSerial") ?: 0).toInt()
                        } else {
                            0 // Reset if it's a new day
                        }
                        loadQueue()
                    }
            }
    }

    // Walk-in Dialog
    if (showAddWalkIn) {
        AlertDialog(
            onDismissRequest = { showAddWalkIn = false },
            title = { Text("Add Walk-in Patient", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = walkInName,
                        onValueChange = { walkInName = it },
                        label = { Text("Patient Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = walkInReason,
                        onValueChange = { walkInReason = it },
                        label = { Text("Reason for Visit") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (walkInName.isEmpty()) {
                            Toast.makeText(context, "Enter patient name!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val serialNumber = appointments.size + 1
                        val walkIn = hashMapOf(
                            "patientName" to walkInName,
                            "patientId" to "walkin",
                            "doctorId" to assignedDoctorId,
                            "doctorName" to "",
                            "date" to today,
                            "reason" to walkInReason,
                            "appointmentType" to "walkin",
                            "fee" to "0",
                            "status" to "pending",
                            "serialNumber" to serialNumber,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("appointments")
                            .add(walkIn)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Walk-in added! Serial: #$serialNumber", Toast.LENGTH_SHORT).show()
                                walkInName = ""
                                walkInReason = ""
                                showAddWalkIn = false
                                loadQueue()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddWalkIn = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Queue", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showAddWalkIn = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Walk-in", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Serial Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Now Seeing", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        text = if (currentSerial == 0) "Not Started" else "Serial #$currentSerial",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Show current patient name
                    val currentPatient = appointments.find { it.serialNumber == currentSerial }
                    if (currentPatient != null) {
                        Text(
                            text = currentPatient.patientName,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ Fixed: Only go to next if there's a next patient
                    val nextAppointment = appointments.find {
                        it.serialNumber == currentSerial + 1 &&
                                it.status != "cancelled" &&
                                it.status != "completed"
                    }

                    Button(
                        onClick = {
                            if (nextAppointment == null && currentSerial >= appointments.size) {
                                Toast.makeText(context, "No more patients in queue!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val nextSerial = currentSerial + 1
                            val nextPatient = appointments.find { it.serialNumber == nextSerial }

                            if (nextPatient == null) {
                                Toast.makeText(context, "No patient found for serial #$nextSerial", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Mark current as completed
                            val currentAppointment = appointments.find { it.serialNumber == currentSerial }
                            currentAppointment?.let { appt ->
                                if (appt.status == "pending" || appt.status == "confirmed") {
                                    db.collection("appointments").document(appt.id)
                                        .update("status", "completed")
                                }
                            }

                            // Update queue
                            db.collection("queue")
                                .document(assignedDoctorId)
                                .set(mapOf(
                                    "currentSerial" to nextSerial,
                                    "doctorId" to assignedDoctorId,
                                    "date" to today
                                ))
                                .addOnSuccessListener {
                                    currentSerial = nextSerial

                                    // Notify patient 3 serials away
                                    val notifySerial = nextSerial + 3
                                    val notifyAppointment = appointments.find { it.serialNumber == notifySerial }
                                    if (notifyAppointment != null && notifyAppointment.patientId != "walkin") {
                                        db.collection("notifications").add(mapOf(
                                            "userId" to notifyAppointment.patientId,
                                            "title" to "Your turn is coming soon!",
                                            "message" to "You are 3 serials away. Please head to the clinic. Your serial: #${notifyAppointment.serialNumber}",
                                            "doctorId" to assignedDoctorId,
                                            "read" to false,
                                            "createdAt" to System.currentTimeMillis()
                                        ))
                                    }
                                    loadQueue()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (currentSerial == 0) "Start Queue → Serial #1"
                            else "Next Patient → Serial #${currentSerial + 1}",
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(appointments.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Text("Total", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(appointments.count { it.status == "completed" }.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text("Done", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(appointments.count { it.status != "completed" && it.status != "cancelled" }.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                        Text("Waiting", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Queue List
            Text("Today's Queue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1565C0))
                }
            } else if (appointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No appointments today", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(appointments) { appointment ->
                        QueueCard(
                            appointment = appointment,
                            currentSerial = currentSerial,
                            onDone = {
                                db.collection("appointments")
                                    .document(appointment.id)
                                    .update("status", "completed")
                                    .addOnSuccessListener {
                                        currentSerial = appointment.serialNumber

                                        // Update queue document
                                        db.collection("queue")
                                            .document(assignedDoctorId)
                                            .set(mapOf(
                                                "currentSerial" to appointment.serialNumber,
                                                "doctorId" to assignedDoctorId,
                                                "date" to today
                                            ))
                                            .addOnSuccessListener {
                                                // ✅ Notify patient who is 3 serials away
                                                val notifySerial = appointment.serialNumber + 3
                                                val notifyAppt = appointments.find {
                                                    it.serialNumber == notifySerial &&
                                                            it.status != "cancelled" &&
                                                            it.patientId != "walkin"
                                                }

                                                if (notifyAppt != null) {
                                                    // Check if notification already sent
                                                    db.collection("notifications")
                                                        .whereEqualTo("userId", notifyAppt.patientId)
                                                        .whereEqualTo("doctorId", assignedDoctorId)
                                                        .whereEqualTo("serialNumber", notifySerial)
                                                        .get()
                                                        .addOnSuccessListener { existing ->
                                                            if (existing.isEmpty) {
                                                                // Send notification only once
                                                                db.collection("notifications").add(mapOf(
                                                                    "userId" to notifyAppt.patientId,
                                                                    "title" to "🔔 Your turn is coming soon!",
                                                                    "message" to "Serial #${notifyAppt.serialNumber} — Please head to the clinic now. Current serial: #${appointment.serialNumber}",
                                                                    "doctorId" to assignedDoctorId,
                                                                    "serialNumber" to notifySerial,
                                                                    "read" to false,
                                                                    "createdAt" to System.currentTimeMillis()
                                                                ))
                                                            }
                                                        }
                                                }

                                                Toast.makeText(context, "✅ Visit completed!", Toast.LENGTH_SHORT).show()
                                                loadQueue()
                                            }
                                    }
                            },
                            onCancel = {
                                db.collection("appointments")
                                    .document(appointment.id)
                                    .update("status", "cancelled")
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Appointment cancelled!", Toast.LENGTH_SHORT).show()
                                        loadQueue()
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueCard(
    appointment: QueueAppointment,
    currentSerial: Int,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val isCurrentlyServing = appointment.serialNumber == currentSerial
    val isCompleted = appointment.status == "completed"
    val isCancelled = appointment.status == "cancelled"

    val cardColor = when {
        isCompleted -> Color(0xFFE8F5E9)
        isCancelled -> Color(0xFFFFEBEE)
        isCurrentlyServing -> Color(0xFFE3F2FD)
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Serial Number Badge
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> Color(0xFF4CAF50)
                            isCancelled -> Color.Red
                            isCurrentlyServing -> Color(0xFF1565C0)
                            else -> Color.Gray
                        }
                    )
                ) {
                    Text(
                        text = "#${appointment.serialNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(appointment.patientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = when (appointment.appointmentType) {
                            "followup" -> "Follow-up"
                            "report" -> "Report Review"
                            "walkin" -> "Walk-in"
                            else -> "New Visit"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (isCurrentlyServing) {
                        Text("🔵 Currently Serving", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    }
                }

                // Fee
                Text(
                    text = if (appointment.fee == "0") "FREE" else "BDT ${appointment.fee}",
                    fontSize = 12.sp,
                    color = if (appointment.fee == "0") Color(0xFF4CAF50) else Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            if (!isCompleted && !isCancelled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Done Button
                    Button(
                        onClick = onDone,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", color = Color.White, fontSize = 13.sp)
                    }
                    // Cancel Button
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Cancel", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", color = Color.Red, fontSize = 13.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCompleted) "✅ Visit Completed" else "❌ Cancelled",
                    fontSize = 12.sp,
                    color = if (isCompleted) Color(0xFF4CAF50) else Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}