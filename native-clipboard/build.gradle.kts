plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nna)
}

val hostOs: String = System.getProperty("os.name")
val hostArch: String = System.getProperty("os.arch")
val isWindows = hostOs.startsWith("Windows")

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    val nativeTarget = when {
        isWindows -> mingwX64 {
            binaries.all {
                linkerOpts("-luser32", "-lkernel32")
            }
        }
        hostOs == "Linux" && hostArch.contains("aarch64") -> linuxArm64()
        hostOs == "Linux" -> linuxX64()
        hostOs == "Mac OS X" && hostArch.contains("aarch64") -> macosArm64()
        hostOs == "Mac OS X" -> macosX64()
        else -> error("Unsupported host OS: $hostOs ($hostArch)")
    }

    jvm()

    if (!isWindows) {
        sourceSets.named("${nativeTarget.name}Main") {
            kotlin.srcDir("src/stubNativeMain/kotlin")
        }
    }
}

kotlinNativeExport {
    nativeLibName = "aerodlclipboard"
    nativePackage = "io.github.kdroidfilter.ytdlpgui.nativeclipboard"
    buildType = "release"
}
