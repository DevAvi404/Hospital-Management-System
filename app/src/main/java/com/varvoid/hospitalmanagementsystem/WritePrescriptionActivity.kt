package com.varvoid.hospitalmanagementsystem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*

data class PrescriptionMedicine(
    val name: String = "",
    val dosage: String = "",
    val duration: String = "",
    val instructions: String = ""
)

class WritePrescriptionActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val patientId = intent.getStringExtra("patientId") ?: ""
        val patientName = intent.getStringExtra("patientName") ?: ""
        val appointmentId = intent.getStringExtra("appointmentId") ?: ""

        setContent {
            MaterialTheme {
                WritePrescriptionScreen(
                    auth = auth,
                    db = db,
                    patientId = patientId,
                    patientName = patientName,
                    appointmentId = appointmentId,
                    context = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePrescriptionScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    patientId: String,
    patientName: String,
    appointmentId: String,
    context: android.content.Context,
    onBack: () -> Unit
) {
    // Clinical Vitals
    var temperature by remember { mutableStateOf("") }
    var bloodPressure by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var oxygenSaturation by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var respiratoryRate by remember { mutableStateOf("") }
    var clinicalNotes by remember { mutableStateOf("") }

    // Diagnosis
    var diagnosis by remember { mutableStateOf("") }

    // Medicines
    var medicines by remember { mutableStateOf(listOf(PrescriptionMedicine())) }

    // Investigations
    var investigations by remember { mutableStateOf(listOf("")) }

    // Doctor Notes
    var notes by remember { mutableStateOf("") }
    var nextVisit by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val doctorId = auth.currentUser?.uid ?: ""
    val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Prescription", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Patient", tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Patient: $patientName", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Date: $today", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // 🩺 Section 1 — Clinical Vitals & Observations
            ProfileCard(title = "🩺 Clinical Vitals & Observations") {
                Text("Record the patient's current physical state", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temp (°F)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pulse,
                        onValueChange = { pulse = it },
                        label = { Text("Pulse/min") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bloodPressure,
                        onValueChange = { bloodPressure = it },
                        label = { Text("BP (mmHg)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("120/80") }
                    )
                    OutlinedTextField(
                        value = oxygenSaturation,
                        onValueChange = { oxygenSaturation = it },
                        label = { Text("SpO2 (%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = respiratoryRate,
                        onValueChange = { respiratoryRate = it },
                        label = { Text("Resp Rate/min") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = clinicalNotes,
                    onValueChange = { clinicalNotes = it },
                    label = { Text("Clinical Observations") },
                    placeholder = { Text("e.g. Patient appears pale, mild dehydration...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // 🔍 Diagnosis
            ProfileCard(title = "🔍 Diagnosis") {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Diagnosis / Chief Complaint") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // 💊 Section 2 — Medication & Treatment (Rx)
            ProfileCard(title = "💊 Medication & Treatment (Rx)") {
                medicines.forEachIndexed { index, medicine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Medicine ${index + 1}", fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
                                if (medicines.size > 1) {
                                    IconButton(onClick = {
                                        medicines = medicines.toMutableList().also { it.removeAt(index) }
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = medicine.name,
                                onValueChange = { medicines = medicines.toMutableList().also { list -> list[index] = list[index].copy(name = it) } },
                                label = { Text("Medicine Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = medicine.dosage,
                                onValueChange = { medicines = medicines.toMutableList().also { list -> list[index] = list[index].copy(dosage = it) } },
                                label = { Text("Dosage (e.g. 1+0+1, 0+0+1)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = medicine.duration,
                                onValueChange = { medicines = medicines.toMutableList().also { list -> list[index] = list[index].copy(duration = it) } },
                                label = { Text("Duration (e.g. 7 days, 2 weeks)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = medicine.instructions,
                                onValueChange = { medicines = medicines.toMutableList().also { list -> list[index] = list[index].copy(instructions = it) } },
                                label = { Text("Instructions (e.g. After food, Before sleep)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { medicines = medicines + PrescriptionMedicine() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Medicine")
                }
            }

            // 🧪 Section 3 — Investigations & Diagnostics
            ProfileCard(title = "🧪 Investigations & Diagnostics") {
                Text("Tests ordered for this patient", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                investigations.forEachIndexed { index, investigation ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = investigation,
                            onValueChange = {
                                investigations = investigations.toMutableList().also { list -> list[index] = it }
                            },
                            label = { Text("Test ${index + 1}") },
                            placeholder = { Text("e.g. CBC, Blood Sugar, X-Ray") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        if (investigations.size > 1) {
                            IconButton(onClick = {
                                investigations = investigations.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedButton(
                    onClick = { investigations = investigations + "" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Test", tint = Color(0xFF1565C0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Test")
                }
            }

            // 📝 Doctor's Notes
            ProfileCard(title = "📝 Doctor's Notes") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional notes or instructions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
                ProfileTextField(
                    value = nextVisit,
                    onValueChange = { nextVisit = it },
                    label = "Next Visit (e.g. After 7 days)"
                )
            }

            // Save Button
            Button(
                onClick = {
                    if (diagnosis.isEmpty()) {
                        Toast.makeText(context, "Please enter diagnosis!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (medicines.first().name.isEmpty()) {
                        Toast.makeText(context, "Please add at least one medicine!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true

                    val medicinesList = medicines.map {
                        mapOf("name" to it.name, "dosage" to it.dosage, "duration" to it.duration, "instructions" to it.instructions)
                    }

                    val vitals = mapOf(
                        "temperature" to temperature,
                        "bloodPressure" to bloodPressure,
                        "pulse" to pulse,
                        "oxygenSaturation" to oxygenSaturation,
                        "weight" to weight,
                        "respiratoryRate" to respiratoryRate,
                        "clinicalNotes" to clinicalNotes
                    )

                    val prescription = hashMapOf(
                        "doctorId" to doctorId,
                        "patientId" to patientId,
                        "patientName" to patientName,
                        "appointmentId" to appointmentId,
                        "vitals" to vitals,
                        "diagnosis" to diagnosis,
                        "medicines" to medicinesList,
                        "investigations" to investigations.filter { it.isNotEmpty() },
                        "notes" to notes,
                        "nextVisit" to nextVisit,
                        "date" to today,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("prescriptions")
                        .add(prescription)
                        .addOnSuccessListener {
                            // ✅ Add Notification for Patient
                            db.collection("notifications").add(mapOf(
                                "userId" to patientId,
                                "title" to "💊 New Prescription Issued",
                                "message" to "Dr. ${auth.currentUser?.displayName ?: "Doctor"} has issued a new prescription for you. Date: $today",
                                "read" to false,
                                "createdAt" to System.currentTimeMillis()
                            ))

                            isSaving = false
                            Toast.makeText(context, "Prescription saved!", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        .addOnFailureListener {
                            isSaving = false
                            Toast.makeText(context, "Failed to save!", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.Save, contentDescription = "Save", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Prescription", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}