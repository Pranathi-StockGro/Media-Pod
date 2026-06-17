import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.atomicfu.kotlinx)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    android {
        namespace = "com.stockgro.mediapod.core"
        compileSdk = 37
        minSdk = 24
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.atomicfu)
                api(libs.compose.ui)
                api(libs.compose.runtime)
                api(libs.compose.foundation)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
            }
        }
    }
}

publishing {
    val props = gradleLocalProperties(rootDir, providers)
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/Pranathi-StockGro/Media-Pod")
            credentials {
                username = props.getProperty("githubPackagesUsername")
                password = props.getProperty("githubPackagesPassword")
            }
        }
    }
}

mavenPublishing {

    // Configure POM metadata for the published artifact
    pom {
        name.set("MediaPod")
        description.set("Library for loading and displaying media content using coil for compose")
        inceptionYear.set("2026")
        url.set("https://maven.pkg.github.com/Pranathi-StockGro/Media-Pod")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        // Specify developers information
        developers {
            developer {
                id.set("Pranathi-StockGro")
                name.set("Pranathi")
                email.set("pranathi.pellakuru@stockgro.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://maven.pkg.github.com/Pranathi-StockGro/Media-Pod")
        }
    }
}

