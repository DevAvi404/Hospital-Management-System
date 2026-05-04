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

class PatientDashboardActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                PatientDashboardScreen(
                    auth = auth,
                    db = db,
                    onLogout = {
                        notificationListener?.remove()
                        auth.signOut()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onBookAppointment = {
                        startActivity(Intent(this, DoctorListActivity::class.java))
                    },
                    onMyAppointments = {
                        startActivity(Intent(this, MyAppointmentsActivity::class.java))
                    },
                    onMyPrescriptions = {
                        startActivity(Intent(this, MyPrescriptionsActivity::class.java))
                    },
                    onSymptomChecker = {
                        startActivity(Intent(this, SymptomCheckerActivity::class.java))
                    },
                    onMedicineSearch = {
                        startActivity(Intent(this, MedicineSearchActivity::class.java))
                    },
                    onNotifications = {
                        startActivity(Intent(this, NotificationActivity::class.java))
                    },
                    onEditProfile = {
                        startActivity(Intent(this, EditProfileActivity::class.java))
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
fun PatientDashboardScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onLogout: () -> Unit,
    onBookAppointment: () -> Unit,
    onMyAppointments: () -> Unit,
    onMyPrescriptions: () -> Unit,
    onSymptomChecker: () -> Unit,
    onMedicineSearch: () -> Unit,
    onNotifications: () -> Unit,
    onEditProfile: () -> Unit,
    onRegisterNotificationListener: (ListenerRegistration) -> Unit
) {
    var patientName by remember { mutableStateOf("Patient") }
    var bloodGroup by remember { mutableStateOf("Not set") }
    var unreadNotifications by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val userId = auth.currentUser?.uid ?: ""

    // ✅ Real-time notification listener — NO composite index needed
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            try {
                val listener = db.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        // ✅ Count unread in code, not in query
                        unreadNotifications = snapshot?.documents?.count { doc ->
                            !(doc.getBoolean("read") ?: false)
                        } ?: 0
                    }
                onRegisterNotificationListener(listener)
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    // Load profile on resume
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED && userId.isNotEmpty()) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    patientName = document.getString("name") ?: "Patient"
                    bloodGroup = document.getString("bloodGroup") ?: "Not set"
                }

            // ✅ Check for appointment reminders every time dashboard opens
            AppointmentReminderUtils.checkAndSendReminders(
                db = db,
                patientId = userId
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White
                ), /*
                actions = {
                    // ✅ Notification bell — safe click
                    IconButton(onClick = { onNotifications() }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge {
                                        Text(unreadNotifications.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                } */

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

                    // ✅ Logout button
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
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
                        text = "Welcome, $patientName!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Blood Group: $bloodGroup",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (unreadNotifications > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = "🔔 You have $unreadNotifications new notification(s)!",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Appointments Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Appointments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    DashboardButton(
                        icon = Icons.Filled.Add,
                        title = "Book Appointment",
                        subtitle = "Schedule with a doctor",
                        onClick = onBookAppointment
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(
                        icon = Icons.Filled.CalendarToday,
                        title = "My Appointments",
                        subtitle = "View your appointments",
                        onClick = onMyAppointments
                    )
                }
            }

            // Health Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Health",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    DashboardButton(
                        icon = Icons.Filled.MedicalServices,
                        title = "My Prescriptions",
                        subtitle = "View your prescriptions",
                        onClick = onMyPrescriptions
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(
                        icon = Icons.Filled.Psychology,
                        title = "AI Symptom Checker",
                        subtitle = "Check symptoms with AI",
                        onClick = onSymptomChecker
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashboardButton(
                        icon = Icons.Filled.Search,
                        title = "Medicine Search",
                        subtitle = "Search medicines",
                        onClick = onMedicineSearch
                    )
                }
            }

            // Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
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