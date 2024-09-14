package com.example.camera_ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.Camera
import android.view.Surface

class CustomCameraActivity : AppCompatActivity() {

    private lateinit var camera: Camera
    private lateinit var preview: CameraPreview
    private val REQUEST_CODE_CAMERA_PERMISSION = 100

    // Declare TextView references for the boxes
    private lateinit var rectBox1: TextView
    private lateinit var rectBox2: TextView
    private lateinit var rectBox3: TextView
    private lateinit var rectBox4: TextView
    private var areBoxesVisible = false  // Track visibility state of boxes

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera)

        val captureButton = findViewById<Button>(R.id.captureButton)
        captureButton.setOnClickListener {
            camera.takePicture(null, null) { data, _ ->
                // Process the captured image
                processCapturedImage(data)
            }
        }

        // Initialize the boxes
        rectBox1 = findViewById(R.id.rect_box1)
        rectBox2 = findViewById(R.id.rect_box2)
        rectBox3 = findViewById(R.id.rect_box3)
        rectBox4 = findViewById(R.id.rect_box4)

        // Initially hide the boxes
        hideBoxes()

        // Handle right icon click to toggle boxes visibility
        val rightIcon = findViewById<ImageView>(R.id.right_icon)
        rightIcon.setOnClickListener {
            toggleBoxesVisibility()
        }

        // Check for camera permission
        if (checkCameraPermission()) {
            setupCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE_CAMERA_PERMISSION
        )
    }

    private fun setupCamera() {
        camera = Camera.open()
        val params = camera.parameters

        // Set the best possible focus mode (auto or continuous picture)
        val focusModes = params.supportedFocusModes
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }

        // Set the highest picture quality available
        val sizes = params.supportedPictureSizes
        val bestSize = getBestPictureSize(sizes)
        params.setPictureSize(bestSize.width, bestSize.height)

        // Set the best preview size available
        val previewSizes = params.supportedPreviewSizes
        val bestPreviewSize = getBestPreviewSize(previewSizes)
        params.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height)

        // Set the JPEG quality (highest = 100)
        params.jpegQuality = 100

        // Set these parameters to the camera
        camera.parameters = params

        // Start the camera preview
        preview = CameraPreview(this, camera)
        val previewFrame = findViewById<FrameLayout>(R.id.camera_preview)
        previewFrame.addView(preview)

        // Set the correct display orientation
        setCameraDisplayOrientation()
    }

    // Toggle visibility of boxes
    private fun toggleBoxesVisibility() {
        if (areBoxesVisible) {
            hideBoxes()
        } else {
            showBoxes()
        }
    }

    // Hide all the boxes
    private fun hideBoxes() {
        rectBox1.visibility = View.GONE
        rectBox2.visibility = View.GONE
        rectBox3.visibility = View.GONE
        rectBox4.visibility = View.GONE
        areBoxesVisible = false
    }

    // Show all the boxes
    private fun showBoxes() {
        rectBox1.visibility = View.VISIBLE
        rectBox2.visibility = View.VISIBLE
        rectBox3.visibility = View.VISIBLE
        rectBox4.visibility = View.VISIBLE
        areBoxesVisible = true
    }

    // Get the best picture size
    private fun getBestPictureSize(sizes: List<Camera.Size>): Camera.Size {
        var bestSize = sizes[0]
        for (size in sizes) {
            if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size
            }
        }
        return bestSize
    }

    // Get the best preview size
    private fun getBestPreviewSize(sizes: List<Camera.Size>): Camera.Size {
        var bestSize = sizes[0]
        for (size in sizes) {
            if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size
            }
        }
        return bestSize
    }

    private fun setCameraDisplayOrientation() {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info)

        val rotation = windowManager.defaultDisplay.rotation
        val degrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        val displayOrientation = (info.orientation - degrees + 360) % 360
        camera.setDisplayOrientation(displayOrientation)
    }

    private fun processCapturedImage(data: ByteArray) {
        // Process the image data
        Toast.makeText(this, "Image captured!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.release()  // Release the camera when done
    }
}
