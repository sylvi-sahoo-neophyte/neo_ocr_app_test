package com.example.camera_ai

import RegexInference
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
import java.io.ByteArrayOutputStream
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

    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var imageContainer: LinearLayout


    private lateinit var box1: ConstraintLayout
    private lateinit var box2: ConstraintLayout
    private lateinit var box3: ConstraintLayout
    private lateinit var box4: ConstraintLayout

    private lateinit var editIconBATCH: ImageView

    private lateinit var priceValueText: TextView
    private lateinit var expiryValueText: TextView
    private lateinit var mfdValueText: TextView

//    private lateinit var recognizedTextView: TextView


    private lateinit var mrpTextView: TextView
    private lateinit var manufacturingDateTextView: TextView
    private lateinit var expiryDateTextView: TextView
    private lateinit var numberInput: EditText
    private lateinit var sessionButton: Button


    private var session: Int = 1

    // Global variable to store the API response details
    companion object {
        var ocrDetails: MutableMap<String, Any> = mutableMapOf()
    }

    @SuppressLint("MissingInflatedId", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera)

        // Initialize SharedPreferences
        val prefs: SharedPreferences = getSharedPreferences("session_prefs", MODE_PRIVATE)
        session = prefs.getInt("session_count", 1)  // Default value is 1

        cameraView = findViewById(R.id.captureButton)

        cameraView.setOnClickListener {
            camera.takePicture(null, null) { data, _ ->
                // Process the captured image
                processCapturedImage(data)
                camera.startPreview()
            }
        }

        // Initialize other views
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
        mrpTextView = findViewById(R.id.mrpTextView)
        manufacturingDateTextView = findViewById(R.id.manufacturingDateTextView)
        expiryDateTextView = findViewById(R.id.expiryDateTextView)
        box1 = findViewById(R.id.rect_1)
        box2 = findViewById(R.id.rect_2)
        box3 = findViewById(R.id.rect_3)
        box4 = findViewById(R.id.rect_4)
        priceValueText = findViewById(R.id.price) // MRP
        expiryValueText = findViewById(R.id.exp) // Expiry date
        mfdValueText = findViewById(R.id.mfd)

        // Initially hide the boxes and image
        hideBoxes()
        hideImage()

        rightIcon.setOnClickListener {
            if (isImageVisible) hideImage()
            toggleBoxesVisibility()
            updateRightIcon()
        }

        leftIcon.setOnClickListener {
            if (areBoxesVisible) hideBoxes()
            toggleImageVisibility()
            updateLeftIcon()
        }

        // Check for camera permission
        if (checkCameraPermission()) {
            setupCamera()
        } else {
            requestCameraPermission()
        }

        val editIcon: ImageView = findViewById(R.id.edit_icon1)
        numberInput = findViewById(R.id.number_input)
        val priceValueText: TextView = findViewById(R.id.price_value1)

        editIcon.setOnClickListener {
            numberInput.visibility = View.VISIBLE
            numberInput.requestFocus()
            showKeyboard(numberInput)
        }

        numberInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN) {
                handleNumberInput(priceValueText)
                true
            } else {
                false
            }
        }

        // Session Button
        sessionButton =
            findViewById(R.id.stop_session_button)  // Assume you have this button in your layout
        sessionButton.text = "Stop Session $session"

        sessionButton.setOnClickListener {
            session++
            sessionButton.text = "Stop Session $session"
            saveSessionCount(prefs)
            Toast.makeText(this, "Session ${session - 1} ended", Toast.LENGTH_SHORT).show()
        }

        // Other EditText interactions
        setupEditIcon(editIconBATCH, findViewById(R.id.edit_text4), findViewById(R.id.price_value4))
        editIconMFD.setOnClickListener { openDatePicker(R.id.price_value2) }
        editIconEXP.setOnClickListener { openDatePicker(R.id.price_value3) }


        setMidnightAlarm()
        checkSessionReset()
    }

    private fun setMidnightAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ResetSessionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0, // request code
            intent,
            PendingIntent.FLAG_IMMUTABLE // or FLAG_MUTABLE if needed
        )

        // Set the alarm to start at midnight
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // If the time for the alarm is in the past, move to the next day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }


    private fun checkSessionReset() {
        val prefs: SharedPreferences = getSharedPreferences("session_prefs", MODE_PRIVATE)
        val lastResetDate = prefs.getLong("last_reset_date", 0)
        val currentDate = System.currentTimeMillis()

        if (lastResetDate == 0L || isSameDay(lastResetDate, currentDate)) {
            // No reset needed or first run
        } else {
            // Reset the session
            session = 1
            saveSessionCount(prefs)
        }

        // Update the last reset date
        with(prefs.edit()) {
            putLong("last_reset_date", currentDate)
            apply()
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }


    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun handleNumberInput(priceValueText: TextView) {
        val enteredValue = numberInput.text.toString().trim()
        Log.d("EditText", "Captured value: $enteredValue")
        priceValueText.text = if (enteredValue.isNotEmpty()) enteredValue else "Rs 0"
        numberInput.visibility = View.GONE
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(numberInput.windowToken, 0)
    }

    private fun saveSessionCount(prefs: SharedPreferences) {
        with(prefs.edit()) {
            putInt("session_count", session)
            apply()
        }
    }

    private fun updateRightIcon() {
        if (areBoxesVisible) {
            rightIcon.setBackgroundResource(R.drawable.white_circle)
            rightIcon.setColorFilter(Color.BLACK)
            leftIcon.setBackgroundResource(R.drawable.grey_circle)
            leftIcon.setColorFilter(Color.WHITE)
        } else {
            rightIcon.setBackgroundResource(R.drawable.grey_circle)
            rightIcon.setColorFilter(Color.WHITE)
        }
    }

    private fun updateLeftIcon() {
        if (isImageVisible) {
            leftIcon.setBackgroundResource(R.drawable.white_circle)
            leftIcon.setColorFilter(Color.BLACK)
            rightIcon.setBackgroundResource(R.drawable.grey_circle)
            rightIcon.setColorFilter(Color.WHITE)
            displayCapturedImages()
        } else {
            leftIcon.setBackgroundResource(R.drawable.grey_circle)
            leftIcon.setColorFilter(Color.WHITE)
        }
    }

    private fun setupEditIcon(icon: ImageView, editText: EditText, valueText: TextView) {
        icon.setOnClickListener {
            editText.visibility = View.VISIBLE
            editText.requestFocus()
            showKeyboard(editText)
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN) {
                valueText.text = editText.text.toString()
                editText.visibility = View.GONE
                hideKeyboard()
                true
            } else {
                false
            }
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

    @SuppressLint("SetTextI18n")
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

        // Set up a camera preview callback to process frames
        camera.setPreviewCallback { data, _ ->
            processImage(data, camera.parameters.previewSize)
        }
    }

    private fun processImage(data: ByteArray, previewSize: Camera.Size) {
        val image = YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null)
        val out = ByteArrayOutputStream()
        image.compressToJpeg(Rect(0, 0, previewSize.width, previewSize.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Create InputImage object
        val inputImage = InputImage.fromBitmap(bitmap, getRotationCompensation())

        // Set up ML Kit text recognition
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Process the image
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                val recognizedText = visionText.text
                runOnUiThread {
                    // Update UI with recognized text
                    val extractedData = RegexInference.processText(recognizedText)

                    // Get MRP, Manufacturing Date, and Expiry Date
                    val mrp = extractedData["mrp"] as List<Double>?
                    val manufacturingDate = extractedData["manufacturingDate"] as String?
                    val expiryDate = extractedData["expiryDate"] as String?

//                    mrpTextView.text = if (mrp != null) {
//                        "Extracted MRP: ${mrp.joinToString(", ")}"
//                    } else {
//                        "Extracted MRP : 0"
//                    }

//                    manufacturingDateTextView.text = if (manufacturingDate != null) {
//                        "Manufacturing Date: $manufacturingDate"
//                    } else {
//                        "Manufacturing Date: 0 "
//                    }
//
//                    expiryDateTextView.text = if (expiryDate != null) {
//                        "Expiry Date: $expiryDate"
//                    } else {
//                        "Expiry Date: 0"
//                    }


                    val sharedPreferences =
                        this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()


                    // Retrieve previously stored values from SharedPreferences
                    var previousMrp: List<Double> =
                        sharedPreferences.getString("previousMrp", "0.0")?.split(",")
                            ?.map { it.toDouble() } ?: listOf(0.0)
                    var previousManufacturingDate: String =
                        sharedPreferences.getString("previousManufacturingDate", "0") ?: "0"
                    var previousExpiryDate: String =
                        sharedPreferences.getString("previousExpiryDate", "0") ?: "0"

                    // Update the TextViews and check for new values
                    mrpTextView.text =
                        if (mrp != null && mrp.isNotEmpty() && mrp.any { it != 0.0 }) {
                            previousMrp = mrp // Update previous value only if valid
                            editor.putString(
                                "previousMrp",
                                mrp.joinToString(",")
                            ) // Store in SharedPreferences
                            editor.apply()
                            "Extracted MRP: ${mrp.joinToString(", ")}"
                        } else {
                            // If current mrp is null, empty, or all zeros, show previous value
                            "Extracted MRP: ${previousMrp.joinToString(", ")}"
                        }

                    manufacturingDateTextView.text =
                        if (manufacturingDate != null && manufacturingDate != "0") {
                            previousManufacturingDate =
                                manufacturingDate // Update previous value only if valid
                            editor.putString(
                                "previousManufacturingDate",
                                manufacturingDate
                            ) // Store in SharedPreferences
                            editor.apply() // Commit changes
                            "Manufacturing Date: $manufacturingDate"
                        } else {
                            // If current manufacturingDate is null or zero, show previous value
                            "Manufacturing Date: $previousManufacturingDate"
                        }

                    expiryDateTextView.text = if (expiryDate != null && expiryDate != "0") {
                        previousExpiryDate = expiryDate // Update previous value only if valid
                        editor.putString(
                            "previousExpiryDate",
                            expiryDate
                        ) // Store in SharedPreferences
                        editor.apply() // Commit changes
                        "Expiry Date: $expiryDate"
                    } else {
                        // If current expiryDate is null or zero, show previous value
                        "Expiry Date: $previousExpiryDate"
                    }

// Log extracted information
                    Log.d(
                        "Extracted Data:",
                        "Price: $previousMrp, Manufacturing Date: $previousManufacturingDate, Expiry Date: $previousExpiryDate"
                    )

// Log extracted information
//                    Log.d(
//                        "Extracted Data:",
//                        "Price: $previousMrp, Manufacturing Date: $previousManufacturingDate, Expiry Date: $previousExpiryDate"
//                    )

                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                Log.e("ML Kit", "Text recognition failed: ${e.message}")
            }
    }


    private fun getRotationCompensation(): Int {
        val deviceRotation = windowManager.defaultDisplay.rotation
        var degrees = when (deviceRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        // Account for the mirroring of the image on the device screen
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo)
        degrees = (cameraInfo.orientation - degrees + 360) % 360

        return degrees
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
                        val mrp = ocrResult.getDouble("MRP").toString()
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

                        runOnUiThread {
                            displaydatafromAPI(ocrDetails)
                        }

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


    private fun displaydatafromAPI(response: Map<String, Any>) {
        runOnUiThread {
            // Log the received response to check if the values are correct
            Log.d("updateUIFromResponse", "Received response: $response")

            // Safely convert values to String
            val mrp = response["MRP"]?.toString() ?: "Rs 0"
            val expiry = response["Exp. Date"]?.toString() ?: "MM/DD/YYYY"
            val mfd = response["Mfg. Date"]?.toString() ?: "MM/DD/YYYY"

            // Log the parsed values to verify them
            Log.d("updateUIFromResponse", "Parsed values -> MRP: $mrp, Expiry: $expiry, MFD: $mfd")


//            findViewById<View>(R.id.rect_3).visibility = View.VISIBLE
//            findViewById<View>(R.id.rect_4).visibility = View.VISIBLE

            // Set the new text values
            priceValueText.text = mrp
            expiryValueText.text = expiry
            mfdValueText.text = mfd

            // Log the text set to the TextViews to ensure they are updated
            Log.d("mrp", "MRP TextView: ${priceValueText.text}")
            Log.d("updateUIFromResponse", "Expiry TextView: ${expiryValueText.text}")
            Log.d("updateUIFromResponse", "MFD TextView: ${mfdValueText.text}")

            // Force invalidate the parent layout if needed
            val parentLayout: View =
                findViewById(R.id.full_screen_image) // Replace with your parent layout ID
            parentLayout.invalidate()
        }
    }

    private fun releaseCamera() {
        try {
            if (::camera.isInitialized) {
                camera.setPreviewCallback(null)
                camera.stopPreview()
                camera.release()
            }
        } catch (e: Exception) {
            Log.e("Camera Error", "Error releasing camera: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    override fun onResume() {
        super.onResume()
        if (::camera.isInitialized.not() && checkCameraPermission()) {
            setupCamera()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        releaseCamera() // Release camera resources when the activity is destroyed
    }


}