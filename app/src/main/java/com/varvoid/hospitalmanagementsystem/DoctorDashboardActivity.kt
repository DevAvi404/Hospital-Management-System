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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorDashboardActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var notificationListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                DoctorDashboardScreen(
                    auth = auth,
                    db = db,
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onEditProfile = {
                        startActivity(Intent(this, DoctorEditProfileActivity::class.java))
                    },
                    onMedicineSearch = {
                        startActivity(Intent(this, MedicineSearchActivity::class.java))
                    },
                    // onViewAppointments
                    onViewAppointments = {
                        startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
                    },
                    // onMyPatients
                    onMyPatients = {
                        startActivity(Intent(this, MyPatientsActivity::class.java))
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
fun DoctorDashboardScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onLogout: () -> Unit,
    onMedicineSearch: () -> Unit,
    onViewAppointments: () -> Unit,
    onMyPatients: () -> Unit,
    onEditProfile: () -> Unit,
    onNotifications: () -> Unit,
    onRegisterNotificationListener: (com.google.firebase.firestore.ListenerRegistration) -> Unit
) {
    var doctorName by remember { mutableStateOf("Doctor") }
    var specialization by remember { mutableStateOf("Not set") }
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

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        doctorName = document.getString("name") ?: "Doctor"
                        specialization = document.getString("specialization") ?: "Not set"
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Dashboard", fontWeight = FontWeight.Bold) },
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
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
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
                        text = "Welcome, Dr. $doctorName!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Specialization: $specialization",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Quick Actions Card
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
                        icon = Icons.Filled.CalendarToday,
                        title = "View Appointments",
                        subtitle = "Check your schedule",
                        onClick = onViewAppointments
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DashboardButton(
                        icon = Icons.Filled.People,
                        title = "My Patients",
                        subtitle = "View your patient list",
                        onClick = onMyPatients
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DashboardButton(
                        icon = Icons.Filled.Search,
                        title = "Medicine Search",
                        subtitle = "Search medicine via API",
                        onClick = onMedicineSearch
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

@Composable
fun DashboardButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF1565C0),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Go",
                tint = Color.Gray
            )
        }
    }
}