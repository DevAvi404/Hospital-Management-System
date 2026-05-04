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

class EditProfileActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                EditProfileScreen(
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
fun EditProfileScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    context: android.content.Context,
    onBack: () -> Unit
) {
    // Personal Info
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var dateOfBirth by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    // Medical
    var allergies by remember { mutableStateOf("None") }
    var hasAllergies by remember { mutableStateOf(false) }
    var chronicDiseases by remember { mutableStateOf("None") }
    var hasChronicDiseases by remember { mutableStateOf(false) }
    var currentMedications by remember { mutableStateOf("None") }
    var hasCurrentMedications by remember { mutableStateOf(false) }

    // Emergency
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var emergencyRelation by remember { mutableStateOf("") }

    // Address
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val userId = auth.currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        if (userId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                name = document.getString("name") ?: ""
                phone = document.getString("phone") ?: ""
                email = document.getString("email") ?: ""
                age = document.getString("age") ?: ""
                gender = document.getString("gender") ?: "Male"
                dateOfBirth = document.getString("dateOfBirth") ?: ""
                bloodGroup = document.getString("bloodGroup") ?: ""
                weight = document.getString("weight") ?: ""
                height = document.getString("height") ?: ""

                val storedAllergies = document.getString("allergies") ?: "None"
                allergies = storedAllergies
                hasAllergies = storedAllergies.isNotEmpty() && storedAllergies != "None"

                val storedChronic = document.getString("chronicDiseases") ?: "None"
                chronicDiseases = storedChronic
                hasChronicDiseases = storedChronic.isNotEmpty() && storedChronic != "None"

                val storedMeds = document.getString("currentMedications") ?: "None"
                currentMedications = storedMeds
                hasCurrentMedications = storedMeds.isNotEmpty() && storedMeds != "None"

                emergencyName = document.getString("emergencyName") ?: ""
                emergencyPhone = document.getString("emergencyPhone") ?: ""
                emergencyRelation = document.getString("emergencyRelation") ?: ""
                city = document.getString("city") ?: ""
                address = document.getString("address") ?: ""

                isLoading = false
            }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = millis
                        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        val month = calendar.get(java.util.Calendar.MONTH) + 1
                        val year = calendar.get(java.util.Calendar.YEAR)
                        dateOfBirth = "%02d-%02d-%04d".format(day, month, year)
                        val today = java.util.Calendar.getInstance()
                        var calculatedAge = today.get(java.util.Calendar.YEAR) - year
                        if (today.get(java.util.Calendar.MONTH) + 1 < month ||
                            (today.get(java.util.Calendar.MONTH) + 1 == month &&
                                    today.get(java.util.Calendar.DAY_OF_MONTH) < day)
                        ) calculatedAge--
                        age = calculatedAge.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 👤 Personal Information
                ProfileCard(title = "👤 Personal Information") {

                    ProfileTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name"
                    )

                    // ✅ Registered email — read only
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = "Email",
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Registered Email",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = email,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ✅ Phone number — for emergency contact
                    ProfileTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number (emergency contact)"
                    )

                    // DOB picker
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = {},
                        label = { Text("Date of Birth") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = Color(0xFF1565C0)
                                )
                            }
                        }
                    )

                    // Age (auto calculated)
                    OutlinedTextField(
                        value = if (age.isEmpty()) "" else "$age years old",
                        onValueChange = {},
                        label = { Text("Age (auto calculated)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Gray,
                            disabledBorderColor = Color.LightGray,
                            disabledLabelColor = Color.Gray
                        )
                    )

                    ProfileTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = "Blood Group (A+, B-, O+...)"
                    )

                    Text("Gender", fontSize = 14.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { option ->
                            FilterChip(
                                selected = gender == option,
                                onClick = { gender = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                // 🏥 Medical Information
                ProfileCard(title = "🏥 Medical Information") {
                    ProfileTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = "Weight (kg)"
                    )
                    ProfileTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = "Height (cm)"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Known Allergies", fontSize = 14.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = hasAllergies,
                            onClick = { hasAllergies = true },
                            label = { Text("Yes") }
                        )
                        FilterChip(
                            selected = !hasAllergies,
                            onClick = { hasAllergies = false; allergies = "None" },
                            label = { Text("No") }
                        )
                    }
                    if (hasAllergies) {
                        ProfileTextField(
                            value = if (allergies == "None") "" else allergies,
                            onValueChange = { allergies = it },
                            label = "Describe your allergies"
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Chronic Diseases", fontSize = 14.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = hasChronicDiseases,
                            onClick = { hasChronicDiseases = true },
                            label = { Text("Yes") }
                        )
                        FilterChip(
                            selected = !hasChronicDiseases,
                            onClick = { hasChronicDiseases = false; chronicDiseases = "None" },
                            label = { Text("No") }
                        )
                    }
                    if (hasChronicDiseases) {
                        ProfileTextField(
                            value = if (chronicDiseases == "None") "" else chronicDiseases,
                            onValueChange = { chronicDiseases = it },
                            label = "Describe your chronic diseases"
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Current Medications", fontSize = 14.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = hasCurrentMedications,
                            onClick = { hasCurrentMedications = true },
                            label = { Text("Yes") }
                        )
                        FilterChip(
                            selected = !hasCurrentMedications,
                            onClick = { hasCurrentMedications = false; currentMedications = "None" },
                            label = { Text("No") }
                        )
                    }
                    if (hasCurrentMedications) {
                        ProfileTextField(
                            value = if (currentMedications == "None") "" else currentMedications,
                            onValueChange = { currentMedications = it },
                            label = "List your current medications"
                        )
                    }
                }

                // 🚨 Emergency Contact
                ProfileCard(title = "🚨 Emergency Contact") {
                    ProfileTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        label = "Contact Name"
                    )
                    ProfileTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = "Contact Phone"
                    )
                    ProfileTextField(
                        value = emergencyRelation,
                        onValueChange = { emergencyRelation = it },
                        label = "Relationship (e.g. Father)"
                    )
                }

                // 📍 Address
                ProfileCard(title = "📍 Address") {
                    ProfileTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = "City"
                    )
                    ProfileTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Full Address"
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true

                        val updates = hashMapOf<String, Any>(
                            "name" to name,
                            "phone" to phone,
                            "age" to age,
                            "gender" to gender,
                            "dateOfBirth" to dateOfBirth,
                            "bloodGroup" to bloodGroup,
                            "weight" to weight,
                            "height" to height,
                            "allergies" to allergies,
                            "chronicDiseases" to chronicDiseases,
                            "currentMedications" to currentMedications,
                            "emergencyName" to emergencyName,
                            "emergencyPhone" to emergencyPhone,
                            "emergencyRelation" to emergencyRelation,
                            "city" to city,
                            "address" to address
                        )

                        db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                isSaving = false
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Failed to update!", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Save Changes", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            content()
        }
    }
}

@Composable
fun ProfileTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}