import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "br.edu.uniceplac.modapraia"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.edu.uniceplac.modapraia"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    signingConfigs {
        create("production") {
            val signingFile = rootProject.file("signing.properties")
            if (signingFile.exists()) {
                val values = Properties().apply { signingFile.inputStream().use { load(it) } }
                storeFile = file(values.getProperty("storeFile"))
                storePassword = values.getProperty("storePassword")
                keyAlias = values.getProperty("keyAlias")
                keyPassword = values.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            if (rootProject.file("signing.properties").exists()) signingConfig = signingConfigs.getByName("production")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("junit:junit:4.13.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
