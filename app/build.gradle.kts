import java.util.Properties
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.URL
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.io.InputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties()
try {
    localProperties.load(FileInputStream(rootProject.file("local.properties")))
} catch (e: FileNotFoundException) {
    // Ignore if not present
}

android {
    namespace = "com.onestopshop"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.onestopshop"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${localProperties.getProperty("GITHUB_CLIENT_ID", "")}\"")
        buildConfigField("String", "GITHUB_CLIENT_SECRET", "\"${localProperties.getProperty("GITHUB_CLIENT_SECRET", "")}\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.apache.commons:commons-compress:1.24.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.register("downloadAssets") {
    val assetsDir = file("src/main/assets")
    val prootFile = file("src/main/assets/proot")
    val rootfsFile = file("src/main/assets/ubuntu-rootfs.tar.gz")

    doLast {
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }

        // Check if proot exists and is not empty
        if (!prootFile.exists() || prootFile.length() < 1024) {
            println("Downloading proot binary...")
            val prootUrl = URL("https://github.com/proot-me/proot/releases/download/v5.3.0/proot-v5.3.0-aarch64-static")
            prootUrl.openStream().use { input: InputStream ->
                FileOutputStream(prootFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }
        }

        // Check if rootfs exists and is not empty/dummy
        if (!rootfsFile.exists() || rootfsFile.length() < 1024) {
            println("Downloading Ubuntu rootfs...")
            // Follow redirects properly for CD image
            var connection = URL("http://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.4-base-arm64.tar.gz").openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            var redirectCount = 0
            while (connection.responseCode / 100 == 3 && redirectCount < 5) {
                val newUrl = connection.getHeaderField("Location")
                connection = URL(newUrl).openConnection() as HttpURLConnection
                redirectCount++
            }
            connection.inputStream.use { input: InputStream ->
                FileOutputStream(rootfsFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadAssets")
}
