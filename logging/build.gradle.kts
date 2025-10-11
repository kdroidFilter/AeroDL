plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            // No external deps; simple console logger
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

