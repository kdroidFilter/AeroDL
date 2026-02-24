plugins {
    `java`
    alias(libs.plugins.graalvmNative)
}

val isMac = org.gradle.internal.os.OperatingSystem.current().isMacOsX
val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
val isLinux = org.gradle.internal.os.OperatingSystem.current().isLinux

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
    }
}

sourceSets {
    main {
        resources.srcDir(
            when {
                isMac -> "src/main/resources-macos"
                isWindows -> "src/main/resources-windows"
                isLinux -> "src/main/resources-linux"
                else -> throw GradleException("Unsupported OS")
            }
        )
    }
}

dependencies {
    implementation(project(":composeApp"))
}

val javaHomeDir = provider { org.gradle.internal.jvm.Jvm.current().javaHome.absolutePath }

val nativeImageConfigDir = layout.projectDirectory.dir(
    when {
        isMac -> "src/main/resources-macos/META-INF/native-image"
        isWindows -> "src/main/resources-windows/META-INF/native-image"
        isLinux -> "src/main/resources-linux/META-INF/native-image"
        else -> throw GradleException("Unsupported OS")
    }
)

tasks.register<JavaExec>("runWithAgent") {
    description = "Run the app with the native-image-agent to collect reflection metadata"
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.kdroidfilter.ytdlpgui.MainKt")
    jvmArgs(
        "-agentlib:native-image-agent=config-output-dir=${nativeImageConfigDir.asFile.absolutePath}"
    )
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
    })
}

graalvmNative {
    toolchainDetection.set(false)
    binaries {
        named("main") {
            mainClass.set("io.github.kdroidfilter.ytdlpgui.MainKt")
            imageName.set("aerodl")
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
                    })
            buildArgs.addAll(
                "-march=native",
                "-H:+AddAllCharsets",
                "-Djava.awt.headless=false",
                "-Os",
                "-H:-IncludeMethodData",
            )
            if (isMac) {
                buildArgs.add("-H:NativeLinkerOption=-Wl,-U,_Java_java_awt_Cursor_finalizeImpl")
            }
            if (isLinux) {
                buildArgs.add("-H:NativeLinkerOption=-Wl,-rpath,\$ORIGIN")
            }
            if (isWindows) {
                buildArgs.addAll(
                    "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS",
                    "-H:NativeLinkerOption=/ENTRY:mainCRTStartup",
                )
            }
            resources.autodetect()
        }
    }
}

// ── macOS packaging: .app bundle ──

val appBundleDir = layout.buildDirectory.dir("native/AeroDl.app/Contents")

if (isMac) {
    tasks.register<Copy>("copyBinaryToApp") {
        description = "Copy native binary into .app bundle"
        group = "build"
        dependsOn("nativeCompile")
        from(layout.buildDirectory.file("native/nativeCompile/aerodl"))
        into(appBundleDir.map { it.dir("MacOS") })
    }

    tasks.register<Copy>("copyAwtDylibs") {
        description = "Copy AWT dylibs into .app bundle"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/lib") {
            include(
                "libawt.dylib", "libawt_lwawt.dylib", "libfontmanager.dylib",
                "libfreetype.dylib", "libjava.dylib", "libjavajpeg.dylib",
                "libjawt.dylib", "liblcms.dylib", "libmlib_image.dylib",
                "libosxapp.dylib", "libsplashscreen.dylib",
            )
        }
        from("${javaHomeDir.get()}/lib/server") {
            include("libjvm.dylib")
        }
        into(appBundleDir.map { it.dir("MacOS") })
    }

    tasks.register<Copy>("copyJawtToLib") {
        description = "Copy libjawt.dylib to lib/ subdir for Skiko"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/lib") {
            include("libjawt.dylib")
        }
        into(appBundleDir.map { it.dir("MacOS/lib") })
    }

    tasks.register<Exec>("stripDylibs") {
        description = "Strip debug symbols from dylibs to reduce size"
        group = "build"
        dependsOn("copyAwtDylibs")
        val macosDir = appBundleDir.map { it.dir("MacOS") }
        commandLine("bash", "-c", "strip -x ${macosDir.get().asFile.absolutePath}/*.dylib")
    }

    tasks.register<Exec>("codesignDylibs") {
        description = "Re-sign dylibs after stripping (ad-hoc)"
        group = "build"
        dependsOn("stripDylibs")
        val macosDir = appBundleDir.map { it.dir("MacOS") }
        commandLine("bash", "-c", "codesign --force --sign - ${macosDir.get().asFile.absolutePath}/*.dylib")
    }

    tasks.register<Exec>("fixRpath") {
        description = "Add @executable_path rpath to native image"
        group = "build"
        dependsOn("copyBinaryToApp")
        val binary = appBundleDir.map { it.file("MacOS/aerodl") }
        commandLine("install_name_tool", "-add_rpath", "@executable_path/.", binary.get().asFile.absolutePath)
        isIgnoreExitValue = true
    }

    tasks.register<Copy>("copyInfoPlist") {
        description = "Copy Info.plist into .app bundle"
        group = "build"
        from(layout.projectDirectory.file("src/main/packaging/Info.plist"))
        into(appBundleDir)
    }

    tasks.register("packageNative") {
        description = "Build native image and package as macOS .app bundle"
        group = "build"
        dependsOn("copyBinaryToApp", "copyAwtDylibs", "copyJawtToLib", "stripDylibs", "codesignDylibs", "fixRpath", "copyInfoPlist")
    }
}

// ── Windows packaging: flat directory with DLLs ──

if (isWindows) {
    val windowsOutputDir = layout.buildDirectory.dir("native/aerodl")

    tasks.register<Copy>("copyBinaryToOutput") {
        description = "Copy native binary into output directory"
        group = "build"
        dependsOn("nativeCompile")
        from(layout.buildDirectory.file("native/nativeCompile/aerodl.exe"))
        into(windowsOutputDir)
    }

    tasks.register<Copy>("copyAwtDlls") {
        description = "Copy AWT DLLs into output directory"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/bin") {
            include(
                "awt.dll", "java.dll", "javajpeg.dll", "fontmanager.dll",
                "freetype.dll", "lcms.dll", "mlib_image.dll", "splashscreen.dll",
            )
        }
        into(windowsOutputDir)
    }

    tasks.register<Copy>("copyJvmDll") {
        description = "Copy jvm.dll into output directory"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/bin/server") {
            include("jvm.dll")
        }
        into(windowsOutputDir)
    }

    tasks.register<Copy>("copyJawtToBin") {
        description = "Copy jawt.dll to bin/ subdir for Skiko"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/bin") {
            include("jawt.dll")
        }
        into(windowsOutputDir.map { it.dir("bin") })
    }

    tasks.register<Copy>("copyJawtToNativeCompileBin") {
        description = "Copy jawt.dll to nativeCompile/bin/ for Skiko"
        group = "build"
        dependsOn("nativeCompile")
        from(layout.buildDirectory.file("native/nativeCompile/jawt.dll"))
        into(layout.buildDirectory.dir("native/nativeCompile/bin"))
    }

    tasks.named("nativeCompile") {
        finalizedBy("copyJawtToNativeCompileBin")
    }

    tasks.register("packageNative") {
        description = "Build native image and package with DLLs"
        group = "build"
        dependsOn("copyBinaryToOutput", "copyAwtDlls", "copyJvmDll", "copyJawtToBin")
    }
}

// ── Linux packaging: flat directory with .so ──

if (isLinux) {
    val linuxOutputDir = layout.buildDirectory.dir("native/aerodl")

    tasks.register<Copy>("copyBinaryToOutput") {
        description = "Copy native binary into output directory"
        group = "build"
        dependsOn("nativeCompile")
        from(layout.buildDirectory.file("native/nativeCompile/aerodl"))
        into(linuxOutputDir)
    }

    tasks.register<Copy>("copyAwtSoLibs") {
        description = "Copy AWT .so libs into output directory"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/lib") {
            include(
                "libawt.so", "libawt_headless.so", "libawt_xawt.so", "libfontmanager.so",
                "libjava.so", "libjavajpeg.so", "libjawt.so", "liblcms.so",
                "libmlib_image.so", "libsplashscreen.so",
            )
        }
        into(linuxOutputDir)
    }

    tasks.register<Copy>("copyJvmSo") {
        description = "Copy libjvm.so into output directory"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/lib/server") {
            include("libjvm.so")
        }
        into(linuxOutputDir)
    }

    tasks.register<Copy>("copyJawtToLib") {
        description = "Copy libjawt.so to lib/ subdir for Skiko"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/lib") {
            include("libjawt.so")
        }
        into(linuxOutputDir.map { it.dir("lib") })
    }

    tasks.register<Copy>("copyJawtToNativeCompileLib") {
        description = "Copy libjawt.so to nativeCompile/lib/ for Skiko"
        group = "build"
        dependsOn("nativeCompile")
        from("${javaHomeDir.get()}/lib") {
            include("libjawt.so")
        }
        into(layout.buildDirectory.dir("native/nativeCompile/lib"))
    }

    tasks.named("nativeCompile") {
        finalizedBy("copyJawtToNativeCompileLib")
    }

    tasks.register<Exec>("stripSoLibs") {
        description = "Strip debug symbols from .so libs to reduce size"
        group = "build"
        dependsOn("copyAwtSoLibs", "copyJvmSo")
        val outputDir = linuxOutputDir.get().asFile.absolutePath
        commandLine("bash", "-c", "strip --strip-debug ${outputDir}/*.so")
    }

    tasks.register("packageNative") {
        description = "Build native image and package with .so libs"
        group = "build"
        dependsOn("copyBinaryToOutput", "copyAwtSoLibs", "copyJvmSo", "copyJawtToLib", "stripSoLibs")
    }
}
