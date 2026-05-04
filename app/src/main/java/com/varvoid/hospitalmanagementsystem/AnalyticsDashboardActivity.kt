package com.varvoid.hospitalmanagementsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

data class AnalyticsData(
    val totalAppointments: Int = 0,
    val completedAppointments: Int = 0,
    val cancelledAppointments: Int = 0,
    val pendingAppointments: Int = 0,
    val totalPatients: Int = 0,
    val totalDoctors: Int = 0,
    val totalRevenue: Int = 0,
    val appointmentsByType: Map<String, Int> = emptyMap(),
    val appointmentsByDay: Map<String, Int> = emptyMap()
)

class AnalyticsDashboardActivity : ComponentActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                AnalyticsDashboardScreen(db = db, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    db: FirebaseFirestore,
    onBack: () -> Unit
) {
    var analytics by remember { mutableStateOf(AnalyticsData()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        var appointments = listOf<Map<String, Any?>>()
        var patients = 0
        var doctors = 0
        var loaded = 0

        db.collection("appointments").get().addOnSuccessListener { docs ->
            appointments = docs.documents.map { doc ->
                mapOf(
                    "status" to doc.getString("status"),
                    "appointmentType" to doc.getString("appointmentType"),
                    "fee" to doc.getString("fee"),
                    "date" to doc.getString("date")
                )
            }
            loaded++
            if (loaded == 3) processData(appointments, patients, doctors) { analytics = it; isLoading = false }
        }

        db.collection("users").whereEqualTo("role", "patient").get()
            .addOnSuccessListener { docs -> patients = docs.size(); loaded++; if (loaded == 3) processData(appointments, patients, doctors) { analytics = it; isLoading = false } }

        db.collection("users").whereEqualTo("role", "doctor").get()
            .addOnSuccessListener { docs -> doctors = docs.size(); loaded++; if (loaded == 3) processData(appointments, patients, doctors) { analytics = it; isLoading = false } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", fontWeight = FontWeight.Bold) },
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
                // Overview Stats
                Text("Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsStatCard("Total Appointments", analytics.totalAppointments.toString(), Color(0xFF1565C0), Modifier.weight(1f))
                    AnalyticsStatCard("Patients", analytics.totalPatients.toString(), Color(0xFF4CAF50), Modifier.weight(1f))
                    AnalyticsStatCard("Doctors", analytics.totalDoctors.toString(), Color(0xFFF57F17), Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsStatCard("Completed", analytics.completedAppointments.toString(), Color(0xFF4CAF50), Modifier.weight(1f))
                    AnalyticsStatCard("Pending", analytics.pendingAppointments.toString(), Color(0xFFF57F17), Modifier.weight(1f))
                    AnalyticsStatCard("Cancelled", analytics.cancelledAppointments.toString(), Color.Red, Modifier.weight(1f))
                }

                // Revenue
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Total Revenue", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("BDT ${analytics.totalRevenue}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("From ${analytics.completedAppointments} completed appointments", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Appointment Status Pie Chart
                Text("Appointment Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (analytics.totalAppointments > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pie Chart
                                Canvas(
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    drawPieChart(
                                        completed = analytics.completedAppointments,
                                        pending = analytics.pendingAppointments,
                                        cancelled = analytics.cancelledAppointments,
                                        total = analytics.totalAppointments
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Legend
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PieLegendItem("Completed", analytics.completedAppointments, Color(0xFF4CAF50), analytics.totalAppointments)
                                    PieLegendItem("Pending", analytics.pendingAppointments, Color(0xFFF57F17), analytics.totalAppointments)
                                    PieLegendItem("Cancelled", analytics.cancelledAppointments, Color.Red, analytics.totalAppointments)
                                }
                            }
                        } else {
                            Text("No appointment data yet", color = Color.Gray)
                        }
                    }
                }

                // Appointment Type Bar Chart
                Text("Appointment Types", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val typeMap = analytics.appointmentsByType
                        val maxVal = typeMap.values.maxOrNull()?.toFloat() ?: 1f

                        listOf(
                            Triple("New Visit", typeMap["new"] ?: 0, Color(0xFF1565C0)),
                            Triple("Follow-up", typeMap["followup"] ?: 0, Color(0xFF4CAF50)),
                            Triple("Report Review", typeMap["report"] ?: 0, Color(0xFFF57F17)),
                            Triple("Walk-in", typeMap["walkin"] ?: 0, Color(0xFF9C27B0))
                        ).forEach { (label, value, color) ->
                            BarChartRow(label = label, value = value, maxValue = maxVal, color = color)
                        }
                    }
                }

                // Recent Activity
                Text("Quick Insights", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val completionRate = if (analytics.totalAppointments > 0)
                            (analytics.completedAppointments * 100 / analytics.totalAppointments) else 0
                        val cancellationRate = if (analytics.totalAppointments > 0)
                            (analytics.cancelledAppointments * 100 / analytics.totalAppointments) else 0

                        InsightRow("✅ Completion Rate", "$completionRate%",
                            if (completionRate >= 70) Color(0xFF4CAF50) else Color(0xFFF57F17))
                        HorizontalDivider()
                        InsightRow("❌ Cancellation Rate", "$cancellationRate%",
                            if (cancellationRate <= 20) Color(0xFF4CAF50) else Color.Red)
                        HorizontalDivider()
                        InsightRow("💰 Avg Revenue/Appointment",
                            if (analytics.completedAppointments > 0)
                                "BDT ${analytics.totalRevenue / analytics.completedAppointments}"
                            else "N/A",
                            Color(0xFF1565C0))
                        HorizontalDivider()
                        InsightRow("👥 Patients per Doctor",
                            if (analytics.totalDoctors > 0)
                                "${analytics.totalPatients / analytics.totalDoctors}"
                            else "N/A",
                            Color(0xFF1565C0))
                    }
                }
            }
        }
    }
}

fun processData(
    appointments: List<Map<String, Any?>>,
    patients: Int,
    doctors: Int,
    onResult: (AnalyticsData) -> Unit
) {
    val total = appointments.size
    val completed = appointments.count { it["status"] == "completed" }
    val cancelled = appointments.count { it["status"] == "cancelled" }
    val pending = appointments.count { it["status"] == "pending" || it["status"] == "confirmed" }

    val revenue = appointments
        .filter { it["status"] == "completed" }
        .sumOf { (it["fee"] as? String)?.toIntOrNull() ?: 0 }

    val byType = mapOf(
        "new" to appointments.count { it["appointmentType"] == "new" },
        "followup" to appointments.count { it["appointmentType"] == "followup" },
        "report" to appointments.count { it["appointmentType"] == "report" },
        "walkin" to appointments.count { it["appointmentType"] == "walkin" }
    )

    onResult(AnalyticsData(
        totalAppointments = total,
        completedAppointments = completed,
        cancelledAppointments = cancelled,
        pendingAppointments = pending,
        totalPatients = patients,
        totalDoctors = doctors,
        totalRevenue = revenue,
        appointmentsByType = byType
    ))
}

fun DrawScope.drawPieChart(completed: Int, pending: Int, cancelled: Int, total: Int) {
    if (total == 0) return
    val cx = size.width / 2
    val cy = size.height / 2
    val radius = minOf(cx, cy) - 10f
    val rect = androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius)

    var startAngle = -90f
    listOf(
        completed to android.graphics.Color.parseColor("#4CAF50"),
        pending to android.graphics.Color.parseColor("#F57F17"),
        cancelled to android.graphics.Color.RED
    ).forEach { (value, color) ->
        if (value > 0) {
            val sweep = 360f * value / total
            drawArc(
                color = Color(color),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2)
            )
            startAngle += sweep
        }
    }
    // White center circle (donut)
    drawCircle(color = Color.White, radius = radius * 0.5f, center = Offset(cx, cy))
}

@Composable
fun PieLegendItem(label: String, value: Int, color: Color, total: Int) {
    val pct = if (total > 0) (value * 100 / total) else 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp, 12.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = color)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: $value ($pct%)", fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun BarChartRow(label: String, value: Int, maxValue: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(110.dp), fontSize = 12.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(22.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = if (maxValue > 0) size.width * value / maxValue else 0f
                drawRoundRect(
                    color = color.copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                )
                if (barWidth > 0) {
                    drawRoundRect(
                        color = color,
                        size = Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    value.toString(),
                    barWidth + 8f,
                    size.height / 2 + 5f,
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.DKGRAY
                        textSize = 28f
                    }
                )
            }
        }
    }
}

@Composable
fun InsightRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color.DarkGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun AnalyticsStatCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 10.sp, color = color.copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}