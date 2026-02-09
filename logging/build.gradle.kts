plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.sentry.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
