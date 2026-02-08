plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("io.github.kdroidfilter.manifest.MainKt")
        }
    }

    sourceSets {
        jvmMain.dependencies {
            // For the ReleaseManifest model
            implementation(project(":ytdlp"))

            // For GitHubReleaseFetcher (used only in the generator)
            implementation(libs.platformtools.releasefetcher)

            // Network module (Ktor HttpClient)
            implementation(project(":network"))

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)

            // Coroutines
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}
