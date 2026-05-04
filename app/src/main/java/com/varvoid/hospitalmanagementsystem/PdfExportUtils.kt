package com.varvoid.hospitalmanagementsystem

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtils {

    private val paintTitle = Paint().apply {
        color = Color.parseColor("#1565C0")
        textSize = 22f
        isFakeBoldText = true
    }

    private val paintHeader = Paint().apply {
        color = Color.parseColor("#1565C0")
        textSize = 16f
        isFakeBoldText = true
    }

    private val paintNormal = Paint().apply {
        color = Color.BLACK
        textSize = 13f
    }

    private val paintSmall = Paint().apply {
        color = Color.GRAY
        textSize = 11f
    }

    private val paintBold = Paint().apply {
        color = Color.BLACK
        textSize = 13f
        isFakeBoldText = true
    }

    private val paintLine = Paint().apply {
        color = Color.parseColor("#DDDDDD")
        strokeWidth = 1f
    }

    private val paintGreen = Paint().apply {
        color = Color.parseColor("#4CAF50")
        textSize = 13f
        isFakeBoldText = true
    }

    private val paintRed = Paint().apply {
        color = Color.RED
        textSize = 13f
        isFakeBoldText = true
    }

    private val paintOrange = Paint().apply {
        color = Color.parseColor("#F57F17")
        textSize = 13f
        isFakeBoldText = true
    }

    // ✅ Export Prescription as PDF
    fun exportPrescriptionPdf(
        context: Context,
        patientName: String,
        doctorName: String,
        date: String,
        diagnosis: String,
        medicines: List<Map<String, String>>,
        investigations: List<String>,
        notes: String,
        nextVisit: String,
        vitals: Map<String, String> = emptyMap()
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var y = 60f
        val leftMargin = 50f
        val rightMargin = 545f

        // Header
        canvas.drawText("🏥 Hospital Management System", leftMargin, y, paintTitle)
        y += 25f
        canvas.drawText("PRESCRIPTION", leftMargin, y, paintHeader)
        y += 5f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 20f

        // Patient & Doctor Info
        canvas.drawText("Patient: $patientName", leftMargin, y, paintBold)
        canvas.drawText("Date: $date", 380f, y, paintNormal)
        y += 20f
        canvas.drawText("Doctor: Dr. $doctorName", leftMargin, y, paintBold)
        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 20f

        // Vitals
        if (vitals.isNotEmpty() && vitals.values.any { it.isNotEmpty() }) {
            canvas.drawText("CLINICAL VITALS", leftMargin, y, paintHeader)
            y += 18f
            val vitalsList = listOf(
                "Temperature" to vitals["temperature"],
                "Blood Pressure" to vitals["bloodPressure"],
                "Pulse" to vitals["pulse"],
                "SpO2" to vitals["oxygenSaturation"],
                "Weight" to vitals["weight"],
                "Respiratory Rate" to vitals["respiratoryRate"]
            ).filter { !it.second.isNullOrEmpty() }

            vitalsList.chunked(2).forEach { row ->
                var x = leftMargin
                row.forEach { (label, value) ->
                    canvas.drawText("$label: $value", x, y, paintNormal)
                    x += 250f
                }
                y += 18f
            }
            if (vitals["clinicalNotes"]?.isNotEmpty() == true) {
                canvas.drawText("Notes: ${vitals["clinicalNotes"]}", leftMargin, y, paintSmall)
                y += 18f
            }
            y += 5f
            canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
            y += 20f
        }

        // Diagnosis
        canvas.drawText("DIAGNOSIS", leftMargin, y, paintHeader)
        y += 18f
        canvas.drawText(diagnosis, leftMargin, y, paintNormal)
        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 20f

        // Medicines (Rx)
        canvas.drawText("MEDICINES (Rx)", leftMargin, y, paintHeader)
        y += 18f
        medicines.forEachIndexed { index, medicine ->
            canvas.drawText("${index + 1}. ${medicine["name"] ?: ""}", leftMargin, y, paintBold)
            y += 16f
            if (!medicine["dosage"].isNullOrEmpty()) {
                canvas.drawText("   Dosage: ${medicine["dosage"]}", leftMargin, y, paintNormal)
                y += 14f
            }
            if (!medicine["duration"].isNullOrEmpty()) {
                canvas.drawText("   Duration: ${medicine["duration"]}", leftMargin, y, paintNormal)
                y += 14f
            }
            if (!medicine["instructions"].isNullOrEmpty()) {
                canvas.drawText("   Instructions: ${medicine["instructions"]}", leftMargin, y, paintSmall)
                y += 14f
            }
            y += 4f
        }

        if (investigations.isNotEmpty() && investigations.any { it.isNotEmpty() }) {
            y += 5f
            canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
            y += 20f
            canvas.drawText("INVESTIGATIONS / TESTS", leftMargin, y, paintHeader)
            y += 18f
            investigations.filter { it.isNotEmpty() }.forEachIndexed { index, test ->
                canvas.drawText("${index + 1}. $test", leftMargin, y, paintNormal)
                y += 16f
            }
        }

        if (notes.isNotEmpty()) {
            y += 5f
            canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
            y += 20f
            canvas.drawText("DOCTOR'S NOTES", leftMargin, y, paintHeader)
            y += 18f
            canvas.drawText(notes, leftMargin, y, paintNormal)
            y += 18f
        }

        if (nextVisit.isNotEmpty()) {
            y += 5f
            canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
            y += 20f
            canvas.drawText("Next Visit: $nextVisit", leftMargin, y, paintGreen)
            y += 18f
        }

        // Footer
        y = 800f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 14f
        canvas.drawText("Generated by Hospital Management System • $date", leftMargin, y, paintSmall)
        canvas.drawText("This prescription is system generated", 350f, y, paintSmall)

        document.finishPage(page)

        saveAndShare(context, document, "Prescription_${patientName}_$date.pdf")
    }

    // ✅ Export Appointment History as PDF
    fun exportAppointmentsPdf(
        context: Context,
        patientName: String,
        appointments: List<Map<String, String>>
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var y = 60f
        val leftMargin = 50f
        val rightMargin = 545f

        // Header
        canvas.drawText("🏥 Hospital Management System", leftMargin, y, paintTitle)
        y += 25f
        canvas.drawText("APPOINTMENT HISTORY", leftMargin, y, paintHeader)
        y += 5f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 20f

        canvas.drawText("Patient: $patientName", leftMargin, y, paintBold)
        canvas.drawText("Total: ${appointments.size}", 430f, y, paintBold)
        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 20f

        // Table headers
        val paintTableHeader = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText("Date", leftMargin, y, paintTableHeader)
        canvas.drawText("Doctor", 130f, y, paintTableHeader)
        canvas.drawText("Type", 300f, y, paintTableHeader)
        canvas.drawText("Status", 390f, y, paintTableHeader)
        canvas.drawText("Fee", 490f, y, paintTableHeader)
        y += 5f
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 15f

        appointments.forEach { appt ->
            if (y > 800f) return@forEach // Stop if page full

            val statusPaint = when (appt["status"]) {
                "completed" -> paintGreen
                "cancelled" -> paintRed
                else -> paintOrange
            }

            canvas.drawText(appt["date"] ?: "", leftMargin, y, paintNormal)
            canvas.drawText("Dr. ${appt["doctorName"] ?: ""}", 130f, y, paintNormal)
            canvas.drawText(
                when (appt["appointmentType"]) {
                    "followup" -> "Follow-up"
                    "report" -> "Report"
                    "walkin" -> "Walk-in"
                    else -> "New"
                },
                300f, y, paintNormal
            )
            canvas.drawText(
                (appt["status"] ?: "").replaceFirstChar { it.uppercase() },
                390f, y, statusPaint
            )
            canvas.drawText(
                if (appt["fee"] == "0") "FREE" else "BDT ${appt["fee"]}",
                490f, y, paintNormal
            )
            y += 18f
            canvas.drawLine(leftMargin, y - 4f, rightMargin, y - 4f, paintLine)
        }

        // Footer
        y = 800f
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += 14f
        canvas.drawText("Generated on $today by Hospital Management System", leftMargin, y, paintSmall)

        document.finishPage(page)
        saveAndShare(context, document, "Appointments_${patientName}.pdf")
    }

    private fun saveAndShare(context: Context, document: PdfDocument, fileName: String) {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            Toast.makeText(context, "PDF saved! Opening...", Toast.LENGTH_SHORT).show()

            // Share PDF
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Open PDF with"))

        } catch (e: Exception) {
            document.close()
            Toast.makeText(context, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}