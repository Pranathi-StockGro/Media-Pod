import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    android {
        namespace = "com.stockgro.mediapod.glide.compose"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":mediapod-glide"))
                api(project(":mediapod-compose"))
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
        description.set("Library for loading and displaying media content using glide for compose only for android")
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
