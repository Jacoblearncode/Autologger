package com.nibm.autocare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.nibm.autocare.model.FuelLog
import com.nibm.autocare.model.ServiceRecord
import com.nibm.autocare.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfGenerator(private val context: Context) {

    suspend fun generateServiceRecordPdf(
        vehicleRegistration: String,
        serviceRecords: List<ServiceRecord>
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ServiceRecords_${vehicleRegistration}_$timeStamp.pdf"

            // Use app-specific storage (works on all Android versions)
            val downloadsDir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            } else {
                context.filesDir
            }

            val file = File(downloadsDir, fileName)

            // Initialize PDF writer and document
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Add title
            document.add(
                Paragraph("Service Records for $vehicleRegistration")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(18f)
            )

            document.add(Paragraph("\n"))

            // Add each service record
            serviceRecords.forEach { record ->
                addServiceRecord(document, record)
                document.add(Paragraph("\n"))
                document.add(
                    Paragraph("----------------------------------------")
                        .setTextAlignment(TextAlignment.CENTER)
                )
                document.add(Paragraph("\n"))
            }

            document.close()

            Pair(file.absolutePath, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, false)
        }
    }

    private fun addServiceRecord(document: Document, record: ServiceRecord) {
        // Add date
        document.add(
            Paragraph("Date: ${record.date}")
                .setBold()
                .setFontSize(14f)
        )

        // Add odometer reading
        document.add(
            Paragraph("Odometer: ${record.odometerReading} km")
                .setFontSize(12f)
        )

        // Add service cost
        document.add(
            Paragraph("Cost: MYR ${record.serviceCost}")
                .setFontSize(12f)
        )

        // Add service type if available
        record.serviceType?.let {
            document.add(
                Paragraph("Service Type: $it")
                    .setFontSize(12f)
            )
        }

        // Add checked items if available
        record.checkedItems?.takeIf { it.isNotEmpty() }?.let { items ->
            val checkedItems = Paragraph("Services Performed:")
                .setFontSize(12f)
            items.forEach { item ->
                checkedItems.add(
                    Paragraph("• $item")
                        .setMarginLeft(10f)
                )
            }
            document.add(checkedItems)
        }

        // Add notes if available
        record.notes?.let {
            document.add(
                Paragraph("Notes: $it")
                    .setFontSize(12f)
            )
        }

        // Add images if available
        record.photoUrls?.takeIf { it.isNotEmpty() }?.let { urls ->
            document.add(
                Paragraph("Service Photos:")
                    .setFontSize(12f)
            )

            for (url in urls) {
                try {
                    // Load image from URL
                    val imageStream = URL(url).openStream()
                    val bitmap = BitmapFactory.decodeStream(imageStream)

                    // Convert bitmap to byte array
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val byteArray = stream.toByteArray()

                    // Add image to PDF
                    val imageData = ImageDataFactory.create(byteArray)
                    val image = Image(imageData)
                        .setAutoScale(true)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)

                    document.add(image)
                    document.add(Paragraph("\n"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    document.add(
                        Paragraph("Could not load image: $url")
                            .setFontSize(10f)
                            .setFontColor(ColorConstants.RED)
                    )
                }
            }
        }
    }

    suspend fun generateFuelLogPdf(
        vehicleFilter: String,
        fuelLogs: List<FuelLog>
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = vehicleFilter.replace(" ", "_")
            val fileName = "FuelLog_${safeName}_$timeStamp.pdf"

            val downloadsDir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            } else {
                context.filesDir
            }

            val file = File(downloadsDir, fileName)
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            document.add(
                Paragraph("Fuel Log — $vehicleFilter")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(18f)
            )
            document.add(Paragraph("\n"))

            var totalCost = 0.0
            var totalLiters = 0.0

            fuelLogs.forEach { log ->
                document.add(Paragraph("Date: ${log.date}  |  ${log.registrationNumber}").setBold().setFontSize(13f))
                document.add(Paragraph("Odometer: ${log.odometer} km  |  Fuel: ${log.liters} L  |  Type: ${log.fuelType}").setFontSize(11f))
                document.add(Paragraph("Price: MYR ${log.pricePerLiter}/L  |  Total: MYR ${log.totalCost}").setFontSize(11f))
                if (log.efficiency.isNotBlank()) document.add(Paragraph("Efficiency: ${log.efficiency}").setFontSize(11f))
                if (log.notes.isNotBlank()) document.add(Paragraph("Notes: ${log.notes}").setFontSize(11f).setItalic())
                document.add(Paragraph("─────────────────────────────────────────").setFontSize(9f).setFontColor(ColorConstants.GRAY))

                totalCost += log.totalCost.toDoubleOrNull() ?: 0.0
                totalLiters += log.liters.toDoubleOrNull() ?: 0.0
            }

            document.add(Paragraph("\n"))
            document.add(
                Paragraph("Total: ${fuelLogs.size} fill-ups  |  ${String.format("%.1f", totalLiters)} L  |  MYR ${String.format("%,.0f", totalCost)}")
                    .setBold().setFontSize(13f).setTextAlignment(TextAlignment.CENTER)
            )

            document.close()
            Pair(file.absolutePath, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, false)
        }
    }

    suspend fun generateServiceRecordCsv(
        vehicleRegistration: String,
        serviceRecords: List<ServiceRecord>
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ServiceRecords_${vehicleRegistration}_$timeStamp.csv"
            val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            else context.filesDir
            val file = File(dir, fileName)

            file.bufferedWriter().use { writer ->
                writer.write("Date,Odometer (km),Cost (MYR),Service Type,Services Performed,Notes\n")
                serviceRecords.forEach { record ->
                    val services = record.checkedItems?.joinToString("; ")?.csvEscape() ?: ""
                    writer.write(
                        "${record.date.csvEscape()}," +
                        "${record.odometerReading.csvEscape()}," +
                        "${record.serviceCost.csvEscape()}," +
                        "${(record.serviceType ?: "").csvEscape()}," +
                        "$services," +
                        "${(record.notes ?: "").csvEscape()}\n"
                    )
                }
            }
            Pair(file.absolutePath, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, false)
        }
    }

    suspend fun generateFuelLogCsv(
        vehicleFilter: String,
        fuelLogs: List<FuelLog>
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = vehicleFilter.replace(" ", "_")
            val fileName = "FuelLog_${safeName}_$timeStamp.csv"
            val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            else context.filesDir
            val file = File(dir, fileName)

            file.bufferedWriter().use { writer ->
                writer.write("Date,Vehicle,Odometer (km),Liters,Fuel Type,Price/L (MYR),Total Cost (MYR),Efficiency,Notes\n")
                fuelLogs.forEach { log ->
                    writer.write(
                        "${log.date.csvEscape()}," +
                        "${log.registrationNumber.csvEscape()}," +
                        "${log.odometer.csvEscape()}," +
                        "${log.liters.csvEscape()}," +
                        "${log.fuelType.csvEscape()}," +
                        "${log.pricePerLiter.csvEscape()}," +
                        "${log.totalCost.csvEscape()}," +
                        "${log.efficiency.csvEscape()}," +
                        "${log.notes.csvEscape()}\n"
                    )
                }
            }
            Pair(file.absolutePath, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, false)
        }
    }

    suspend fun generateTripLogCsv(
        vehicleRegistration: String,
        trips: List<Trip>
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "TripLog_${vehicleRegistration}_$timeStamp.csv"
            val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            else context.filesDir
            val file = File(dir, fileName)

            file.bufferedWriter().use { writer ->
                writer.write("Date,Purpose,Start Odometer (km),End Odometer (km),Distance (km),Notes\n")
                trips.forEach { trip ->
                    writer.write(
                        "${trip.date.csvEscape()}," +
                        "${trip.purpose.csvEscape()}," +
                        "${trip.startOdometer.csvEscape()}," +
                        "${trip.endOdometer.csvEscape()}," +
                        "${String.format("%.1f", trip.distance).csvEscape()}," +
                        "${trip.notes.csvEscape()}\n"
                    )
                }
            }
            Pair(file.absolutePath, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, false)
        }
    }

    // P2 — Full vehicle report PDF
    suspend fun generateFullReport(
        vehicleRegistration: String,
        brand: String,
        model: String,
        year: String,
        serviceRecords: List<ServiceRecord>,
        currency: String,
        fuelTotal: Double,
        fuelCount: Int
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "FullReport_${vehicleRegistration}_$timeStamp.pdf"
            val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            else context.filesDir
            val file = File(dir, fileName)

            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Title
            document.add(Paragraph("Vehicle Report")
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(22f))
            document.add(Paragraph(vehicleRegistration)
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18f)
                .setFontColor(com.itextpdf.kernel.colors.DeviceRgb(92, 171, 0)))
            document.add(Paragraph("Generated ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(10f)
                .setFontColor(ColorConstants.GRAY))
            document.add(Paragraph("\n"))

            // Vehicle details
            document.add(Paragraph("VEHICLE DETAILS").setBold().setFontSize(13f)
                .setFontColor(com.itextpdf.kernel.colors.DeviceRgb(92, 171, 0)))
            document.add(Paragraph("Brand / Model:  $brand $model").setFontSize(11f))
            document.add(Paragraph("Year:  $year").setFontSize(11f))
            document.add(Paragraph("\n"))

            // Financial summary
            val svcTotal = serviceRecords.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
            document.add(Paragraph("FINANCIAL SUMMARY").setBold().setFontSize(13f)
                .setFontColor(com.itextpdf.kernel.colors.DeviceRgb(92, 171, 0)))
            document.add(Paragraph("Total service cost:   $currency ${"%,.0f".format(svcTotal)}").setFontSize(11f))
            document.add(Paragraph("Total fuel cost:   $currency ${"%,.0f".format(fuelTotal)}").setFontSize(11f))
            document.add(Paragraph("Total ($fuelCount fuel fill-ups + ${serviceRecords.size} services):  $currency ${"%,.0f".format(svcTotal + fuelTotal)}")
                .setFontSize(11f).setBold())
            document.add(Paragraph("\n"))

            // Service records
            document.add(Paragraph("SERVICE RECORDS (${serviceRecords.size})").setBold().setFontSize(13f)
                .setFontColor(com.itextpdf.kernel.colors.DeviceRgb(92, 171, 0)))
            serviceRecords.forEach { record ->
                document.add(Paragraph("${record.date}  |  ${record.odometerReading} km  |  $currency ${record.serviceCost}")
                    .setBold().setFontSize(11f))
                record.serviceType?.let { document.add(Paragraph("  Type: $it").setFontSize(10f)) }
                record.checkedItems?.takeIf { it.isNotEmpty() }?.let {
                    document.add(Paragraph("  Services: ${it.joinToString(", ")}").setFontSize(10f))
                }
                record.notes?.let { document.add(Paragraph("  Notes: $it").setFontSize(10f).setItalic()) }
                document.add(Paragraph("─────────────────────────────────────────").setFontSize(9f).setFontColor(ColorConstants.GRAY))
            }

            document.close()
            Pair(file.absolutePath, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, false)
        }
    }

    private fun String.csvEscape(): String {
        return if (contains(',') || contains('"') || contains('\n')) {
            "\"${replace("\"", "\"\"")}\""
        } else this
    }
}