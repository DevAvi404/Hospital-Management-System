package com.varvoid.hospitalmanagementsystem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

data class AllAppointmentItem(
    val id: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val date: String = "",
    val serialNumber: Int = 0,
    val appointmentType: String = "new",
    val fee: String = "0",
    val status: String = "pending",
    val reason: String = ""
)

class AllAppointmentsActivity : ComponentActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        setContent {
            MaterialTheme {
                AllAppointmentsScreen(db = db, context = this, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppointmentsScreen(
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var appointments by remember { mutableStateOf(listOf<AllAppointmentItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedTypeFilter by remember { mutableStateOf("All") }

    val statusFilters = listOf("All", "Pending", "Completed", "Cancelled")
    val typeFilters = listOf("All", "New Visit", "Follow-up", "Report", "Walk-in")

    LaunchedEffect(Unit) {
        db.collection("appointments").get()
            .addOnSuccessListener { documents ->
                appointments = documents.map { doc ->
                    AllAppointmentItem(
                        id = doc.id,
                        patientName = doc.getString("patientName") ?: "Unknown",
                        doctorName = doc.getString("doctorName") ?: "Unknown",
                        date = doc.getString("date") ?: "",
                        serialNumber = (doc.getLong("serialNumber") ?: 0).toInt(),
                        appointmentType = doc.getString("appointmentType") ?: "new",
                        fee = doc.getString("fee") ?: "0",
                        status = doc.getString("status") ?: "pending",
                        reason = doc.getString("reason") ?: ""
                    )
                }.sortedByDescending { it.date }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    val filtered = appointments.filter { appt ->
        val matchSearch = appt.patientName.contains(searchQuery, ignoreCase = true) ||
                appt.doctorName.contains(searchQuery, ignoreCase = true) ||
                appt.date.contains(searchQuery)
        val matchStatus = when (selectedStatusFilter) {
            "Pending" -> appt.status == "pending"
            "Completed" -> appt.status == "completed"
            "Cancelled" -> appt.status == "cancelled"
            else -> true
        }
        val matchType = when (selectedTypeFilter) {
            "New Visit" -> appt.appointmentType == "new"
            "Follow-up" -> appt.appointmentType == "followup"
            "Report" -> appt.appointmentType == "report"
            "Walk-in" -> appt.appointmentType == "walkin"
            else -> true
        }
        matchSearch && matchStatus && matchType
    }

    val totalCount = appointments.size
    val pendingCount = appointments.count { it.status == "pending" }
    val completedCount = appointments.count { it.status == "completed" }
    val cancelledCount = appointments.count { it.status == "cancelled" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Appointments", fontWeight = FontWeight.Bold) },
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
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else {
            // ✅ Everything inside ONE LazyColumn
            // Stats + Search + Filters scroll away, list gets full space
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

                // ── Stats Header ──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1565C0), Color(0xFF1976D2))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatPill("Total", totalCount.toString(), Color.White, Color.White.copy(alpha = 0.2f), Modifier.weight(1f))
                            StatPill("Pending", pendingCount.toString(), Color(0xFFFFD54F), Color(0xFFFFD54F).copy(alpha = 0.2f), Modifier.weight(1f))
                            StatPill("Done", completedCount.toString(), Color(0xFF81C784), Color(0xFF81C784).copy(alpha = 0.2f), Modifier.weight(1f))
                            StatPill("Cancelled", cancelledCount.toString(), Color(0xFFEF9A9A), Color(0xFFEF9A9A).copy(alpha = 0.2f), Modifier.weight(1f))
                        }
                    }
                }

                // ── Search Bar ──
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search patient, doctor or date...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF1565C0))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        )
                    )
                }

                // ── Status Filter ──
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Status", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            statusFilters.forEach { filter ->
                                val isSelected = selectedStatusFilter == filter
                                val chipColor = when (filter) {
                                    "Pending" -> Color(0xFFF57F17)
                                    "Completed" -> Color(0xFF4CAF50)
                                    "Cancelled" -> Color.Red
                                    else -> Color(0xFF1565C0)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) chipColor else Color.Transparent)
                                        .border(1.5.dp, if (isSelected) chipColor else Color(0xFFDDDDDD), RoundedCornerShape(20.dp))
                                        .clickable { selectedStatusFilter = filter }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        filter,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Type Filter ──
                item {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Type", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            typeFilters.forEach { filter ->
                                val isSelected = selectedTypeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0xFF1565C0) else Color.Transparent)
                                        .border(1.5.dp, if (isSelected) Color(0xFF1565C0) else Color(0xFFDDDDDD), RoundedCornerShape(20.dp))
                                        .clickable { selectedTypeFilter = filter }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        filter,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Result Count ──
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1565C0))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${filtered.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text("appointment(s) found", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // ── Divider ──
                item {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Empty State ──
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "None", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No appointments found", color = Color.Gray, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // ── Appointment Cards ──
                items(filtered, key = { it.id }) { appt ->
                    AllAppointmentCard(
                        appointment = appt,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(title: String, value: String, textColor: Color, bgColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(title, fontSize = 10.sp, color = textColor.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun AppointmentStatCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun AllAppointmentCard(appointment: AllAppointmentItem, modifier: Modifier = Modifier) {
    val statusColor = when (appointment.status) {
        "completed" -> Color(0xFF4CAF50)
        "cancelled" -> Color.Red
        "confirmed" -> Color(0xFF1565C0)
        else -> Color(0xFFF57F17)
    }
    val typeLabel = when (appointment.appointmentType) {
        "followup" -> "Follow-up"
        "report" -> "Report Review"
        "walkin" -> "Walk-in"
        else -> "New Visit"
    }
    val typeIcon = when (appointment.appointmentType) {
        "followup" -> "🔄"
        "report" -> "📋"
        "walkin" -> "🚶"
        else -> "🆕"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Patient + Doctor + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1565C0).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            appointment.patientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1565C0)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(appointment.patientName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Dr. ${appointment.doctorName}", fontSize = 12.sp, color = Color(0xFF1565C0), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        appointment.status.replaceFirstChar { it.uppercase() },
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(10.dp))

            // Date + Type + Serial + Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Date", tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Text(appointment.date, fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(typeIcon, fontSize = 11.sp)
                        Text(typeLabel, fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Tag, contentDescription = "Serial", tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Text("Serial #${appointment.serialNumber}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (appointment.fee == "0") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("FREE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    } else {
                        Text("BDT", fontSize = 10.sp, color = Color.Gray)
                        Text(appointment.fee, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                }
            }

            // Reason
            if (appointment.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8F8F8))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Notes, contentDescription = "Reason", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(appointment.reason, fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }
}