import java.util.Properties

rootProject.name = "MediaPod"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val localPropertiesFile = settingsDir.resolve("local.properties")
val localProperties = Properties()

// 2. Safely read the file if it exists
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            name = "gitHubPackages"
            url = uri("https://maven.pkg.github.com/Pranathi-StockGro/Anchor")
            credentials {
                username = localProperties.getProperty("githubPackagesUsername")
                password = localProperties.getProperty("githubPackagesPassword")
            }
        }
//        mavenLocal()
    }
}

include(":androidApp")
include(":shared")
include(":androidsampleapp")

include(":mediapod-core")
include(":mediapod-coil")
include(":mediapod-glide")
include(":mediapod-compose")
include(":mediapod-view")
include(":coil-compose")
include(":coil-view")
include(":glide-compose")
include(":glide-view")
