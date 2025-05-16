# MindRove SDK Android v2.0 Documentation

## Introduction

The MindRove SDK v2.0 for Android enables seamless integration with MindRove devices, providing tools for managing server communication, processing sensor data, and handling device instructions. This version is compatible with MindRove devices sold before November 2023. For newer devices please refer to official upated [repository](https://github.com/MindRove/MindRoveSDK/tree/main) 

## Structure

```
mylibrary
    ↪ mindrove
        ↪ ServerManager
        ↪ ServerThread
        ↪ SensorData
        ↪ Instruction
```

### SensorData Class

The `SensorData` class in the `mylibrary.mindrove` package is a data class that represents sensor data. It contains the following properties:

- `SensorData.channel{1-8}`  
  - **Type**: Double  
  - Voltage measured on each (1–8) EEG channel (in microvolts)

- `SensorData.acceleration{X|Y|Z}`  
  - **Type**: Int  
  - Accelerometer data corresponding to the three axes (X, Y, Z)

- `SensorData.angularRate{X|Y|Z}`  
  - **Type**: Int  
  - Gyroscope data corresponding to the three axes (X, Y, Z)

- `SensorData.voltage`  
  - **Type**: UInt  
  - Battery voltage measured [mV]

- `SensorData.trigger`  
  - **Type**: UInt  
  - Trigger events:  
    - 0 — None  
    - 1 — Beep trigger  
    - 2 — Boop trigger

- `SensorData.numberOfMeasurement`  
  - **Type**: UInt  
  - Packet identifier

- `SensorData.impedance1ToDRL`  
  - **Type**: Int  
  - Magnitudes of impedance measured between pairs of electrodes [Ω]  
    - (1ToDRL, 3ToDRL, RefToDRL, RefTo4, 1To2, 2To3, 3To4, 5To4, 5To6, 6ToRef)

### ServerManager Class

The `ServerManager` class is responsible for managing a server thread and its interactions.

- `ServerManager.sendInstruction`  
  - Sending instructions to the client  
  - Expecting `Instruction`

- `ServerManager.start/stop`  
  - Starting and stopping the server thread

- `ServerManager.isMessageReceived`  
  - Check if a message has been received

- `ServerManager.ipAddress`  
  - IP address of the server

### Instruction Enum

The `Instruction` is an enum class for different types of instructions:

- `Instruction.BEEP` — for Beep trigger  
- `Instruction.BOOP` — for Boop trigger  
- `Instruction.EEG` — for EEG mode  
- `Instruction.IMP` — for impedance mode  
- `Instruction.TEST` — for generating test signals

### ServerThread Class

The `ServerThread` class is a thread for the server, the whole class is managed by the `ServerManager`.

## How to Use

### Importing `.aar` file to new Android Studio project

1. Add `.aar` file to project's `libs` folder (`project\app\libs`)  
   [Reference](https://developer.android.com/studio/projects/android-library)

2. `build.gradle`:
   ```kotlin
   implementation(files("libs/mindRove-release_v2_0.aar"))
   implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    ```

3. Import classes:

   ```kotlin
   import mylibrary.mindrove.Instruction
   import mylibrary.mindrove.SensorData
   import mylibrary.mindrove.ServerManager
   ```

4. Add necessary network permissions in your `AndroidManifest.xml` file:

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

5. To write data to external storage:

   ```xml
   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   ```

6. For live data:

   ```kotlin
   implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
   implementation("androidx.compose.runtime:runtime:1.6.1")
   ```

The `INTERNET` permission is needed for network communication with the MindRove device, and the `WRITE_EXTERNAL_STORAGE` permission is needed to write sensor data to external storage.

### Getting Started with Code

> The Android device needs to be connected to the MindRove device via Wi-Fi before launching the app!

1. Import the necessary classes from the library:

   ```kotlin
   import mylibrary.mindrove.SensorData
   import mylibrary.mindrove.ServerManager
   ```

2. Create an instance of `ServerManager` and provide a callback function that will be called when new data is received. The callback function takes a `SensorData` object as a parameter:

   ```kotlin
   private val serverManager = ServerManager { sensorData: SensorData ->
       // Handle the received data here
   }
   ```

3. Start the `ServerManager` when a network connection is available:

   ```kotlin
   serverManager.start()
   ```

4. Stop the `ServerManager` when the activity is destroyed to clean up resources:

   ```kotlin
   serverManager.stop()
   ```

### Example Code in Kotlin

```kotlin
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager

class MainActivity : ComponentActivity() {
    private val serverManager = ServerManager { sensorData: SensorData ->
        // Update the sensor data text
        sensorDataText.postValue(sensorData.channel1.toString())
    }

    private val sensorDataText = MutableLiveData("No data yet")
    private val networkStatus = MutableLiveData("Checking network status...")
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var isServerManagerStarted = false
    private var isWifiSettingsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            val isNetworkAvailable = isNetworkAvailable()
            if (!isNetworkAvailable) {
                networkStatus.value = "No network connection. Please enable Wi-Fi."
                if (!isWifiSettingsOpen) {
                    openWifiSettings()
                    isWifiSettingsOpen = true
                }
            } else {
                networkStatus.value = "Connected to the network."
                isWifiSettingsOpen = false

                if (!isServerManagerStarted) {
                    serverManager.start()
                    isServerManagerStarted = true
                }
            }
            handler.postDelayed(runnable, 3000)
        }

        handler.post(runnable)

        setContent {
            Try2_0Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val networkStatusValue by networkStatus.asFlow().collectAsState(initial = "Checking network status...")
                    val sensorDataTextValue by sensorDataText.asFlow().collectAsState(initial = "No data yet")

                    Column {
                        Text(text = networkStatusValue)
                        Text(text = sensorDataTextValue)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        serverManager.stop()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private val wifiSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isWifiSettingsOpen = false
    }

    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        wifiSettingsLauncher.launch(intent)
    }
}
```