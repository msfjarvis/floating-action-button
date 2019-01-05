import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "me.msfjarvis.floatingactionbutton.sample"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
    buildTypes {
	    getByName("release") {
	        signingConfig = signingConfigs.getByName("debug")
	        isMinifyEnabled = true
	        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
	    }
	}
}

dependencies {
    implementation(project(":library"))
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("com.google.android.material:material:1.0.0")
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
}
repositories {
    mavenCentral()
}