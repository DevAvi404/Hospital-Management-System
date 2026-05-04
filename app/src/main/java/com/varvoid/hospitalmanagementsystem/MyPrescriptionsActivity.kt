package com.varvoid.hospitalmanagementsystem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class PrescriptionItem(
    val id: String = "",
    val doctorName: String = "",
    val diagnosis: String = "",
    val medicines: List<Map<String, String>> = listOf(),
    val notes: String = "",
    val nextVisit: String = "",
    val date: String = ""
)

class MyPrescriptionsActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                MyPrescriptionsScreen(
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
fun MyPrescriptionsScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    var prescriptions by remember { mutableStateOf(listOf<PrescriptionItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPrescription by remember { mutableStateOf<PrescriptionItem?>(null) }
    var exportingId by remember { mutableStateOf("") }
    val patientId = auth.currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        db.collection("prescriptions")
            .whereEqualTo("patientId", patientId)
            .get()
            .addOnSuccessListener { docs ->
                val list = mutableListOf<PrescriptionItem>()
                var count = docs.size()
                if (count == 0) {
                    isLoading = false
                    return@addOnSuccessListener
                }
                docs.forEach { doc ->
                    val doctorId = doc.getString("doctorId") ?: ""
                    db.collection("users").document(doctorId)
                        .get()
                        .addOnSuccessListener { doctorDoc ->
                            val doctorName = doctorDoc.getString("name") ?: ""
                            val medicines = (doc.get("medicines") as? List<*>)
                                ?.filterIsInstance<Map<String, String>>() ?: listOf()
                            list.add(
                                PrescriptionItem(
                                    id = doc.id,
                                    doctorName = doctorName,
                                    diagnosis = doc.getString("diagnosis") ?: "",
                                    medicines = medicines,
                                    notes = doc.getString("notes") ?: "",
                                    nextVisit = doc.getString("nextVisit") ?: "",
                                    date = doc.getString("date") ?: ""
                                )
                            )
                            count--
                            if (count == 0) {
                                prescriptions = list.sortedByDescending { it.date }
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load prescriptions", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Export function
    fun exportPrescription(prescription: PrescriptionItem) {
        exportingId = prescription.id
        db.collection("prescriptions").document(prescription.id).get()
            .addOnSuccessListener { doc ->
                val medicines = (doc.get("medicines") as? List<*>)
                    ?.filterIsInstance<Map<String, String>>() ?: emptyList()
                val investigations = (doc.get("investigations") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val vitals = (doc.get("vitals") as? Map<String, Any>)
                    ?.mapValues { it.value.toString() } ?: emptyMap()
                val patientName = doc.getString("patientName") ?: "Patient"

                PdfExportUtils.exportPrescriptionPdf(
                    context = context,
                    patientName = patientName,
                    doctorName = prescription.doctorName,
                    date = prescription.date,
                    diagnosis = prescription.diagnosis,
                    medicines = medicines,
                    investigations = investigations,
                    notes = prescription.notes,
                    nextVisit = prescription.nextVisit,
                    vitals = vitals
                )
                exportingId = ""
            }
            .addOnFailureListener {
                exportingId = ""
                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Prescription Detail Dialog ──
    selectedPrescription?.let { prescription ->
        AlertDialog(
            onDismissRequest = { selectedPrescription = null },
            title = {
                Column {
                    Text("Prescription", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    Text("Dr. ${prescription.doctorName}", fontSize = 13.sp, color = Color.Gray)
                    Text("Date: ${prescription.date}", fontSize = 12.sp, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Diagnosis
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Diagnosis", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(prescription.diagnosis, fontSize = 14.sp, color = Color.DarkGray)
                        }
                    }

                    // Medicines
                    Text("💊 Medicines", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    prescription.medicines.forEachIndexed { index, medicine ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${index + 1}. ${medicine["name"] ?: ""}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (!medicine["dosage"].isNullOrEmpty())
                                    Text("Dosage: ${medicine["dosage"]}", fontSize = 12.sp, color = Color.Gray)
                                if (!medicine["duration"].isNullOrEmpty())
                                    Text("Duration: ${medicine["duration"]}", fontSize = 12.sp, color = Color.Gray)
                                if (!medicine["instructions"].isNullOrEmpty())
                                    Text("Instructions: ${medicine["instructions"]}", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                        }
                    }

                    // Notes
                    if (prescription.notes.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Doctor's Notes", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(prescription.notes, fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    // Next Visit
                    if (prescription.nextVisit.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Next Visit", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Next Visit: ${prescription.nextVisit}", fontSize = 13.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // ✅ Export PDF Button inside dialog
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            selectedPrescription = null
                            exportPrescription(prescription)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Icon(
                            Icons.Filled.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export as PDF", color = Color.White, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPrescription = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Prescriptions", fontWeight = FontWeight.Bold) },
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
        } else if (prescriptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MedicalServices, contentDescription = "No prescriptions", tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No prescriptions yet", fontSize = 16.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${prescriptions.size} prescription(s)", fontSize = 14.sp, color = Color.Gray)
                }
                items(prescriptions) { prescription ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // ── Header row ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MedicalServices, contentDescription = "Prescription", tint = Color(0xFF1565C0), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Dr. ${prescription.doctorName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(prescription.date, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = "View", tint = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Diagnosis: ${prescription.diagnosis}", fontSize = 13.sp, color = Color.DarkGray)
                            Text("${prescription.medicines.size} medicine(s) prescribed", fontSize = 12.sp, color = Color.Gray)
                            if (prescription.nextVisit.isNotEmpty()) {
                                Text("Next Visit: ${prescription.nextVisit}", fontSize = 12.sp, color = Color(0xFF4CAF50))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ── Action buttons row ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // View details button
                                OutlinedButton(
                                    onClick = { selectedPrescription = prescription },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = "View", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View", fontSize = 13.sp)
                                }

                                // ✅ Export PDF button
                                Button(
                                    onClick = { exportPrescription(prescription) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    enabled = exportingId != prescription.id
                                ) {
                                    if (exportingId == prescription.id) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PDF", color = Color.White, fontSize = 13.sp)
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