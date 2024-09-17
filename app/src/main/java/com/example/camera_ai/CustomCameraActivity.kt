package com.example.camera_ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.media.ExifInterface
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class CustomCameraActivity : AppCompatActivity() {

    private lateinit var camera: Camera
    private lateinit var preview: CameraPreview
    private val REQUEST_CODE_CAMERA_PERMISSION = 100
    private lateinit var displayImageView: ImageView
    private lateinit var leftIcon: ImageView
    private lateinit var rightIcon: ImageView
    private var imageFile: File? = null
    private lateinit var captureButton: ImageButton

    private var capturedImages = mutableListOf<Bitmap>() // List to store captured images

    // Declare TextView references for the boxes
    private lateinit var rectBox1: TextView
    private lateinit var rectBox2: TextView
    private lateinit var rectBox3: TextView
    private lateinit var rectBox4: TextView

    private var areBoxesVisible = false
    private var isImageVisible = false

    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var imageContainer: LinearLayout

    @SuppressLint("MissingInflatedId", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera)

        captureButton = findViewById(R.id.captureButton)

        captureButton.setOnClickListener {
            camera.takePicture(null, null) { data, _ ->
                // Process the captured image
                processCapturedImage(data)

                // Immediately restart the camera preview after the image is processed
                camera.startPreview()
            }
        }

        // Initialize the boxes
        rectBox1 = findViewById(R.id.rect_box1)
        rectBox2 = findViewById(R.id.rect_box2)
        rectBox3 = findViewById(R.id.rect_box3)
        rectBox4 = findViewById(R.id.rect_box4)
        leftIcon = findViewById(R.id.left_icon)
        rightIcon = findViewById(R.id.right_icon)
        imageContainer = findViewById(R.id.image_container)
        horizontalScrollView = findViewById(R.id.horizontal_scroll_view)

        // Initially hide the boxes and image
        hideBoxes()
        hideImage()

        rightIcon.setOnClickListener {
            if (isImageVisible) {
                hideImage()
            }
            toggleBoxesVisibility()

            // Update right icon background and color based on the visibility of the boxes
            if (areBoxesVisible) {
                rightIcon.setBackgroundResource(R.drawable.white_circle)
                rightIcon.setColorFilter(Color.BLACK)

                // Reset the left icon to grey when the right icon is selected
                leftIcon.setBackgroundResource(R.drawable.grey_circle)
                leftIcon.setColorFilter(Color.WHITE)
            } else {
                rightIcon.setBackgroundResource(R.drawable.grey_circle)
                rightIcon.setColorFilter(Color.WHITE)
            }
        }

        // Handle left icon click to toggle image visibility
        leftIcon.setOnClickListener {
            if (areBoxesVisible) {
                hideBoxes()
            }
            toggleImageVisibility()

            // Show the captured images when the left icon is clicked
            if (isImageVisible) {
                leftIcon.setBackgroundResource(R.drawable.white_circle)
                leftIcon.setColorFilter(Color.BLACK)

                // Reset the right icon to grey when the left icon is selected
                rightIcon.setBackgroundResource(R.drawable.grey_circle)
                rightIcon.setColorFilter(Color.WHITE)

                // Display all captured images
                displayCapturedImages()
            } else {
                leftIcon.setBackgroundResource(R.drawable.grey_circle)
                leftIcon.setColorFilter(Color.WHITE)
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

    private fun toggleBoxesVisibility() {
        if (areBoxesVisible) {
            hideBoxes()
        } else {
            showBoxes()
        }
    }

    private fun hideBoxes() {
        rectBox1.visibility = View.GONE
        rectBox2.visibility = View.GONE
        rectBox3.visibility = View.GONE
        rectBox4.visibility = View.GONE
        areBoxesVisible = false
    }

    private fun showBoxes() {
        rectBox1.visibility = View.VISIBLE
        rectBox2.visibility = View.VISIBLE
        rectBox3.visibility = View.VISIBLE
        rectBox4.visibility = View.VISIBLE
        areBoxesVisible = true
    }

    private fun toggleImageVisibility() {
        if (isImageVisible) {
            hideImage()
        } else {
            showImage()
        }
        isImageVisible = !isImageVisible
    }

    private fun showImage() {
        horizontalScrollView.visibility = View.VISIBLE
    }

    private fun hideImage() {
        horizontalScrollView.visibility = View.GONE
    }

    private fun getBestPictureSize(sizes: List<Camera.Size>): Camera.Size {
        var bestSize = sizes[0]
        for (size in sizes) {
            if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size
            }
        }
        return bestSize
    }

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
        imageFile = File(getExternalFilesDir(null), "captured_image.jpg")

        try {
            // Save the captured image
            FileOutputStream(imageFile).use { outputStream ->
                outputStream.write(data)
            }

            // Load the image file and correct its orientation
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            val rotatedBitmap = rotateImageToPortrait(bitmap, imageFile!!.absolutePath)

            // Add the image to the list of captured images
            capturedImages.add(rotatedBitmap)

            // Notify user
            Toast.makeText(this, "Image saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }


    private fun displayCapturedImages() {
        imageContainer.removeAllViews() // Clear any previously displayed images

        // Iterate over the captured images and add them to the container
        for (capturedImage in capturedImages) {
            val rotatedBitmap = rotateBitmapForVerticalDisplay(capturedImage)
            val newImageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    80.dpToPx(), // Adjust size as needed
                    150.dpToPx() // Adjust size to maintain vertical orientation
                ).apply {
                    marginEnd = 8.dpToPx()
                }
                setImageBitmap(rotatedBitmap)
            }
            imageContainer.addView(newImageView)
        }
        horizontalScrollView.visibility = View.VISIBLE
    }

    private fun rotateBitmapForVerticalDisplay(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f) // Rotate the image 90 degrees clockwise to ensure vertical display
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun rotateImageToPortrait(bitmap: Bitmap, imagePath: String): Bitmap {
        val exif = ExifInterface(imagePath)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        camera.release()
    }

    override fun onResume() {
        super.onResume()
        if (::camera.isInitialized.not() && checkCameraPermission()) {
            setupCamera()
        }
    }
}
