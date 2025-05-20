<img src="Readme Resources/images/Title ligth.webp" alt="Title" style="border-radius: 15px;"/></br>

# Table of Contents
- [Table of Contents](#table-of-contents)
- [Overview](#overview)
- [System Features](#system-features)
- [Scientific Background](#scientific-background)
- [System Architecture](#system-architecture)
- [Installation](#installation)
  - [Building from Source (Android Studio)](#building-from-source-android-studio)
  - [Flask Server](#flask-server)
- [Usage](#usage)
  - [🧠 Data Collection Mode](#-data-collection-mode)
  - [⚡ Inference Mode](#-inference-mode)
- [Result \& Evaluation](#result--evaluation)
- [License](#license)
- [Contributors](#contributors)

# Overview
Foodback is a mobile app that automatically rates food by analyzing physiological and neural signals from wearable devices like EEG headsets and smartwatches. During short tasting sessions, the system captures brain activity, heart rate, and skin responses, using a deep learning model to predict food enjoyment on a 5-point scale. It offers a scalable, objective, and effortless alternative to traditional food reviews.

# System Features
The presented application is a **prototype** developed to demonstrate the feasibility of fully automated, user-independent food rating through physiological and neural signal analysis. While functional, the system remains at an experimental stage, aimed primarily at validating core concepts and guiding future development. The prototype's implemented functionalities include:

* 🤖 Automatic Food Rating
* 📡 Real-Time Multimodal Signal Integration
* 🧠 Lightweight End-to-End Neural Architecture
* 🔄 Dual Operational Modes
* 🔐 Secure Authentication and Role-Based Access
* 🎯 Guided Experiment Interface
* ⌚ Reliable Wearable Synchronization
* 🧩 Modular and Extensible Architecture

# Scientific Background
Our system builds on recent research in emotion and sensory recognition using wearable and neural signals. Notably, [EmotionSense](https://dl.acm.org/doi/abs/10.1145/3384394?casa_token=1_weDYcQRMUAAAAA:SGxIzGSlrhrdo7eTaPnmUtZ5AC9DSPp-LuJRtoXeSyELLNCTwNPEX5NsoJhj3y6VSAoDDx9x_zxotw) demonstrated emotion tracking via smartwatches with context-aware modeling, but was limited to general emotional states and lacked integration with neural data, reducing its applicability to specific experiences like food enjoyment.

Conversely, [Zhang et al.](https://link.springer.com/article/10.1007/s10489-024-05374-5) achieved high-accuracy taste recognition using EEG and deep learning, but relied on artificial lab settings, with limited ecological validity and potential methodological biases due to unclear data partitioning.

Our approach integrates smartwatch and EEG data in real-world settings, capturing both physiological and cognitive responses to food. This enables automated, objective, and scalable reviews of food experiences, bridging the gap between lab-grade precision and everyday usability.

# System Architecture
<img src="Readme Resources/images/FoodbackArchitecture.webp" alt="Title" style="border-radius: 15px;"/></br>

The system is composed of multiple interconnected components designed to capture, process, and store physiological and neural signals during food consumption in real-world settings.

* **EEG (MindRove ARC)**: Captures brain activity related to sensory and cognitive responses during eating.
* **Smartwatch (Google Pixel Watch 2)**: Records **heart rate** and **electrodermal activity**, providing real-time indicators of emotional arousal.
* **Mobile Phone**: Acts as the central hub, collecting data from the EEG and smartwatch, running the neural inference model, and providing a user-facing interface.
* **Backend (Flask)**: Manages communication between the mobile app and the database, handling data processing, API endpoints, and user/session management.
* **Database (MySQL)**: Stores user data and metadata, supporting authentication and role-based access control.

# Installation
For the application to function correctly, all three main components must be installed:

* **Mobile App (Android)**
* **Wearable Companion App (Wear OS)**
* **Flask Backend Server (Python)**

The first two components (mobile and wearable apps) can be installed by building from source using Android Studio.

The Flask server must be set up separately by running the Python backend locally or on a remote host. Detailed instructions for each installation method are provided below.

## Building from Source (Android Studio)
We recommend using Android Studio to modify, run, or compile the source code of both the mobile and wearable applications.

1. **Clone the repository**:

   ```bash
   git clone https://github.com/Dimerin/Foodback.git
   cd Foodback
   ```
2. **Open the project in Android Studio**.
3. Choose the module to build:
   * `:app` for the Android app
   * `:wear` for the Wear OS companion app
4. Connect your devices via USB or Wi-Fi.
5. Press ▶️ **Run**, or generate APKs via **Build > Build Bundle(s) / APK(s)**.

> N.B. Make sure to update the Flask server IP address in the app’s build.gradle file to match the backend host.

## Flask Server
* Clone the project repository:

  ```bash
  git clone https://github.com/Dimerin/Foodback.git
  cd Foodback
  ```

* Create a `.env` file in `./Application/src/backend`:

  ```env
    DB_HOST=db
    DB_USER=user
    DB_PASSWORD=<password>
    DB_NAME=foodback_database
    SECRET_KEY=<secret_key>
    JWT_SECRET_KEY=<jwt_secret_key>
  ```

* Start the application:

  ```bash
  docker-compose up --build
  ```

* Common management commands:

  * Rebuild and start: `docker-compose up --build`
  * Start without rebuild: `docker-compose up`
  * Stop containers: `docker-compose down`
  * View logs: `docker-compose logs -f`


# Usage
The application can be used in two main modes, depending on the context:

* **🧠 Data Collection Mode**
  For structured data collection using a guided tasting protocol. This mode is used to build or update the machine learning model.

* **⚡ Inference Mode**
  For real-time evaluation of food experiences using a pre-trained model. This mode is intended for end users.

## 🧠 Data Collection Mode
In **Data Collection Mode**, the application guides the user through a structured protocol to collect synchronized physiological and neural signals during food tasting. The protocol consists of the following steps:
* **Device Initialization**:
  The protocol begins once the **EEG headset** and the **wearable watch** (recording **heart rate** and **electrodermal activity**) are properly connected and actively transmitting data.

* **Preparation Phase (5 seconds)**:
  A **first beep** signals the subject to bring the food sample into their mouth.
  This 5-second window allows the subject to prepare for tasting.

* **Recording Phase (10 seconds)**:
  A **second beep** marks the beginning of the data recording session.
  For the next **10 seconds**, the system captures:

  * EEG signals (brain activity)
  * Heart rate (HR)
  * Electrodermal activity (EDA)
  
  These signals represent the subject’s physiological and emotional response to the food.

* **Rating Phase**:
  A **final beep** indicates the end of the recording.
  A **rating interface** appears, allowing the **administrator** to enter a subjective evaluation of the subject’s experience, using a rating scale (e.g., 1-5 stars).

All phases are guided through the user interface, with visual cues and audio signals to assist both the subject and the administrator.


<img src="Readme Resources/images/TastingProtocol.webp" alt="Title" style="border-radius: 15px;"/></br>
<img src="Readme Resources/images//TastingProtocol2.webp" alt="Title" style="border-radius: 15px;"/></br>

## ⚡ Inference Mode
In this mode, the data collected from the sensors is immediately processed for inference, generating a score from the pre-trained model. All computations are performed directly on the phone.

<img src="Readme Resources/images/Evaluation Protocol.webp" alt="Title" style="border-radius: 15px;"/></br>

# Result & Evaluation
The study involved 10 volunteers (aged 16–56) with 45 tasting sessions, generating labeled EEG and smartwatch data paired with self-reported ratings. This multimodal dataset was used to train and validate the model over 1000 epochs with 10% validation split.

Two evaluation strategies were tested:

* A **session-level split** (80/20), keeping entire sessions exclusive to train or test sets, achieved about **33.33%** accuracy.
* An **epoch-level split** randomly divided 2-second epochs regardless of sessions, yielding higher accuracy (\~**73.33%**).

The likely motivation behind this discrepancy is information leakage in the epoch-level split, caused by overlap of session-specific signals between training and test sets, which artificially inflates performance. Therefore, only the session-level split provides a reliable estimate of the model’s true generalization ability.

# License
This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).

You are free to use, modify, and distribute this software under the terms of the GPLv3.
Please see the `LICENSE` file for the full license text.

# Contributors
This project was developed as part of a university course assignment at University of Pisa. Here are the members of the team:

  <a href="https://github.com/Califfo8" target="_blank">
    <img src="https://github.com/Califfo8.png" alt="Tommaso Califano GitHub avatar" width= "40px" height="40px">
  <a href="https://github.com/giovanniligato" target="_blank">
    <img src="https://github.com/giovanniligato.png" alt="Giovanni Ligato GitHub avatar"  width= "40px" height="40px">
  <a href="https://github.com/nicorama06" target="_blank">
    <img src="https://github.com/nicorama06.png" alt="Nicola Ramacciotti GitHub avatar"  width= "40px" height="40px">
  </a>
  <a href="https://github.com/Dimerin" target="_blank">
    <img src="https://github.com/Dimerin.png" alt="Gabriele Suma GitHub avatar"  width= "40px" height="40px">
  </a>