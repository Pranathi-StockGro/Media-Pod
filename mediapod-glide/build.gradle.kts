plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.stockgro.mediapod.glide"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":mediapod-core"))
            }
        }
        androidMain {
            dependencies {
                api(libs.glide.core)
                implementation(libs.glide.okhttp)
                implementation(libs.kotlinx.io.okio)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.glide.ksp)
}
