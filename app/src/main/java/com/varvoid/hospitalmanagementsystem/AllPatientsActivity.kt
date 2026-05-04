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
import kotlin.jvm.java

data class PatientItem(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val age: String = "",
    val bloodGroup: String = "",
    val city: String = "",
    val gender: String = ""
)

class AllPatientsActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                AllPatientsScreen(
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
fun AllPatientsScreen(
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var patients by remember { mutableStateOf(listOf<PatientItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPatient by remember { mutableStateOf<PatientItem?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "patient")
            .get()
            .addOnSuccessListener { documents ->
                patients = documents.map { doc ->
                    PatientItem(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "Not set",
                        age = doc.getString("age") ?: "Not set",
                        bloodGroup = doc.getString("bloodGroup") ?: "Not set",
                        city = doc.getString("city") ?: "Not set",
                        gender = doc.getString("gender") ?: "Not set"
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load patients", Toast.LENGTH_SHORT).show()
            }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedPatient != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Patient") },
            text = { Text("Are you sure you want to remove ${selectedPatient!!.name}? This action cannot be undone!") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("users").document(selectedPatient!!.userId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "${selectedPatient!!.name} removed!", Toast.LENGTH_SHORT).show()
                                patients = patients.filter { it.userId != selectedPatient!!.userId }
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
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Patients", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                    label = { Text("Search patients...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredPatients = patients.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.email.contains(searchQuery, ignoreCase = true) ||
                            it.city.contains(searchQuery, ignoreCase = true)
                }

                Text(
                    text = "${filteredPatients.size} patient(s) found",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (filteredPatients.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No patients found",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPatients) { patient ->
                            PatientItemCard(
                                patient = patient,
                                onViewDetails = {
                                    val intent = Intent(context, PatientDetailActivity::class.java)
                                    intent.putExtra("patientId", patient.userId)
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    selectedPatient = patient
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
fun PatientItemCard(
    patient: PatientItem,
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
                    Icons.Filled.Person,
                    contentDescription = "Patient",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = patient.email,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            DoctorInfoRow(label = "Phone", value = patient.phone)
            DoctorInfoRow(label = "Age", value = patient.age)
            DoctorInfoRow(label = "Blood Group", value = patient.bloodGroup)
            DoctorInfoRow(label = "City", value = patient.city)

            Spacer(modifier = Modifier.height(12.dp))

            // View Details Button
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
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
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove Patient", color = Color.Red)
            }
        }
    }
}