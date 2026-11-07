plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tikdl"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tikdl"
        minSdk = 29
        targetSdk = 34
        versionCode = 31
        versionName = "3.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
