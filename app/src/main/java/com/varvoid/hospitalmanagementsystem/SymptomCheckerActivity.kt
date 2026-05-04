package com.varvoid.hospitalmanagementsystem

import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "SymptomChecker"

data class SymptomResult(
    val possibleConditions: String = "",
    val severity: String = "",
    val recommendedSpecialist: String = "",
    val homeRemedies: String = "",
    val urgentWarning: String = "",
    val disclaimer: String = ""
)

class SymptomCheckerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SymptomCheckerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(onBack: () -> Unit) {
    var symptoms by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SymptomResult?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // API key — replace with a fresh one from aistudio.google.com if quota runs out
    val geminiApiKey = BuildConfig.GEMINI_API_KEY

    // Log API key status for debugging
    LaunchedEffect(Unit) {
        Log.d(TAG, "API Key loaded: ${geminiApiKey.take(10)}...${if(geminiApiKey.length > 10) geminiApiKey.takeLast(4) else ""}")
        Log.d(TAG, "API Key empty: ${geminiApiKey.isEmpty()}")
        Log.d(TAG, "API Key is 'GEMINI_API_KEY_NOT_SET': ${geminiApiKey == "GEMINI_API_KEY_NOT_SET"}")
    }

    // Model: gemini-2.0-flash on v1beta is the confirmed working endpoint
    val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$geminiApiKey"

    fun analyzeSymptoms() {
        if (symptoms.isBlank()) return
        if (geminiApiKey.isEmpty() || geminiApiKey == "GEMINI_API_KEY_NOT_SET") {
            errorMessage = "❌ Error: API key is not configured. Please set GEMINI_API_KEY in gradle.properties"
            return
        }
        
        isLoading = true
        errorMessage = ""
        result = null

        scope.launch {
            try {
                Log.d(TAG, "Starting symptom analysis with symptoms: $symptoms")
                
                val prompt = """
                    You are a medical assistant. A patient has the following symptoms:
                    Symptoms: $symptoms
                    Age: ${age.ifEmpty { "Not specified" }}
                    Gender: $gender
                    
                    Please provide a structured analysis in exactly this JSON format:
                    {
                        "possibleConditions": "List possible conditions separated by commas",
                        "severity": "Mild or Moderate or Severe",
                        "recommendedSpecialist": "Type of doctor to see",
                        "homeRemedies": "Simple home remedies if applicable",
                        "urgentWarning": "Warning signs that need immediate medical attention",
                        "disclaimer": "Brief medical disclaimer"
                    }
                    Respond ONLY with the JSON, no other text.
                """.trimIndent()

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                val responseText = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Making API request to: ${apiUrl.take(60)}...")
                    val connection = URL(apiUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 30_000
                    connection.readTimeout = 30_000
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(requestBody.toString())
                        writer.flush()
                    }

                    val code = connection.responseCode
                    Log.d(TAG, "API Response code: $code")
                    
                    if (code == 200) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        val errBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                        Log.e(TAG, "API Error $code: $errBody")
                        throw Exception("API Error $code: $errBody")
                    }
                }

                Log.d(TAG, "Response received, parsing JSON...")
                
                // Parse Gemini response
                val text = JSONObject(responseText)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Strip markdown code fences (e.g. ```json\n...\n```)
                val cleanJson = text.trim()
                    .replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("```\\s*$"), "")
                    .trim()

                val parsed = JSONObject(cleanJson)
                result = SymptomResult(
                    possibleConditions = parsed.optString("possibleConditions", ""),
                    severity = parsed.optString("severity", ""),
                    recommendedSpecialist = parsed.optString("recommendedSpecialist", ""),
                    homeRemedies = parsed.optString("homeRemedies", ""),
                    urgentWarning = parsed.optString("urgentWarning", ""),
                    disclaimer = parsed.optString("disclaimer", "")
                )
                Log.d(TAG, "Analysis complete!")
                isLoading = false

            } catch (e: Exception) {
                isLoading = false
                val msg = e.message ?: "Unknown error"
                Log.e(TAG, "Error during analysis: $msg", e)
                
                if (msg.contains("429")) {
                    result = buildDemoResult(symptoms)
                    errorMessage = "⚠️ API quota exceeded — showing demo result based on your symptoms. Quota resets in 24 hours."
                } else if (msg.contains("401") || msg.contains("PERMISSION_DENIED")) {
                    errorMessage = "❌ Authentication failed: Invalid or expired API key. Please update GEMINI_API_KEY."
                    result = buildDemoResult(symptoms)
                } else if (msg.contains("timeout") || msg.contains("TimeoutException")) {
                    errorMessage = "⏱️ Request timeout. Please check your internet connection and try again."
                } else {
                    errorMessage = "Error: $msg"
                    // Show demo result as fallback
                    result = buildDemoResult(symptoms)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Symptom Checker", fontWeight = FontWeight.Bold) },
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
            // Info banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Powered by Google Gemini AI. Not a substitute for professional medical advice.",
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0)
                    )
                }
            }

            // Input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Enter Your Symptoms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))

                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        label = { Text("Describe your symptoms...") },
                        placeholder = { Text("e.g. fever, headache, body pain for 2 days") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
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

                    Button(
                        onClick = { analyzeSymptoms() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = symptoms.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing with AI...")
                        } else {
                            Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Symptoms")
                        }
                    }
                }
            }

            // Error card
            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage, fontSize = 13.sp, color = Color.Red)
                    }
                }
            }

            // Results
            result?.let { res ->
                val severityColor = when (res.severity.lowercase()) {
                    "mild" -> Color(0xFF4CAF50)
                    "moderate" -> Color(0xFFF57F17)
                    "severe" -> Color.Red
                    else -> Color.Gray
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = severityColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Severity Level", fontSize = 14.sp, color = Color.White)
                        Text(res.severity, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                ResultCard(title = "🔍 Possible Conditions", content = res.possibleConditions, containerColor = Color(0xFFF3E5F5))
                ResultCard(title = "👨‍⚕️ Recommended Specialist", content = res.recommendedSpecialist, containerColor = Color(0xFFE3F2FD))

                if (res.homeRemedies.isNotEmpty()) {
                    ResultCard(title = "🏠 Home Remedies", content = res.homeRemedies, containerColor = Color(0xFFE8F5E9))
                }

                if (res.urgentWarning.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🚨 Seek Immediate Help If:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(res.urgentWarning, fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(res.disclaimer, fontSize = 11.sp, color = Color(0xFFF57F17))
                    }
                }
            }
        }
    }
}

fun buildDemoResult(symptoms: String): SymptomResult {
    val s = symptoms.lowercase()
    val disclaimer = "⚠️ DEMO RESULT — Gemini API daily quota exhausted. Quota resets in 24 hours. This is NOT a real medical diagnosis."

    return when {
        s.contains("chest pain") || s.contains("chest") && (s.contains("tight") || s.contains("pressure")) ->
            SymptomResult(
                possibleConditions = "Angina, GERD (Acid Reflux), Costochondritis, Anxiety, or Cardiac issues",
                severity = "Severe",
                recommendedSpecialist = "Cardiologist or Emergency Room immediately",
                homeRemedies = "Sit upright, rest, avoid exertion. Do NOT ignore chest pain.",
                urgentWarning = "Call emergency services (999/911) immediately if: radiating pain to arm/jaw, sweating, shortness of breath, or nausea.",
                disclaimer = disclaimer
            )
        s.contains("back pain") || s.contains("backache") || s.contains("lower back") ->
            SymptomResult(
                possibleConditions = "Muscle Strain, Herniated Disc, Sciatica, Kidney Stones, or Poor Posture",
                severity = "Moderate",
                recommendedSpecialist = "Orthopedic Specialist or Physiotherapist",
                homeRemedies = "Apply heat/ice pack, rest, gentle stretching. Avoid heavy lifting.",
                urgentWarning = "Seek immediate help if: numbness in legs, loss of bladder/bowel control, or pain after a fall/injury.",
                disclaimer = disclaimer
            )
        s.contains("headache") || s.contains("migraine") || s.contains("head pain") ->
            SymptomResult(
                possibleConditions = "Tension Headache, Migraine, Dehydration, Hypertension, or Sinusitis",
                severity = "Mild",
                recommendedSpecialist = "Neurologist or General Physician",
                homeRemedies = "Rest in a dark quiet room, drink water, apply cold compress to forehead, avoid screens.",
                urgentWarning = "Seek immediate help if: sudden severe 'thunderclap' headache, headache with fever and stiff neck, vision changes, or after head injury.",
                disclaimer = disclaimer
            )
        s.contains("cough") || s.contains("cold") || s.contains("sore throat") || s.contains("throat") ->
            SymptomResult(
                possibleConditions = "Common Cold, Pharyngitis, Bronchitis, Allergic Rhinitis, or COVID-19",
                severity = "Mild",
                recommendedSpecialist = "General Physician or ENT Specialist",
                homeRemedies = "Warm water with honey and lemon, steam inhalation, rest, stay hydrated, gargle with salt water.",
                urgentWarning = "Seek help if: difficulty breathing, coughing blood, high fever above 103°F, or symptoms lasting over 2 weeks.",
                disclaimer = disclaimer
            )
        s.contains("stomach") || s.contains("abdominal") || s.contains("belly") || s.contains("nausea") || s.contains("vomit") ->
            SymptomResult(
                possibleConditions = "Gastritis, Food Poisoning, IBS, Appendicitis, or GERD",
                severity = "Moderate",
                recommendedSpecialist = "Gastroenterologist or General Physician",
                homeRemedies = "Eat bland foods (BRAT diet), stay hydrated with ORS, avoid spicy/oily food, rest.",
                urgentWarning = "Seek immediate help if: severe sharp pain in lower right abdomen, blood in vomit/stool, or pain lasting over 6 hours.",
                disclaimer = disclaimer
            )
        s.contains("diarrhea") || s.contains("loose stool") ->
            SymptomResult(
                possibleConditions = "Gastroenteritis, Food Poisoning, IBS, or Bacterial Infection",
                severity = "Mild",
                recommendedSpecialist = "General Physician or Gastroenterologist",
                homeRemedies = "ORS (Oral Rehydration Solution), avoid dairy and fatty foods, eat bananas/rice/toast, rest.",
                urgentWarning = "Seek help if: blood in stool, severe dehydration (dry mouth, no urination), or diarrhea lasting more than 3 days.",
                disclaimer = disclaimer
            )
        s.contains("breathing") || s.contains("shortness of breath") || s.contains("breathless") || s.contains("asthma") ->
            SymptomResult(
                possibleConditions = "Asthma, Bronchitis, Pneumonia, Anxiety Attack, or Cardiac issues",
                severity = "Severe",
                recommendedSpecialist = "Pulmonologist or Emergency Room",
                homeRemedies = "Sit upright, use rescue inhaler if prescribed, stay calm, breathe slowly.",
                urgentWarning = "Call emergency services immediately if breathing is very difficult, lips/fingertips turn blue, or you cannot speak full sentences.",
                disclaimer = disclaimer
            )
        s.contains("joint pain") || s.contains("knee") || s.contains("shoulder") || s.contains("arthritis") ->
            SymptomResult(
                possibleConditions = "Arthritis, Gout, Bursitis, Tendinitis, or Sports Injury",
                severity = "Moderate",
                recommendedSpecialist = "Orthopedic Specialist or Rheumatologist",
                homeRemedies = "RICE method (Rest, Ice, Compression, Elevation), anti-inflammatory diet, gentle exercise.",
                urgentWarning = "Seek help if: joint is deformed, severely swollen/hot/red, or you cannot bear weight on it.",
                disclaimer = disclaimer
            )
        s.contains("dizz") || s.contains("vertigo") || s.contains("lightheaded") ->
            SymptomResult(
                possibleConditions = "Vertigo, Dehydration, Anemia, Low Blood Pressure, or Inner Ear Infection",
                severity = "Mild",
                recommendedSpecialist = "Neurologist or ENT Specialist",
                homeRemedies = "Sit or lie down immediately, drink water, avoid sudden head movements, get adequate sleep.",
                urgentWarning = "Seek immediate help if: dizziness with chest pain, fainting, severe headache, or sudden vision/speech problems.",
                disclaimer = disclaimer
            )
        s.contains("skin") || s.contains("rash") || s.contains("itch") || s.contains("allerg") ->
            SymptomResult(
                possibleConditions = "Allergic Reaction, Eczema, Contact Dermatitis, Urticaria, or Psoriasis",
                severity = "Mild",
                recommendedSpecialist = "Dermatologist or Allergist",
                homeRemedies = "Avoid known allergens, apply calamine lotion, take antihistamines, keep skin moisturized.",
                urgentWarning = "Seek immediate help if: rash with high fever, difficulty breathing, or widespread blistering.",
                disclaimer = disclaimer
            )
        s.contains("eye") || s.contains("vision") || s.contains("blurr") ->
            SymptomResult(
                possibleConditions = "Conjunctivitis, Eye Strain, Myopia, Glaucoma, or Diabetic Retinopathy",
                severity = "Moderate",
                recommendedSpecialist = "Ophthalmologist (Eye Specialist)",
                homeRemedies = "Rest your eyes every 20 minutes (20-20-20 rule), wash eyes with clean water, avoid rubbing.",
                urgentWarning = "Seek immediate help if: sudden vision loss, severe eye pain, or flashes of light.",
                disclaimer = disclaimer
            )
        s.contains("fever") || s.contains("temperature") ->
            SymptomResult(
                possibleConditions = "Viral Fever, Common Cold, Influenza, Dengue Fever, or Malaria",
                severity = "Mild",
                recommendedSpecialist = "General Physician",
                homeRemedies = "Rest, drink plenty of fluids, take paracetamol, use a damp cloth on forehead to cool down.",
                urgentWarning = "Seek immediate help if: temperature exceeds 103°F (39.4°C), fever with stiff neck/rash, or fever lasting more than 3 days.",
                disclaimer = disclaimer
            )
        else ->
            SymptomResult(
                possibleConditions = "Unable to determine without professional examination",
                severity = "Moderate",
                recommendedSpecialist = "General Physician for initial evaluation",
                homeRemedies = "Rest, stay hydrated, monitor symptoms. Keep a record of when symptoms started and their severity.",
                urgentWarning = "Seek immediate help if symptoms are severe, worsening rapidly, or preventing normal daily activities.",
                disclaimer = disclaimer
            )
    }
}

@Composable
fun ResultCard(title: String, content: String, containerColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(modifier = Modifier.height(8.dp))
            Text(content, fontSize = 13.sp, color = Color.DarkGray)
        }
    }
}