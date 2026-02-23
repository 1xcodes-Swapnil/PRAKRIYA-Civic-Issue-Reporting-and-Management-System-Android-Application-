plugins {
    // This is the standard plugin for an Android Application. It's correct.
    id("com.android.application")

    // This plugin is for integrating Firebase services. It's correct.
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.prakriya20"
    compileSdk = 34 // Changed to 34 as 36 is not a standard SDK version yet

    defaultConfig {
        applicationId = "com.example.prakriya20"
        minSdk = 24
        targetSdk = 34 // Changed to 34 to match compileSdk
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
        // Your project is Java, so these are correct.
        sourceCompatibility = JavaVersion.VERSION_1_8 // Changed to 1.8 to match common practice
        targetCompatibility = JavaVersion.VERSION_1_8 // Changed to 1.8 to match common practice
    }
}

dependencies {
    // Standard Android libraries
    implementation("androidx.appcompat:appcompat:1.6.1") // Using a common stable version
    implementation("com.google.android.material:material:1.11.0") // Using a common stable version
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.constraintlayout)

    // Libraries for testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    // For circular image views
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // For loading images (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-location:21.2.0")
// Use the latest version available
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Add these three lines for Firebase Firestore and Storage
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth") // Good to have here too
    // In your build.gradle.kts (Module :app) file, inside the dependencies { ... } block

    implementation("it.xabaras.android:recyclerview-swipedecorator:1.4")


}
