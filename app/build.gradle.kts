plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.bloombooth"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bloombooth"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation(libs.places)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-firestore:24.0.2")

    // AndroidX Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // UI Libraries
    implementation(libs.material)
    implementation(libs.picasso)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.cloudinary.android)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation(platform("org.mockito:mockito-core:5.14.2"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.runner.v152)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.fragment.testing)
    androidTestImplementation(libs.mockito)
}