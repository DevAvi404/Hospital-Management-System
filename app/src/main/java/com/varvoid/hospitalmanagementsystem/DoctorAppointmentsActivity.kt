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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DoctorAppointmentsActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                DoctorAppointmentsScreen(
                    auth = auth,
                    db = db,
                    context = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

// ✅ FIX: Dedicated data class for doctor's appointment view
// Previously the code was misusing AppointmentItem fields:
//   - patientName was stored in doctorName field
//   - patientId was stored in specialization field
// This dedicated class makes field intent crystal clear.
data class DoctorAppointmentEntry(
    val id: String = "",
    val patientId: String = "",        // ✅ correct field
    val patientName: String = "",      // ✅ correct field
    val date: String = "",
    val serialNumber: Int = 0,
    val appointmentType: String = "new",
    val fee: String = "0",
    val status: String = "pending",
    val reason: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentsScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var appointments by remember { mutableStateOf(listOf<DoctorAppointmentEntry>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Today", "Upcoming", "Past")
    val doctorId = auth.currentUser?.uid ?: ""

    val today = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { documents ->
                appointments = documents.map { doc ->
                    DoctorAppointmentEntry(
                        id = doc.id,
                        patientId = doc.getString("patientId") ?: "",      // ✅ FIXED
                        patientName = doc.getString("patientName") ?: "",  // ✅ FIXED
                        date = doc.getString("date") ?: "",
                        serialNumber = (doc.getLong("serialNumber") ?: 0).toInt(),
                        appointmentType = doc.getString("appointmentType") ?: "new",
                        fee = doc.getString("fee") ?: "0",
                        status = doc.getString("status") ?: "pending",
                        reason = doc.getString("reason") ?: ""
                    )
                }.sortedBy { it.serialNumber }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    val filteredAppointments = when (selectedTab) {
        0 -> appointments.filter { it.date == today }
        1 -> appointments.filter { it.date > today && it.status != "cancelled" }
        2 -> appointments.filter { it.date < today || it.status == "completed" }
        else -> appointments
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Appointments", fontWeight = FontWeight.Bold) },
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1565C0)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1565C0))
                }
            } else if (filteredAppointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "No appointments", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No ${tabs[selectedTab]} appointments", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("${filteredAppointments.size} appointment(s)", fontSize = 14.sp, color = Color.Gray)
                    }
                    items(filteredAppointments) { appointment ->
                        DoctorAppointmentCard(
                            appointment = appointment,
                            onViewPatient = {
                                val intent = Intent(context, DoctorPatientDetailsActivity::class.java)
                                intent.putExtra("patientId", appointment.patientId)   // ✅ FIXED
                                intent.putExtra("doctorId", doctorId)
                                context.startActivity(intent)
                            },
                            onWritePrescription = {
                                val intent = Intent(context, WritePrescriptionActivity::class.java)
                                intent.putExtra("patientId", appointment.patientId)   // ✅ FIXED
                                intent.putExtra("patientName", appointment.patientName) // ✅ FIXED
                                intent.putExtra("appointmentId", appointment.id)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorAppointmentCard(
    appointment: DoctorAppointmentEntry,
    onViewPatient: () -> Unit,
    onWritePrescription: () -> Unit
) {
    val statusColor = when (appointment.status) {
        "confirmed" -> Color(0xFF4CAF50)
        "pending" -> Color(0xFFF57F17)
        "completed" -> Color(0xFF1565C0)
        "cancelled" -> Color.Red
        else -> Color.Gray
    }

    val typeLabel = when (appointment.appointmentType) {
        "followup" -> "Follow-up"
        "report" -> "Report Review"
        "walkin" -> "Walk-in"
        else -> "New Visit"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = "Patient", tint = Color(0xFF1565C0), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        // ✅ FIXED: shows patientName correctly now
                        Text(text = appointment.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Serial: #${appointment.serialNumber}", fontSize = 13.sp, color = Color(0xFF1565C0))
                    }
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor)
                ) {
                    Text(
                        text = appointment.status.replaceFirstChar { it.uppercase() },
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "📅 ${appointment.date}", fontSize = 13.sp, color = Color.DarkGray)
                    Text(text = "🏥 $typeLabel", fontSize = 13.sp, color = Color.DarkGray)
                }
                Text(
                    text = if (appointment.fee == "0") "FREE" else "BDT ${appointment.fee}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (appointment.fee == "0") Color(0xFF4CAF50) else Color.DarkGray
                )
            }

            if (appointment.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Text(text = "Reason: ${appointment.reason}", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewPatient,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Filled.Person, contentDescription = "View", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Patient Details", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onWritePrescription,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Prescription", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Prescription", color = Color(0xFF4CAF50))
            }
        }
    }
}