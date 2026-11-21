plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.successtudy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.successtudy"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}


dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    
    // Room Database for local data
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    
    // Gson for JSON serialization
    implementation(libs.gson)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Email (JavaMail for SMTP)
    implementation("javax.mail:mail:1.4.7")
    implementation("javax.activation:activation:1.1.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}