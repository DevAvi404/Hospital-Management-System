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
import java.text.SimpleDateFormat
import java.util.*

class ReceptionistDashboardActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var notificationListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                ReceptionistDashboardScreen(
                    auth = auth,
                    db = db,
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onTodayQueue = {
                        startActivity(Intent(this, TodayQueueActivity::class.java))
                    },

                    onEditProfile = {
                        startActivity(Intent(this, ReceptionistEditProfileActivity::class.java))
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
fun ReceptionistDashboardScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onLogout: () -> Unit,
    onTodayQueue: () -> Unit,
    onEditProfile: () -> Unit,
    onNotifications: () -> Unit,
    onRegisterNotificationListener: (com.google.firebase.firestore.ListenerRegistration) -> Unit
) {
    var receptionistName by remember { mutableStateOf("Receptionist") }
    var assignedDoctorId by remember { mutableStateOf("") }
    var assignedDoctorName by remember { mutableStateOf("") }
    var todayTotal by remember { mutableStateOf(0) }
    var todayCompleted by remember { mutableStateOf(0) }
    var currentSerial by remember { mutableStateOf(0) }
    var unreadNotifications by remember { mutableStateOf(0) }

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
            } catch (e: Exception) { /* Silent fail */ }
        }
    }

    val today = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            val userId = auth.currentUser?.uid ?: return@LaunchedEffect
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    receptionistName = document.getString("name") ?: "Receptionist"
                    assignedDoctorId = document.getString("assignedDoctorId") ?: ""

                    if (assignedDoctorId.isNotEmpty()) {
                        // Load doctor name
                        db.collection("users").document(assignedDoctorId)
                            .get()
                            .addOnSuccessListener { doctorDoc ->
                                assignedDoctorName = doctorDoc.getString("name") ?: ""
                            }

                        // Load today's stats
                        db.collection("appointments")
                            .whereEqualTo("doctorId", assignedDoctorId)
                            .whereEqualTo("date", today)
                            .get()
                            .addOnSuccessListener { docs ->
                                todayTotal = docs.size()
                                todayCompleted = docs.count { it.getString("status") == "completed" }
                            }

                        // Load current serial
                        db.collection("queue").document(assignedDoctorId)
                            .get()
                            .addOnSuccessListener { queueDoc ->
                                currentSerial = (queueDoc.getLong("currentSerial") ?: 0).toInt()
                            }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receptionist Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White
                ),
                actions = {
                    // ✅ Notification bell with badge
                    Box {
                        IconButton(onClick = { onNotifications() }) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                        if (unreadNotifications > 0) {
                            Badge(
                                modifier = androidx.compose.ui.Modifier
                                    .align(androidx.compose.ui.Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp),
                                containerColor = Color.Red
                            ) {
                                Text(
                                    text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
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
            // Welcome Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Welcome, $receptionistName!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Assigned to: Dr. $assignedDoctorName",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Today: $today",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Today's Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Total - Blue
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = todayTotal.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Text("Total", fontSize = 12.sp, color = Color.Gray)
                        }
                        // Done - Green
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = todayCompleted.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text("Done", fontSize = 12.sp, color = Color.Gray)
                        }
                        // Waiting - Orange
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (todayTotal - todayCompleted).toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                            Text("Waiting", fontSize = 12.sp, color = Color.Gray)
                        }
                        // Current - Blue
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "#$currentSerial",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Text("Current", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Quick Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    DashboardButton(
                        icon = Icons.Filled.List,
                        title = "Today's Queue",
                        subtitle = "$todayTotal patients — $todayCompleted completed",
                        onClick = onTodayQueue
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(
                        icon = Icons.Filled.Person,
                        title = "Edit Profile",
                        subtitle = "Update your information",
                        onClick = onEditProfile
                    )
                }
            }
        }
    }
}