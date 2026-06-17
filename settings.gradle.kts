rootProject.name = "MediaPod"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
