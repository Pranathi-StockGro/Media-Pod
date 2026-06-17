plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.stockgro.mediapod.coil.compose"
        compileSdk = 37
        minSdk = 24
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":mediapod-coil"))
                api(project(":mediapod-compose"))
                api(libs.coil.compose)
            }
        }
    }
}
