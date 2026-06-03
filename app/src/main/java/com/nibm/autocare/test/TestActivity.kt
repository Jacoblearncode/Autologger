package com.nibm.autocare

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class TestActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var uploadButton: Button
    private lateinit var urlTextView: TextView
    private lateinit var uploadProgressBar: ProgressBar
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        // Initialize views
        imageView = findViewById(R.id.imageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        uploadButton = findViewById(R.id.uploadButton)
        urlTextView = findViewById(R.id.urlTextView)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)


        // Button to select an image from the gallery
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Button to upload the selected image to Cloudinary
        uploadButton.setOnClickListener {
            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri!!)
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle the result of image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            Glide.with(this).load(selectedImageUri).into(imageView)
        }
    }

    // Upload the image to Cloudinary
    private fun uploadImageToCloudinary(imageUri: Uri) {
        MediaManager.get().upload(imageUri)
            .option("folder", "Home/AutoCare") // Save to the "Home/AutoCare" folder
            .option("public_id", "unique_image_name_${System.currentTimeMillis()}") // Unique public ID
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Upload started")
                    uploadProgressBar.progress = 0 // Reset progress bar
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = ((bytes * 100) / totalBytes).toInt()
                    uploadProgressBar.progress = progress // Update progress bar
                    Log.d("Cloudinary", "Upload in progress: $progress%")
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["url"].toString()
                    Log.d("Cloudinary", "Upload successful. URL: $imageUrl")
                    urlTextView.text = imageUrl
                    uploadProgressBar.progress = 100 // Set progress to 100%
                    Toast.makeText(this@TestActivity, "Upload successful!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Upload failed: ${error.description}")
                    Toast.makeText(this@TestActivity, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.d("Cloudinary", "Upload rescheduled")
                }
            })
            .dispatch()
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }
}