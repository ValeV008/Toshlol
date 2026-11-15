import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.nio.file.Path
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

val releaseStoreFile: String? = providers.environmentVariable("FINANCE_RELEASE_STORE_FILE").orNull
val releaseStorePassword: String? = providers.environmentVariable("FINANCE_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias: String? = providers.environmentVariable("FINANCE_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword: String? = providers.environmentVariable("FINANCE_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning: Boolean =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

val validateReleaseSigning: TaskProvider<Task> =
    tasks.register("validateReleaseSigning") {
        group = "verification"
        description = "Checks that a private release keystore and credentials are configured."
        doLast {
            check(hasReleaseSigning) {
                "Set FINANCE_RELEASE_STORE_FILE, FINANCE_RELEASE_STORE_PASSWORD, FINANCE_RELEASE_KEY_ALIAS, and FINANCE_RELEASE_KEY_PASSWORD before assembling a release APK."
            }
            val releaseKeystore: File = file(requireNotNull(releaseStoreFile)).canonicalFile
            val repositoryRoot: Path = rootProject.projectDir.canonicalFile.toPath()
            check(releaseKeystore.isFile) {
                "FINANCE_RELEASE_STORE_FILE must point to an existing keystore outside the repository."
            }
            check(!releaseKeystore.toPath().startsWith(repositoryRoot)) {
                "FINANCE_RELEASE_STORE_FILE must point to an existing keystore outside the repository."
            }
        }
    }

tasks.matching { it.name == "packageRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
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
            if (hasReleaseSigning) {
                signingConfig =
                    signingConfigs.create("localRelease") {
                        storeFile = file(requireNotNull(releaseStoreFile))
                        storePassword = requireNotNull(releaseStorePassword)
                        keyAlias = requireNotNull(releaseKeyAlias)
                        keyPassword = requireNotNull(releaseKeyPassword)
                    }
            }
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
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.android.test.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.android.test.core)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.android.test.uiautomator)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.room.test)

    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
