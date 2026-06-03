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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfGenerator(private val context: Context) {

    suspend fun generateServiceRecordPdf(
        vehicleRegistration: String,
        serviceRecords: List<ServiceRecordActivity.ServiceRecord>
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

    private fun addServiceRecord(document: Document, record: ServiceRecordActivity.ServiceRecord) {
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
            Paragraph("Cost: Rs. ${record.serviceCost}")
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
}