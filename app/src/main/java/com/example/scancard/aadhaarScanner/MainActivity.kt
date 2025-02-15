package com.example.scancard.aadhaarScanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scancard.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sheetsHelper: GoogleSheetsHelper
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var cameraProvider: ProcessCameraProvider

    private var scannedName = ""
    private var scannedAadhaar = ""
    private var scannedDob = ""
    private lateinit var saveButton: Button
    private var isScanning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        saveButton = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveDataToSheet()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        sheetsHelper = GoogleSheetsHelper(this)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, AadhaarAnalyzer { text ->
                        if (isScanning) {
                            processAadhaarText(text)
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processAadhaarText(text: String) {
        Log.d("AadhaarScanner", "Scanned Text:\n$text")

        // Extract Aadhaar number (12 digits)
        val aadhaarPattern = Regex("\\d{4}\\s\\d{4}\\s\\d{4}")
        val aadhaarMatch = aadhaarPattern.find(text)
        aadhaarMatch?.value?.let {
            scannedAadhaar = it
            runOnUiThread {
                findViewById<TextView>(R.id.aadharNumberText).text = "Aadhaar: $it"
            }
        }

        // Extract DOB (DD/MM/YYYY format)
        val dobPattern = Regex("\\d{2}/\\d{2}/\\d{4}")
        val dobMatch = dobPattern.find(text)
        dobMatch?.value?.let {
            scannedDob = it
            runOnUiThread {
                findViewById<TextView>(R.id.dobText).text = "DOB: $it"
            }
        }

        // Extract name (line after "Government of India")
        val lines = text.split("\n")
        for (i in lines.indices) {
            if (lines[i].contains("Government of India", ignoreCase = true) && i + 1 < lines.size) {
                val potentialName = lines[i + 1].trim()
                // Filter out lines that are likely not names (contains numbers or is too short)
                if (potentialName.length > 3 && !potentialName.contains(Regex("\\d"))) {
                    scannedName = potentialName
                    runOnUiThread {
                        findViewById<TextView>(R.id.nameText).text = "Name: $potentialName"
                    }
                    break
                }
            }
        }

        // Enable save button if we have all the data
        runOnUiThread {
            saveButton.isEnabled = scannedName.isNotEmpty() &&
                    scannedAadhaar.isNotEmpty() &&
                    scannedDob.isNotEmpty()
        }
    }

    private fun saveDataToSheet() {
        saveButton.isEnabled = false
        isScanning = false // Stop scanning

        CoroutineScope(Dispatchers.Main).launch {
            try {
                sheetsHelper.appendRow(scannedName, scannedAadhaar, scannedDob)
                Toast.makeText(this@MainActivity, "Data saved successfully!", Toast.LENGTH_SHORT)
                    .show()
                // Clear the scanned data after successful save
                clearScannedData()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error saving data", e)
                Toast.makeText(
                    this@MainActivity,
                    "Failed to save data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                saveButton.isEnabled = true
                isScanning = true // Resume scanning if save failed
            }
        }
    }

    private fun clearScannedData() {
        scannedName = ""
        scannedAadhaar = ""
        scannedDob = ""

        findViewById<TextView>(R.id.nameText).text = ""
        findViewById<TextView>(R.id.aadharNumberText).text = ""
        findViewById<TextView>(R.id.dobText).text = ""
        saveButton.isEnabled = false
        isScanning = true // Resume scanning after clearing data
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}