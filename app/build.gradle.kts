plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

}

val releaseSigningFile = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
fun signingSecret(name: String): String = providers.environmentVariable(name).orNull
    ?.takeIf { it.isNotBlank() } ?: error("Missing release signing environment variable: $name")

android {
    namespace = "com.waypointshift"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        // New app ID avoids stale OEM Settings filtering for the earlier package.
        applicationId = "com.waypointshift.location"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        if (releaseSigningFile.isPresent) {
            create("release") {
                storeFile = file(releaseSigningFile.get())
                storeType = "PKCS12"
                storePassword = signingSecret("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingSecret("ANDROID_KEY_ALIAS")
                keyPassword = signingSecret("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningFile.isPresent) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

val validateReleaseSigning by tasks.registering {
    doLast {
        check(releaseSigningFile.isPresent) { "Release builds require ANDROID_KEYSTORE_PATH and signing credentials." }
        check(file(releaseSigningFile.get()).isFile) { "Release keystore file does not exist." }
        providers.environmentVariable("RELEASE_TAG").orNull?.let { tag ->
            check(tag == "v${android.defaultConfig.versionName}") {
                "Release tag must match app versionName: v${android.defaultConfig.versionName}"
            }
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(validateReleaseSigning)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.osmdroid.android)
    implementation(libs.google.play.services.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
