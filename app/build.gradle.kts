plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
}

val STORE_FILE: String? = project.findProperty("MYAPP_UPLOAD_STORE_FILE") as String?
val STORE_PASSWORD: String? = project.findProperty("MYAPP_UPLOAD_STORE_PASSWORD") as String?
val KEY_ALIAS: String? = project.findProperty("MYAPP_UPLOAD_KEY_ALIAS") as String?
val KEY_PASSWORD: String? = project.findProperty("MYAPP_UPLOAD_KEY_PASSWORD") as String?

android {
    namespace = "com.example.antibully"
    compileSdk = 35

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.antibully"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (STORE_FILE != null && STORE_PASSWORD != null && KEY_ALIAS != null && KEY_PASSWORD != null) {
                storeFile = file(STORE_FILE!!)
                storePassword = STORE_PASSWORD!!
                keyAlias = KEY_ALIAS!!
                keyPassword = KEY_PASSWORD!!
            }
        }
    }



    buildTypes {
        getByName("debug") {
            // Emulator/dev
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000\"")
        }
        getByName("release") {
            buildConfigField("String", "BASE_URL", "\"http://193.106.55.138:3000/\"")

            // Start with no shrinking to avoid surprises; we can turn these on later.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}


dependencies {
    // --- AndroidX core / UI ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)

    // Compose (you had BOM already)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Navigation (use ONE version: 2.8.9 from your catalog)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Media3
    implementation(libs.androidx.media3.common.ktx)

    // Material (single source — your catalog is 1.12.0)
    implementation(libs.material)

    // --- Firebase (use BoM; do not pin per-artifact versions) ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.google.firebase.storage.ktx) // or libs.firebase.storage.ktx (either is fine, you had both entries)

    // --- Networking ---
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)            // 2.9.0 from catalog
    implementation(libs.converter.gson)      // 2.9.0 from catalog
    implementation("com.google.code.gson:gson:2.10.1") // ok to keep, used beyond Retrofit

    // --- Room (kapt) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // --- Images ---
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.12.0")
    implementation(libs.picasso.v28)

    // --- Charts ---
    implementation(libs.mpandroidchart)

    // --- ZXing (your 'core' alias) ---
    implementation(libs.core)

    // --- Coroutines (unified) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // --- DataStore for notification prefs ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Play Services Auth ---
    implementation(libs.play.services.auth)

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
