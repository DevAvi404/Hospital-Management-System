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
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

data class AppointmentItem(
    val id: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val specialization: String = "",
    val date: String = "",
    val serialNumber: Int = 0,
    val appointmentType: String = "new",
    val fee: String = "",
    val status: String = "pending",
    val reason: String = "",
    val currentSerial: Int = 0
)

class MyAppointmentsActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val listeners = mutableListOf<ListenerRegistration>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                MyAppointmentsScreen(
                    auth = auth,
                    db = db,
                    context = this,
                    onBack = { finish() },
                    onRegisterListener = { listeners.add(it) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit,
    onRegisterListener: (ListenerRegistration) -> Unit
) {
    var appointments by remember { mutableStateOf(listOf<AppointmentItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var liveSerials by remember { mutableStateOf(mapOf<String, Int>()) }
    val tabs = listOf("Upcoming", "Past", "Cancelled")
    val patientId = auth.currentUser?.uid ?: ""

    val today = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    fun loadAppointments() {
        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .get()
            .addOnSuccessListener { documents ->
                val appointmentList = mutableListOf<AppointmentItem>()
                var pendingCount = documents.size()

                if (pendingCount == 0) {
                    appointments = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    val doctorId = doc.getString("doctorId") ?: ""

                    // ✅ Load doctor's actual name from users collection
                    db.collection("users").document(doctorId)
                        .get()
                        .addOnSuccessListener { doctorDoc ->
                            val doctorName = doctorDoc.getString("name") ?: ""
                            val specialization = doctorDoc.getString("specialization") ?: ""

                            appointmentList.add(
                                AppointmentItem(
                                    id = doc.id,
                                    doctorId = doctorId,
                                    doctorName = doctorName, // ✅ Real doctor name
                                    specialization = specialization,
                                    date = doc.getString("date") ?: "",
                                    serialNumber = (doc.getLong("serialNumber") ?: 0).toInt(),
                                    appointmentType = doc.getString("appointmentType") ?: "new",
                                    fee = doc.getString("fee") ?: "0",
                                    status = doc.getString("status") ?: "pending",
                                    reason = doc.getString("reason") ?: ""
                                )
                            )
                            pendingCount--
                            if (pendingCount == 0) {
                                appointments = appointmentList.sortedByDescending { it.date }
                                isLoading = false

                                // ✅ Set up real-time queue listeners
                                val uniqueDoctorIds = appointmentList.map { it.doctorId }.distinct()
                                uniqueDoctorIds.forEach { dId ->
                                    val listener = db.collection("queue").document(dId)
                                        .addSnapshotListener { snapshot, _ ->
                                            if (snapshot != null && snapshot.exists()) {
                                                val queueDate = snapshot.getString("date") ?: ""
                                                val serial = if (queueDate == today) {
                                                    (snapshot.getLong("currentSerial") ?: 0).toInt()
                                                } else 0
                                                liveSerials = liveSerials.toMutableMap().also { it[dId] = serial }
                                            }
                                        }
                                    onRegisterListener(listener)
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) {
        loadAppointments()
    }

    val filteredAppointments = when (selectedTab) {
        0 -> appointments.filter { it.status == "pending" || it.status == "confirmed" }
        1 -> appointments.filter { it.status == "completed" }
        2 -> appointments.filter { it.status == "cancelled" }
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAppointments) { appointment ->
                        val currentSerial = liveSerials[appointment.doctorId] ?: 0
                        AppointmentCard(
                            appointment = appointment.copy(currentSerial = currentSerial),
                            isToday = appointment.date == today,
                            onCancel = {
                                db.collection("appointments")
                                    .document(appointment.id)
                                    .update("status", "cancelled")
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Appointment cancelled!", Toast.LENGTH_SHORT).show()
                                        loadAppointments()
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
fun AppointmentCard(
    appointment: AppointmentItem,
    isToday: Boolean,
    onCancel: () -> Unit
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
        else -> "New Visit"
    }

    val serialsAway = appointment.serialNumber - appointment.currentSerial
    val queueStatus = when {
        appointment.currentSerial == 0 -> "⏳ Queue not started yet"
        serialsAway <= 0 -> "✅ Your turn has passed"
        serialsAway == 1 -> "🔴 You're next!"
        serialsAway <= 3 -> "🟠 Almost your turn! ($serialsAway ahead)"
        else -> "🟢 $serialsAway patients ahead of you"
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
                Column {
                    Text(
                        text = "Dr. ${appointment.doctorName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = appointment.specialization,
                        fontSize = 13.sp,
                        color = Color(0xFF1565C0)
                    )
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "📅 ${appointment.date}", fontSize = 13.sp, color = Color.DarkGray)
                    Text(text = "🏥 $typeLabel", fontSize = 13.sp, color = Color.DarkGray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Serial: #${appointment.serialNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                    Text(
                        text = if (appointment.fee == "0") "Fee: FREE" else "Fee: BDT ${appointment.fee}",
                        fontSize = 13.sp,
                        color = if (appointment.fee == "0") Color(0xFF4CAF50) else Color.DarkGray
                    )
                }
            }

            // Live Queue — only for today
            if (isToday && (appointment.status == "pending" || appointment.status == "confirmed")) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            serialsAway <= 1 -> Color(0xFFFFEBEE)
                            serialsAway <= 3 -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.RadioButtonChecked,
                                    contentDescription = "Live",
                                    tint = Color.Red,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE Queue", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Current: #${appointment.currentSerial} | Yours: #${appointment.serialNumber}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = queueStatus, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Cancel", tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Appointment", color = Color.Red)
                }
            }
        }
    }
}