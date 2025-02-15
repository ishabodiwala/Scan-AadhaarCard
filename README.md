# 📜 Aadhaar Card Scanner  

An Android application that allows users to scan Aadhaar cards using **ML Kit's Text Recognition API** and automatically store the extracted details in **Google Sheets**. This app ensures a seamless experience with real-time scanning, automatic data extraction, and cloud-based record-keeping.

## ✨ Features  

✔️ Real-time Aadhaar card scanning using a mobile camera  
✔️ Uses **ML Kit** for accurate text recognition  
✔️ **Google Sheets Integration** for data storage

## 📦 Dependencies & Technologies 

- 🔍 **ML Kit Text Recognition**  
- 📸 **CameraX**  
- 📊 **Google Sheets API**  
- 🔄 **Coroutines for async operations**   

## 🛠️ Setup  

### 🔹 Prerequisites  

- **Android Studio** (Latest version recommended)  
- **Google Cloud Console account**  
- **Google Sheet for data storage**  

### ☁️ Google Cloud Setup  

1. Create a new project in [Google Cloud Console](https://console.cloud.google.com/)  
2. Enable **Google Sheets API**  
3. Create a **Service Account**  
4. Download the `credentials.json` file  
5. Place `credentials.json` in `app/src/main/assets/`  
6. Share your Google Sheet with the **service account email**  


## 🔨 Building the Project  

**Step-1** Clone the repository  
  ```sh
  git clone https://github.com/ishabodiwala/Scan-AadharCard.git
  cd Scan-AadharCard
  ```

**Step-2** Open the project in Android Studio

**Step-3** Update the spreadsheet ID in GoogleSheetsHelper.kt

**Step-4** Build and run the project

## 🚀 Usage  

### 📷 Steps to Use the App  

1. **Open the app** on your Android device.  
2. **Grant camera permissions** when prompted.  
3. **Position the Aadhaar card** within the camera frame.  
4. **Wait for the app to process** and extract text using ML Kit.  
5. **Review the extracted information** displayed on the screen.  
6. **Tap "Save to Sheet"** to store the data in Google Sheets.  
