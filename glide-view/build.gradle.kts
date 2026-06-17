plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "com.stockgro.mediapod.glide.view"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":mediapod-glide"))
                api(project(":mediapod-view"))
            }
        }
    }
}
