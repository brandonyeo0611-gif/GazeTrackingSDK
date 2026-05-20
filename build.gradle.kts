plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.gazetrackingsdk"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    aaptOptions {
        noCompress += "tflite"
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.opencsv:opencsv:5.8")
    // to write csv
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // TensorFlow Lite runtime
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
// TensorFlow Lite Task Library (includes vision, audio, text tasks)
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")

    // CameraX dependencies
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")

    // Guava is needed for ListenableFuture in Java
    implementation("com.google.guava:guava:31.1-android")

}