plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kleos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kleos"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes.configureEach {
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${project.findProperty("API_BASE_URL") ?: "http://10.0.2.2:3000/"}\""
        )
    }
}

dependencies {

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Copy custom burger menu icons from the img folder into res/drawable
tasks.register<Copy>("copyBurgerIcons") {
    val iconsDir = file("${rootDir}/img")
    from(iconsDir) {
        include("people_13645971.png")
        include("custom-clearance_10194178.png")
        include("smartphone_972170.png")
        include("pencil_5807539.png")
        include("emergency-exit_18615080.png")
        // Rename to valid Android resource names
        rename("people_13645971.png", "ic_profile_menu.png")
        rename("custom-clearance_10194178.png", "ic_partners.png")
        rename("smartphone_972170.png", "ic_support.png")
        rename("pencil_5807539.png", "ic_admission.png")
        rename("emergency-exit_18615080.png", "ic_logout.png")
    }
    into("src/main/res/drawable")
}

// Ensure icons are copied before resource merging
tasks.named("preBuild").configure {
    dependsOn("copyBurgerIcons")
}