import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("kotlin-android")
}

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val buildTypeRelease = "release"

android {
    compileSdkVersion(28)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 212
        versionName = "2.1.2"
        consumerProguardFiles("consumer-proguard-rules.pro")
    }
    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
    // If the keystore file exists
    if (keystorePropertiesFile.exists()) {
        // Initialize a new Properties() object called keystoreProperties.
        val keystoreProperties = Properties()

        // Load your keystore.properties file into the keystoreProperties object.
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

        signingConfigs {
            create(buildTypeRelease) {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
    }
    buildTypes {
        getByName(buildTypeRelease) {
            if (keystorePropertiesFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    lintOptions.isAbortOnError = false
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("com.google.android.material:material:1.0.0")
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
