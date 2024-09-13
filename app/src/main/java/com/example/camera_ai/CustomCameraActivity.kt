package com.example.camera_ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.Camera
import android.widget.FrameLayout

class CustomCameraActivity : AppCompatActivity() {

    private lateinit var camera: Camera
    private lateinit var preview: CameraPreview
    private val REQUEST_CODE_CAMERA_PERMISSION = 100

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
        preview = CameraPreview(this, camera)
        val previewFrame = findViewById<FrameLayout>(R.id.camera_preview)
        previewFrame.addView(preview)
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
