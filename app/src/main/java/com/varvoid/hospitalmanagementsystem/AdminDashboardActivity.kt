package com.varvoid.hospitalmanagementsystem

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminDashboardActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                AdminDashboardScreen(
                    auth = auth,
                    db = db,
                    onLogout = {
                        notificationListener?.remove()
                        auth.signOut()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onPendingDoctors = {
                        startActivity(Intent(this, PendingDoctorsActivity::class.java))
                    },
                    onAllDoctors = {
                        startActivity(Intent(this, AllDoctorsActivity::class.java))
                    },
                    onAllPatients = {
                        startActivity(Intent(this, AllPatientsActivity::class.java))
                    },
                    onAllAppointments = {
                        startActivity(Intent(this, AllAppointmentsActivity::class.java))
                    },
                    onManageReceptionists = {
                        startActivity(Intent(this, CreateReceptionistActivity::class.java))
                    },
                    onViewReceptionists = {
                        startActivity(Intent(this, AllReceptionistsActivity::class.java))
                    },
                    onAnalytics = {
                        startActivity(Intent(this, AnalyticsDashboardActivity::class.java))
                    },
                    onNotifications = {
                        startActivity(Intent(this, NotificationActivity::class.java))
                    },
                    onRegisterNotificationListener = { listener ->
                        notificationListener?.remove()
                        notificationListener = listener
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListener?.remove()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onLogout: () -> Unit,
    onPendingDoctors: () -> Unit,
    onAllDoctors: () -> Unit,
    onAllPatients: () -> Unit,
    onAllAppointments: () -> Unit,
    onManageReceptionists: () -> Unit,
    onViewReceptionists: () -> Unit,
    onAnalytics: () -> Unit,
    onNotifications: () -> Unit,
    onRegisterNotificationListener: (ListenerRegistration) -> Unit
) {
    var totalDoctors by remember { mutableStateOf(0) }
    var totalPatients by remember { mutableStateOf(0) }
    var pendingDoctors by remember { mutableStateOf(0) }
    var totalReceptionists by remember { mutableStateOf(0) }
    var unreadNotifications by remember { mutableStateOf(0) }
    var totalAppointments by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val userId = auth.currentUser?.uid ?: ""

    // ✅ Real-time notification listener
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            try {
                val listener = db.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        unreadNotifications = snapshot?.documents?.count { doc ->
                            !(doc.getBoolean("read") ?: false)
                        } ?: 0
                    }
                onRegisterNotificationListener(listener)
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            db.collection("users").whereEqualTo("role", "doctor").get()
                .addOnSuccessListener { documents ->
                    totalDoctors = documents.size()
                    pendingDoctors = documents.count { !(it.getBoolean("isApproved") ?: false) }
                }
            db.collection("users").whereEqualTo("role", "patient").get()
                .addOnSuccessListener { totalPatients = it.size() }
            db.collection("users").whereEqualTo("role", "receptionist").get()
                .addOnSuccessListener { totalReceptionists = it.size() }
            db.collection("appointments").get()
                .addOnSuccessListener { totalAppointments = it.size() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White
                ),
                actions = {
                    // ✅ Notification bell
                    Box {
                        IconButton(onClick = onNotifications) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                        if (unreadNotifications > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp),
                                containerColor = Color.Red
                            ) {
                                Text(
                                    if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Stats Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("System Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(title = "Doctors", value = totalDoctors.toString())
                        StatItem(title = "Patients", value = totalPatients.toString())
                        StatItem(title = "Pending", value = pendingDoctors.toString())
                        StatItem(title = "Staff", value = totalReceptionists.toString())
                    }
                }
            }

            // ── Doctor Management ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Doctor Management", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), modifier = Modifier.padding(bottom = 12.dp))
                    DashboardButton(icon = Icons.Filled.HourglassEmpty, title = "Pending Approvals", subtitle = "$pendingDoctors doctors waiting", onClick = onPendingDoctors)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(icon = Icons.Filled.LocalHospital, title = "All Doctors", subtitle = "$totalDoctors doctors registered", onClick = onAllDoctors)
                }
            }

            // ── Patient Management ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Patient Management", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), modifier = Modifier.padding(bottom = 12.dp))
                    DashboardButton(icon = Icons.Filled.People, title = "All Patients", subtitle = "$totalPatients patients registered", onClick = onAllPatients)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(icon = Icons.Filled.CalendarToday, title = "All Appointments", subtitle = "$totalAppointments total appointments", onClick = onAllAppointments)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    // ✅ Analytics button
                    DashboardButton(
                        icon = Icons.Filled.Analytics,
                        title = "Analytics Dashboard",
                        subtitle = "Charts & insights",
                        onClick = onAnalytics
                    )
                }
            }

            // ── Staff Management ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Staff Management", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), modifier = Modifier.padding(bottom = 12.dp))
                    DashboardButton(
                        icon = Icons.Filled.PersonAdd,
                        title = "Create Receptionist",
                        subtitle = "Add new receptionist account",
                        onClick = onManageReceptionists
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(
                        icon = Icons.Filled.ManageAccounts,
                        title = "View All Receptionists",
                        subtitle = "$totalReceptionists receptionists registered",
                        onClick = onViewReceptionists
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}