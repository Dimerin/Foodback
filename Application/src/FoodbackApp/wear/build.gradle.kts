plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "unipi.msss.foodback"
    compileSdk = 35

    defaultConfig {
        applicationId = "unipi.msss.foodback"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // Core Dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core)
    implementation(libs.androidx.navigation.compose.jvmstubs)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core.v190)
    implementation(libs.kotlinx.coroutines.android)

    // Material Design Dependencies
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.material3)

    // Wearable Dependencies
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.constraintlayout)

    // Testing Dependencies
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Animation (Lottie)
    implementation ("com.airbnb.android:lottie:6.6.6")
    implementation("com.airbnb.android:lottie-compose:4.0.0")

    implementation(libs.androidx.appcompat)
}