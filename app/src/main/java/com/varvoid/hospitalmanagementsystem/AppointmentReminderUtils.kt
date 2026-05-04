package com.varvoid.hospitalmanagementsystem

import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object AppointmentReminderUtils {

    fun checkAndSendReminders(
        db: FirebaseFirestore,
        patientId: String
    ) {
        if (patientId.isEmpty()) return

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { docs ->
                docs.documents.forEach { doc ->
                    val apptDate = doc.getString("date") ?: return@forEach
                    val apptId = doc.id
                    val serialNumber = doc.getLong("serialNumber")?.toInt() ?: 0
                    val appointmentType = doc.getString("appointmentType") ?: "new"

                    when (apptDate) {
                        tomorrow -> sendReminderIfNotSent(
                            db = db,
                            patientId = patientId,
                            apptId = apptId,
                            title = "📅 Appointment Tomorrow!",
                            message = buildReminderMessage(apptDate, serialNumber, appointmentType, true),
                            reminderType = "tomorrow_$apptId"
                        )
                        today -> sendReminderIfNotSent(
                            db = db,
                            patientId = patientId,
                            apptId = apptId,
                            title = "🏥 Appointment Today!",
                            message = buildReminderMessage(apptDate, serialNumber, appointmentType, false),
                            reminderType = "today_$apptId"
                        )
                    }
                }
            }
    }

    private fun buildReminderMessage(
        apptDate: String,
        serialNumber: Int,
        appointmentType: String,
        isTomorrow: Boolean
    ): String {
        val typeLabel = when (appointmentType) {
            "followup" -> "Follow-up visit"
            "report" -> "Report review"
            "walkin" -> "Walk-in"
            else -> "New visit"
        }
        return if (isTomorrow) {
            "You have a $typeLabel appointment tomorrow ($apptDate). Serial #$serialNumber. Please be on time!"
        } else {
            "You have a $typeLabel appointment TODAY ($apptDate). Serial #$serialNumber. Head to the clinic soon!"
        }
    }

    private fun sendReminderIfNotSent(
        db: FirebaseFirestore,
        patientId: String,
        apptId: String,
        title: String,
        message: String,
        reminderType: String
    ) {
        // ✅ Query by userId only — no composite index needed
        // Check reminderType in code instead
        db.collection("notifications")
            .whereEqualTo("userId", patientId)
            .get()
            .addOnSuccessListener { existing ->
                // ✅ Filter by reminderType in code
                val alreadySent = existing.documents.any { doc ->
                    doc.getString("reminderType") == reminderType
                }
                if (!alreadySent) {
                    db.collection("notifications").add(
                        mapOf(
                            "userId" to patientId,
                            "title" to title,
                            "message" to message,
                            "read" to false,
                            "reminderType" to reminderType,
                            "appointmentId" to apptId,
                            "createdAt" to System.currentTimeMillis()
                        )
                    )
                }
            }
    }
}