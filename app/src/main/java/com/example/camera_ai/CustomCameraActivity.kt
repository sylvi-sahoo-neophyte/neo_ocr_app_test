package com.example.camera_ai

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.buffer
import okio.source
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CustomCameraActivity : AppCompatActivity() {


    private lateinit var camera: Camera
    private lateinit var preview: CameraPreview
    private val REQUEST_CODE_CAMERA_PERMISSION = 100
    private lateinit var leftIcon: ImageView
    private lateinit var rightIcon: ImageView
    private var imageFile: File? = null
    private lateinit var cameraView: ImageButton

    private var capturedImages = mutableListOf<Bitmap>() // List to store captured images


    private lateinit var rectangular_box1: ConstraintLayout
    private lateinit var rectangular_box2: ConstraintLayout
    private lateinit var rectangular_box3: ConstraintLayout
    private lateinit var rectangular_box4: ConstraintLayout


    private lateinit var fullScreenImageView: ImageView


    private lateinit var editIconMFD: ImageView
    private lateinit var editIconEXP: ImageView

    private var areBoxesVisible = false
    private var isImageVisible = false
    private var isFullScreen = false

    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var imageContainer: LinearLayout


    private lateinit var box1: ConstraintLayout
    private lateinit var box2: ConstraintLayout
    private lateinit var box3: ConstraintLayout
    private lateinit var box4: ConstraintLayout

    private lateinit var editIconBATCH: ImageView

    // Global variable to store the API response details
    companion object {
        var ocrDetails: MutableMap<String, Any> = mutableMapOf()
    }


    @SuppressLint("MissingInflatedId", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera)

        cameraView = findViewById(R.id.captureButton)

        cameraView.setOnClickListener {
            camera.takePicture(null, null) { data, _ ->
                // Process the captured image
                processCapturedImage(data)

                // Immediately restart the camera preview after the image is processed
                camera.startPreview()
            }
        }

        // Initialize the boxes
        rectangular_box1 = findViewById(R.id.rectangular_box1)
        rectangular_box2 = findViewById(R.id.rectangular_box2)
        rectangular_box3 = findViewById(R.id.rectangular_box3)
        rectangular_box4 = findViewById(R.id.rectangular_box4)
        leftIcon = findViewById(R.id.left_icon)
        rightIcon = findViewById(R.id.right_icon)
        imageContainer = findViewById(R.id.image_container)
        horizontalScrollView = findViewById(R.id.horizontal_scroll_view)
        editIconMFD = findViewById(R.id.edit_icon2)
        editIconEXP = findViewById(R.id.edit_icon3)
        editIconBATCH = findViewById(R.id.edit_icon4)
        fullScreenImageView = findViewById(R.id.full_screen_image)


        box1 = findViewById(R.id.rect_1)
        box2 = findViewById(R.id.rect_2)
        box3 = findViewById(R.id.rect_3)
        box4 = findViewById(R.id.rect_4)


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


//        val editIcon: ImageView = findViewById(R.id.edit_icon1)


        // Find the views
        val editIcon: ImageView = findViewById(R.id.edit_icon1)
        val numberInput: EditText = findViewById(R.id.number_input)
        val priceValueText: TextView = findViewById(R.id.price_value1)


        editIcon.setOnClickListener {
            numberInput.visibility = View.VISIBLE
            numberInput.requestFocus()

            // Show the keyboard programmatically
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(numberInput, InputMethodManager.SHOW_IMPLICIT)
        }


        numberInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN) {
                val enteredValue = numberInput.text.toString().trim()

                // Log to check if the value is being captured
                Log.d("EditText", "Captured value: $enteredValue")

                if (enteredValue.isNotEmpty()) {
                    priceValueText.text = enteredValue // Update TextView with new value
                } else {
                    priceValueText.text = "Rs 0" // Fallback if input is empty
                }

                // Hide the EditText and the keyboard after input
                numberInput.visibility = View.GONE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(numberInput.windowToken, 0)
                true
            } else {
                false
            }
        }


        val editText4: EditText = findViewById(R.id.edit_text4)
        val Batchvalue: TextView = findViewById(R.id.price_value4)

        editIconBATCH.setOnClickListener {
            editText4.visibility = View.VISIBLE
            editText4.requestFocus() // Focus on the EditText to open the keyboard

            // Show the keyboard programmatically
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText4, InputMethodManager.SHOW_IMPLICIT)
        }

        // Optionally, hide the EditText and keyboard when done
        editText4.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN) {
                Batchvalue.text = editText4.text.toString() // Update the TextView with input text

                // Hide the EditText and keyboard
                editText4.visibility = View.GONE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText4.windowToken, 0)
                true
            } else {
                false
            }
        }

        editIconMFD.setOnClickListener {
            openDatePicker(R.id.price_value2)
        }

        editIconEXP.setOnClickListener {
            openDatePicker(R.id.price_value3)
        }

    }

    private fun openDatePicker(textViewId: Int) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Handle the selected date here
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                // Update the appropriate TextView with the selected date
                val priceValue: TextView = findViewById(textViewId)
                priceValue.text = selectedDate
            }, year, month, day)

        datePickerDialog.show()
    }


    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION
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
        rectangular_box1.visibility = View.GONE
        rectangular_box2.visibility = View.GONE
        rectangular_box3.visibility = View.GONE
        rectangular_box4.visibility = View.GONE
        areBoxesVisible = false
    }

    private fun showBoxes() {
        rectangular_box1.visibility = View.VISIBLE
        rectangular_box2.visibility = View.VISIBLE
        rectangular_box3.visibility = View.VISIBLE
        rectangular_box4.visibility = View.VISIBLE
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

            // Log the path of the saved image
            Log.d("ImagePath", "Image saved at: ${imageFile!!.absolutePath}")

            // Load the image file and correct its orientation
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            val rotatedBitmap = rotateImageToPortrait(bitmap, imageFile!!.absolutePath)

            // Add the image to the list of captured images
            capturedImages.add(rotatedBitmap)

            uploadImage(imageFile!!) { success, message ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(this, "Failed to upload image: $message", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            // Notify user
            Toast.makeText(this, "Image saved!", Toast.LENGTH_SHORT).show()


        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }


    fun uploadImage(imageFile: File, callback: (Boolean, String?) -> Unit) {
        // Configure timeouts for the OkHttpClient
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // Increase connection timeout
            .readTimeout(30, TimeUnit.SECONDS)     // Increase read timeout
            .writeTimeout(30, TimeUnit.SECONDS)    // Increase write timeout
            .build()

        Log.d("ImageURI1", "Image1: ${imageFile.absolutePath}")
        Log.d("ImageUploadSDK", "File size: ${imageFile.length()} bytes")

        val mediaType = "image/jpeg".toMediaType()

        val requestBody = object : RequestBody() {
            override fun contentType() = mediaType

            override fun contentLength() = imageFile.length()

            override fun writeTo(sink: BufferedSink) {
                val source = imageFile.source().buffer()
                sink.writeAll(source)
                source.close()
            }
        }

        // Build the request with multipart form data
        val request = Request.Builder()
            .url("http://216.48.183.210:8000/upload-image/image.jpg")
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.name, requestBody)
                    .build()
            )
            .build()

        Log.d("ImageUploadSDK", "Sending request to ${request.url}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ImageUploadSDK", "API call failed", e)
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                val responseBody = response.body?.string() ?: ""

                if (success) {
                    try {
                        // Parse the response body as JSON
                        val jsonObject = JSONObject(responseBody)
                        val bodyObject = jsonObject.getJSONObject("body")
                        val ocrResult = bodyObject.getJSONObject("ocr_result")

                        // Extract fields from the response
                        val mrp = ocrResult.getDouble("MRP")
                        val mfgDate = ocrResult.getString("Mfg. Date")
                        val expDate = ocrResult.getString("Exp. Date")
                        val metadataId = bodyObject.getString("metadata_id")
                        val barcode = bodyObject.getString("barcode")

                        // Store the extracted details in the global variable
                        ocrDetails["MRP"] = mrp
                        ocrDetails["Mfg. Date"] = mfgDate
                        ocrDetails["Exp. Date"] = expDate
                        ocrDetails["Metadata ID"] = metadataId
                        ocrDetails["Barcode"] = barcode

                        // Log or print the global variable
                        Log.d("ParsedResponse", ocrDetails.toString())
                        callback(success, "Details stored globally.")

                    } catch (e: JSONException) {
                        Log.e("ImageUploadSDK", "Failed to parse JSON: ${e.message}")
                        callback(false, "Failed to parse JSON")
                    }
                } else {
                    val errorBody = responseBody.ifEmpty { "Unknown error" }
                    Log.e(
                        "ImageUploadSDK",
                        "Upload failed: ${response.message}, Response body: $errorBody"
                    )
                    callback(false, "Upload failed: ${response.message}, Response body: $errorBody")
                }
            }
        })
    }


    private fun displayCapturedImages() {
        imageContainer.removeAllViews() // Clear previously displayed images

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

                // Set click listener to display the image in full-screen mode
                setOnClickListener {
                    showFullScreenImage(rotatedBitmap)
                }
            }
            imageContainer.addView(newImageView)
        }
        horizontalScrollView.visibility = View.VISIBLE
    }

    private fun showFullScreenImage(image: Bitmap) {


        // Ensure the view exists before manipulating
        if (fullScreenImageView != null && cameraView != null) {
            // Hide the camera view and show the full-screen image
            cameraView.visibility = View.GONE
            fullScreenImageView.setImageBitmap(image)
            fullScreenImageView.visibility = View.VISIBLE

            box1.visibility = View.VISIBLE
            box2.visibility = View.VISIBLE
            box3.visibility = View.VISIBLE
            box4.visibility = View.VISIBLE

            // Set a click listener to exit full-screen mode and return to the camera view
            fullScreenImageView.setOnClickListener {
                fullScreenImageView.visibility = View.GONE
                box1.visibility = View.GONE
                box2.visibility = View.GONE
                box3.visibility = View.GONE
                box4.visibility = View.GONE
                cameraView.visibility = View.VISIBLE
            }
        }
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
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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