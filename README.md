# Foodback - Passive Food Experience Evaluation Prototype

## Project Structure (`src` folder)

The project is organized into three main components:

- **backend/**  
  Python Flask backend handling user authentication, management, and data collection APIs.
- **FoodbackApp/**  
  Android application (for smartphone and wearable) responsible for data acquisition, inference, and user interface.
- **MindRove SDK/**  
  Android SDK and documentation for integrating with the MindRove EEG device.

---

## Components Overview

### 1. backend/
- **Technologies:** Flask, JWT, SQL
- **Features:**  
  - User authentication with JWT  
  - REST API for user and data management  
  - Database handling and logging  
- **Main entry:**  
  [`src/backend/app/app.py`](src/backend/app/app.py)

### 2. FoodbackApp/
- **Technologies:** Kotlin, Jetpack Compose, TensorFlow Lite
- **Modules:**  
  - **app/**: Main Android app for EEG (MindRove) and wearable (HR/EDA) data collection, real-time inference, synchronization, and CSV management.  
  - **wear/**: Companion Wear OS app for HR/EDA acquisition and Bluetooth/Wearable API data transfer.
- **Key features:**  
  - Real-time data synchronization and visualization  
  - Session management for tasting and rating  
  - On-device inference using EEGNet model converted to TensorFlow Lite  
  - CSV data export and sharing  
- **Main paths:**  
  [`src/FoodbackApp/app/src/main/java/unipi/msss/foodback/`](src/FoodbackApp/app/src/main/java/unipi/msss/foodback/)  
  [`src/FoodbackApp/wear/src/main/java/unipi/msss/foodback/`](src/FoodbackApp/wear/src/main/java/unipi/msss/foodback/)

### 3. MindRove SDK/
- **Contents:**  
  - Android SDK for MindRove EEG device communication  
  - Usage documentation ([src/MindRove SDK/android/v2.0/Doc.md](src/MindRove%20SDK/android/v2.0/Doc.md))
- **Features:**  
  - Wi-Fi connection management with MindRove  
  - EEG data parsing and handling (including triggers, accelerometer, battery status)

---

## Data Collection

Data acquisition is managed through Python scripts and Jupyter notebooks located in the [Data Collection/EEG/](../Data%20Collection/EEG/) directory:

- **Acquisition protocol:**  
  - [Protocol.md](../Data%20Collection/EEG/Protocol.md): Experimental protocol description.  
  - [protocol/protocol.py](../Data%20Collection/EEG/protocol/protocol.py): Main EEG data collection script.  
  - [protocol/protocol_downsampled.py](../Data%20Collection/EEG/protocol/protocol_downsampled.py): Downsampled acquisition variant.  
  - [real_time_visualization/real_time_eeg.py](../Data%20Collection/EEG/real_time_visualization/real_time_eeg.py): Real-time EEG data visualization.
- **Dependencies:**  
  - [requirements.txt](../Data%20Collection/EEG/requirements.txt): Python dependencies for data collection.

---

## Machine Learning Model

The ML pipeline is documented and implemented via Jupyter notebooks in [ML Model/](../ML%20Model/):

- **Main notebook:**  
  - [ml-model.ipynb](../ML%20Model/ml-model.ipynb):  
    - Data preprocessing (EEG, HR, EDA)  
    - Upsampling smartwatch data to 125Hz  
    - Train/test split  
    - EEGNet model definition, training with integrated preprocessing  
    - Conversion to TensorFlow Lite model ([eegnet_preproc.tflite](../ML%20Model/eegnet_preproc.tflite))  
    - Performance analysis (accuracy, loss, etc.)
- **Alternative pipeline:**  
  - [alternative-pipeline/alternative-ml-model.ipynb](../ML%20Model/alternative-pipeline/alternative-ml-model.ipynb): Variant preprocessing and training pipeline.

---

## Workflow Overview

1. **User login** via Flask backend.  
2. **Session start:**  
   - Android app connects to MindRove EEG device and wearable sensors.  
   - Signal synchronization and data acquisition begin.  
3. **Data acquisition:**  
   - EEG at 500 Hz from MindRove.  
   - HR/EDA at 125 Hz from wearable.  
   - Data saved and visualized via Python scripts/Jupyter notebooks.  
4. **Preprocessing & training:**  
   - Jupyter notebooks perform upsampling, dataset split, model training, and validation.  
5. **Inference:**  
   - Data preprocessed and fed to EEGNet TensorFlow Lite model on-device.  
   - User receives real-time feedback on food appreciation level.  
6. **Saving & export:**  
   - Data exportable as CSV files and shareable.

---

## Results

- Dataset: 45 tasting sessions with 10 volunteers.  
- Accuracy: ~33% (session-wise split), ~73% (epoch-wise split, possibly overestimated).

---

## Conclusions

This project demonstrates a proof-of-concept for passive evaluation of food experience leveraging wearable sensors and on-device neural inference.

---

## Further References

- [`src/backend/app/app.py`](src/backend/app/app.py)  
- [`src/FoodbackApp/app/src/main/java/unipi/msss/foodback/`](src/FoodbackApp/app/src/main/java/unipi/msss/foodback/)  
- [`src/FoodbackApp/wear/src/main/java/unipi/msss/foodback/`](src/FoodbackApp/wear/src/main/java/unipi/msss/foodback/)  
- [`src/MindRove SDK/android/v2.0/Doc.md`](src/MindRove%20SDK/android/v2.0/Doc.md)  
- [`Data Collection/EEG/Protocol.md`](../Data%20Collection/EEG/Protocol.md)  
- [`Data Collection/EEG/protocol/protocol.py`](../Data%20Collection/EEG/protocol/protocol.py)  
- [`Data Collection/EEG/real_time_visualization/real_time_eeg.py`](../Data%20Collection/EEG/real_time_visualization/real_time_eeg.py)  
- [`ML Model/ml-model.ipynb`](../ML%20Model/ml-model.ipynb)  
- [`ML Model/alternative-pipeline/alternative-ml-model.ipynb`](../ML%20Model/alternative-pipeline/alternative-ml-model.ipynb)
