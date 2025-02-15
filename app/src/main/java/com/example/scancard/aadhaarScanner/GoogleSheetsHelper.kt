package com.example.scancard.aadhaarScanner

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSheetsHelper(private val context: Context) {
    private val spreadsheetId = "" // Replace with your spreadsheet ID
    private val range = "Sheet1!A:D" // Adjust range as needed

    private suspend fun getSheetService(): Sheets = withContext(Dispatchers.IO) {
        val credentials = context.assets.open("credentials.json").use { inputStream ->
            GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        }
        return@withContext Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("Aadhaar Scanner")
            .build()
    }

    suspend fun appendRow(name: String, aadhaarNumber: String, dob: String) = withContext(Dispatchers.IO) {
        try {
            val service = getSheetService()
            val timestamp = System.currentTimeMillis()
            
            val values = listOf(
                listOf(timestamp, name, aadhaarNumber, dob)
            )
            
            val body = ValueRange().setValues(values)
            
            service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute()

            Log.d("GoogleSheets", "Data appended successfully")
        } catch (e: Exception) {
            Log.e("GoogleSheets", "Error appending data", e)
            throw e
        }
    }
} 