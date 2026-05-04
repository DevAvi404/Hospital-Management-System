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
import com.google.firebase.firestore.FirebaseFirestore

data class ReceptionistItem(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val assignedDoctorName: String = "",
    val assignedDoctorId: String = ""
)

class AllReceptionistsActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                AllReceptionistsScreen(
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
fun AllReceptionistsScreen(
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var receptionists by remember { mutableStateOf(listOf<ReceptionistItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedReceptionist by remember { mutableStateOf<ReceptionistItem?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "receptionist")
            .get()
            .addOnSuccessListener { docs ->
                receptionists = docs.map { doc ->
                    ReceptionistItem(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        assignedDoctorName = doc.getString("assignedDoctorName") ?: "",
                        assignedDoctorId = doc.getString("assignedDoctorId") ?: ""
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load receptionists", Toast.LENGTH_SHORT).show()
            }
    }

    // Delete Dialog
    if (showDeleteDialog && selectedReceptionist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Receptionist") },
            text = { Text("Are you sure you want to remove ${selectedReceptionist!!.name}? This cannot be undone!") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("users").document(selectedReceptionist!!.userId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "${selectedReceptionist!!.name} removed!", Toast.LENGTH_SHORT).show()
                                receptionists = receptionists.filter { it.userId != selectedReceptionist!!.userId }
                                showDeleteDialog = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Receptionists", fontWeight = FontWeight.Bold) },
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
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search receptionists...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredReceptionists = receptionists.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.email.contains(searchQuery, ignoreCase = true) ||
                            it.assignedDoctorName.contains(searchQuery, ignoreCase = true)
                }

                Text(
                    text = "${filteredReceptionists.size} receptionist(s) found",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (filteredReceptionists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.PersonOff,
                                contentDescription = "No receptionists",
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No receptionists found", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredReceptionists) { receptionist ->
                            ReceptionistItemCard(
                                receptionist = receptionist,
                                onViewDetails = {
                                    val intent = Intent(context, ReceptionistDetailActivity::class.java)
                                    intent.putExtra("receptionistId", receptionist.userId)
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    selectedReceptionist = receptionist
                                    showDeleteDialog = true
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
fun ReceptionistItemCard(
    receptionist: ReceptionistItem,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.SupportAgent,
                    contentDescription = "Receptionist",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(receptionist.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(receptionist.email, fontSize = 13.sp, color = Color.Gray)
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        "Active",
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

            DoctorInfoRow(label = "Assigned Doctor", value = "Dr. ${receptionist.assignedDoctorName}")

            Spacer(modifier = Modifier.height(12.dp))

            // View Details Button
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Filled.Visibility, contentDescription = "View", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full Profile", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remove Button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove Receptionist", color = Color.Red)
            }
        }
    }
}