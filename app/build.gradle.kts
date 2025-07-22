import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.databoss.aag"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.databoss.aag"
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
//    kotlinOptions {
//        jvmTarget = "11"
//    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {

    implementation("com.github.gkonovalov.android-vad:webrtc:2.0.10")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}

// **************
// Gemini 2.5 Pro: webrtc-vad github reposu dependency olarak eklendiğinde aldığım build hatasının sebebi ve çözümü için gerekli blok
// **************
// Your project is configured to use a Kotlin compiler that expects metadata version 2.0.0,
// but the android-vad library (or one of its dependencies) is pulling in the Kotlin standard library version 2.2.0.
// This incompatibility leads to an internal compiler error.
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                // Use the version defined by your Kotlin Gradle Plugin
                useVersion(libs.versions.kotlin.get())
                // Or hardcode it if you don't use a version catalog:
                // useVersion("2.0.0")
            }
        }
    }
}