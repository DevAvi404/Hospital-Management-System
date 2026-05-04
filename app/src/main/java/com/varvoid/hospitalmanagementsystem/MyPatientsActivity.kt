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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class MyPatientItem(
    val patientId: String = "",
    val patientName: String = "",
    val lastVisitDate: String = "",
    val lastVisitReason: String = "",
    val totalVisits: Int = 0,
    val lastAppointmentType: String = ""
)

class MyPatientsActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                MyPatientsScreen(
                    auth = auth,
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
fun MyPatientsScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var patients by remember { mutableStateOf(listOf<MyPatientItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val doctorId = auth.currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { docs ->
                // Group by patientId
                val patientMap = mutableMapOf<String, MutableList<Map<String, Any?>>>()
                docs.forEach { doc ->
                    val patientId = doc.getString("patientId") ?: return@forEach
                    if (patientId == "walkin") return@forEach
                    if (!patientMap.containsKey(patientId)) {
                        patientMap[patientId] = mutableListOf()
                    }
                    patientMap[patientId]!!.add(mapOf(
                        "date" to (doc.getString("date") ?: ""),
                        "reason" to (doc.getString("reason") ?: ""),
                        "type" to (doc.getString("appointmentType") ?: "new"),
                        "patientName" to (doc.getString("patientName") ?: "")
                    ))
                }

                val patientList = mutableListOf<MyPatientItem>()
                var pendingCount = patientMap.size

                if (pendingCount == 0) {
                    patients = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                patientMap.forEach { (patientId, visits) ->
                    val sortedVisits = visits.sortedByDescending { it["date"] as String }
                    val lastVisit = sortedVisits.first()
                    patientList.add(
                        MyPatientItem(
                            patientId = patientId,
                            patientName = lastVisit["patientName"] as String,
                            lastVisitDate = lastVisit["date"] as String,
                            lastVisitReason = lastVisit["reason"] as String,
                            totalVisits = visits.size,
                            lastAppointmentType = lastVisit["type"] as String
                        )
                    )
                    pendingCount--
                    if (pendingCount == 0) {
                        patients = patientList.sortedByDescending { it.lastVisitDate }
                        isLoading = false
                    }
                }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load patients", Toast.LENGTH_SHORT).show()
            }
    }

    val filteredPatients = patients.filter {
        it.patientName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Patients", fontWeight = FontWeight.Bold) },
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search patients...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("${filteredPatients.size} patient(s)", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredPatients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.People, contentDescription = "No patients", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No patients found", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredPatients) { patient ->
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
                                        Icon(Icons.Filled.Person, contentDescription = "Patient", tint = Color(0xFF1565C0), modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(patient.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Last visit: ${patient.lastVisitDate}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                                        ) {
                                            Text(
                                                "${patient.totalVisits} visit(s)",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "Last reason: ${patient.lastVisitReason.ifEmpty { "Not specified" }}",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        "Type: ${when(patient.lastAppointmentType) {
                                            "followup" -> "Follow-up"
                                            "report" -> "Report Review"
                                            else -> "New Visit"
                                        }}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val intent = Intent(context, DoctorPatientDetailsActivity::class.java)
                                            intent.putExtra("patientId", patient.patientId)
                                            intent.putExtra("doctorId", doctorId)
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = "View", tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("View Patient Details", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}