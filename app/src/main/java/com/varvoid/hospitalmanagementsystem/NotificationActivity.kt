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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Long = 0L
)

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        // ✅ Get userId safely at activity level
        val userId = auth.currentUser?.uid ?: ""

        setContent {
            MaterialTheme {
                if (userId.isEmpty()) {
                    // Safety: user not logged in
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please login to view notifications")
                    }
                } else {
                    NotificationScreen(
                        db = db,
                        userId = userId,
                        context = this,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    db: FirebaseFirestore,
    userId: String,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var notifications by remember { mutableStateOf(listOf<NotificationItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    /*
    fun loadNotifications() {
        isLoading = true
        errorMsg = ""
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                try {
                    notifications = docs.documents.mapNotNull { doc ->
                        try {
                            NotificationItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Notification",
                                message = doc.getString("message") ?: "",
                                read = doc.getBoolean("read") ?: false,
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.createdAt }
                } catch (e: Exception) {
                    errorMsg = "Error loading notifications"
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMsg = e.message ?: "Failed to load notifications"
            }
    }
    */
    fun startNotificationListener(userId: String) {
        isLoading = true
        errorMsg = ""
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMsg = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            NotificationItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Notification",
                                message = doc.getString("message") ?: "",
                                read = doc.getBoolean("read") ?: false,
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                }
            }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            startNotificationListener(userId)
        }
    }

    val unreadCount = notifications.count { !it.read }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Red
                            ) {
                                Text(
                                    "$unreadCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
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
                    if (unreadCount > 0) {
                        TextButton(onClick = {
                            val batch = db.batch()
                            notifications.filter { !it.read }.forEach { notif ->
                                val ref = db.collection("notifications").document(notif.id)
                                batch.update(ref, "read", true)
                            }
                            batch.commit().addOnFailureListener {
                                Toast.makeText(context, "Failed to mark all read", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Mark all read", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = Color(0xFF1565C0),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMsg.isNotEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMsg, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { if (userId.isNotEmpty()) startNotificationListener(userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) { Text("Retry") }
                    }
                }
                notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.NotificationsNone,
                            contentDescription = "No notifications",
                            tint = Color.LightGray,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No notifications yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You'll receive a notification when your\nappointment turn is coming soon!",
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkRead = {
                                    if (notification.id.isNotEmpty()) {
                                        db.collection("notifications").document(notification.id)
                                            .update("read", true)
                                    }
                                },
                                onDelete = {
                                    if (notification.id.isNotEmpty()) {
                                        db.collection("notifications").document(notification.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (notification.read) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) Color.White else Color(0xFFE3F2FD)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notification.read) Color(0xFFBDBDBD) else Color(0xFF1565C0)
                )
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notification",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp).padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.read) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text(
                                "NEW",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(notification.message, fontSize = 13.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!notification.read) {
                        TextButton(
                            onClick = onMarkRead,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1565C0))
                        ) { Text("Mark as read", fontSize = 12.sp) }
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("Delete", fontSize = 12.sp) }
                }
            }
        }
    }
}