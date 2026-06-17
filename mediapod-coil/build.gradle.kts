plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "com.stockgro.mediapod.coil"
        compileSdk = 37
        minSdk = 24
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":mediapod-core"))
                api(libs.coil.core)
                api(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
                implementation(libs.coil.network.cache.control)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.coil.network.okhttp)
            }
        }
        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
