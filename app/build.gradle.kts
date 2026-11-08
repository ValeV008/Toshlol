plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.toshlol.personalfinance"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.toshlol.finance"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets
        .getByName("androidTest")
        .assets.directories
        .add("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.graphics)
    implementation(libs.compose.preview)
    implementation(libs.compose.material)
    implementation(libs.material)
    implementation(libs.datastore)
    implementation(libs.room.runtime)
    implementation(libs.room.coroutines)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.android.test.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.android.test.core)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.room.test)

    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
