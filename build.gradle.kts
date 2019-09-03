buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath(kotlin("gradle-plugin", "1.3.50"))
        classpath("com.github.dcendents:android-maven-gradle-plugin:2.1")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks {
    named<Wrapper>("wrapper") {
        gradleVersion = "5.6.1"
        distributionType = Wrapper.DistributionType.ALL
    }

    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
