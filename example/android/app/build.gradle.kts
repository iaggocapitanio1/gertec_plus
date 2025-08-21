import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}
val keystorePropsFile = rootProject.file("key.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}
android {
    namespace = "com.example.gertec_plus_example"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.example.gertec_plus_example"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 23
        targetSdk = 30
        versionCode = flutter.versionCode
        versionName = flutter.versionName

    }
    lint {
        // opcional manter:
        checkReleaseBuilds = false
        abortOnError = false
    }
    signingConfigs {
        create("enhanced") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("enhanced")
                ?: signingConfigs.getByName("debug")
        }
        getByName("debug") {
            signingConfig = signingConfigs.findByName("enhanced")
                ?: signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}