import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Locale
import java.io.File as JFile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.hydraulicConveyor)
    alias(libs.plugins.metro)
}

val ref = System.getenv("GITHUB_REF") ?: ""
val version = if (ref.startsWith("refs/tags/")) {
    val tag = ref.removePrefix("refs/tags/")
    if (tag.startsWith("v")) tag.substring(1) else tag
} else "1.3.5"

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)


            // Androidx
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // UI libraries
            implementation(libs.compose.fluent)
            implementation(libs.compose.fluent.icons.extended)
            implementation(libs.composemediaplayer)

            // Coil
            implementation(libs.coil)
            implementation(libs.coil.network)

            // DI - Metro
            implementation(libs.metro.runtime)

            // Platform tools
            implementation(libs.platformtools.core)
            implementation(libs.platformtools.darkmodedetector)
            implementation(libs.platformtools.clipboardmanager)
            implementation(libs.platformtools.releasefetcher)
            implementation(libs.autolaunch)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Settings & Notifications
            implementation(libs.multiplatform.settings)
            implementation(libs.knotify)
            implementation(libs.knotify.compose)

            implementation(libs.confettikit)

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            // Desktop specific
            implementation(compose.desktop.currentOs)
            implementation(libs.composenativetray)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.cardiologist)

            implementation(libs.platformtools.appmanager)
            // no external markdown UI renderer; using lightweight parser

            // Project dependencies
            implementation(project(":ytdlp"))
            implementation(project(":network"))
            implementation(project(":logging"))

            // SQLDelight driver
            implementation(libs.sqlDelight.driver.sqlite)
        }
    }
}



compose.desktop {
    application {
        mainClass = "io.github.kdroidfilter.ytdlpgui.MainKt"

        val cleanInstall = project.findProperty("cleanInstall")?.toString()?.toBoolean() ?: false
        val debugLogs = project.findProperty("debugLogs")?.toString()?.toBoolean() ?: false
        jvmArgs += listOf(
            "-DcleanInstall=$cleanInstall",
            "-DdebugLogs=$debugLogs"
        )


        nativeDistributions {
            vendor = "KDroidFilter"
            targetFormats(TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AeroDl"
            packageVersion = version
            description = "An awesome GUI for yt-dlp!"
            modules("jdk.accessibility", "java.sql", "jdk.security.auth")
            windows {
                dirChooser = true
                menuGroup = "start-menu-group"
                iconFile.set(project.file("icons/logo.ico"))
                shortcut = true
                upgradeUuid = "ada57c09-11e1-4d56-9d5d-0c480f6968ec"
                perUserInstall = true
            }
            macOS {
                bundleID = "io.github.kdroidfilter.ytdlpgui"
                dockName = "AeroDl"
                iconFile.set(project.file("icons/logo.icns"))
            }
            linux {
                iconFile.set(project.file("icons/logo.png"))
            }
            buildTypes.release.proguard {
                isEnabled = true
                obfuscate.set(false)
                optimize.set(true)
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            // Database configuration here.
            // https://cashapp.github.io/sqldelight
            packageName.set("io.github.kdroidfilter.ytdlpgui.db")
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:${libs.versions.sqlDelight.get()}")
        }
    }
}

tasks.withType<Jar> {
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.EC")
}

// MSIX packaging task (Windows-only)
// Creates an MSIX from the Compose Desktop distributable, after the MSI is packaged.
// Usage: ./gradlew :composeApp:packageReleaseMsix (on Windows with Windows SDK installed)
tasks.register("packageReleaseMsix") {
    group = "distribution"
    description = "Packs a Windows MSIX using makeappx.exe after packageReleaseMsi"

    // Ensure the release distributable exists first
    dependsOn(tasks.named("createReleaseDistributable"))

    // Capture project-scoped values at configuration time (config-cache friendly)
    val capturedProjectDir: JFile = project.projectDir
    val capturedRootDir: JFile = project.rootDir
    val capturedBuildDir: JFile = project.buildDir
    val capturedProjectName: String = project.name
    val capturedVersion: String = version
    val capturedProjectIcon: JFile = JFile(capturedProjectDir, "icons/logo.png")

    doLast {
        val os = org.gradle.internal.os.OperatingSystem.current()
        if (!os.isWindows) {
            logger.lifecycle("packageReleaseMsix: Skipped (non-Windows OS)")
            return@doLast
        }

        // Discover makeappx.exe from Windows 10/11 SDK
        val programFilesX86 = System.getenv("ProgramFiles(x86)")
            ?: throw GradleException("ProgramFiles(x86) environment variable not found.")
        val windowsKitsDir = JFile(programFilesX86, "Windows Kits/10/bin")
        val makeAppxCandidates: List<JFile> = if (windowsKitsDir.exists()) {
            windowsKitsDir.walkTopDown()
                .filter { file: JFile -> file.name.equals("makeappx.exe", ignoreCase = true) }
                .toList()
        } else emptyList()

        fun sdkVersionScore(file: JFile): Long {
            val versionDir = file.parentFile?.parentFile?.name ?: return 0
            val nums = versionDir.split('.').mapNotNull { it.toIntOrNull() }
            return (nums.getOrNull(0) ?: 0).toLong() shl 48 or
                    (nums.getOrNull(1) ?: 0).toLong() shl 32 or
                    (nums.getOrNull(2) ?: 0).toLong() shl 16 or
                    (nums.getOrNull(3) ?: 0).toLong()
        }

        val x64Candidates = makeAppxCandidates.filter { it.parentFile.path.contains("x64", ignoreCase = true) }
        val preferredList = x64Candidates.ifEmpty { makeAppxCandidates }
        val makeAppxPath = preferredList.maxByOrNull { sdkVersionScore(it) }
            ?: throw GradleException("makeappx.exe not found in Windows Kits. Please install Windows 10/11 SDK.")

        logger.lifecycle("packageReleaseMsix: Using $makeAppxPath")

        // Compose Desktop release app dir (e.g., compose/binaries/main-release/app/AeroDl)
        val primaryBaseDir = JFile(capturedBuildDir, "compose/binaries/main-release/app")
        val fallbackBaseDir = JFile(capturedBuildDir, "compose/binaries/main/app")
        val baseReleaseDir = if (primaryBaseDir.exists()) primaryBaseDir else fallbackBaseDir

        // Prefer configured package name, otherwise auto-detect
        val configuredPackageName = "AeroDl" // keep in sync with nativeDistributions.packageName
        val candidateDir = JFile(baseReleaseDir, configuredPackageName)
        val appDir: JFile = when {
            candidateDir.exists() -> candidateDir
            baseReleaseDir.exists() -> baseReleaseDir.listFiles()?.firstOrNull { it.isDirectory }
                ?: throw GradleException("Release app directory not found under $baseReleaseDir")

            else -> throw GradleException("Release app directory base not found. Expected $primaryBaseDir or $fallbackBaseDir.")
        }

        // Prepare assets folder and manifest location
        val assetsDir = JFile(appDir, "assets")
        if (!assetsDir.exists()) assetsDir.mkdirs()

        // Provide basic icons by reusing the project icon if dedicated MSIX assets are absent
        val projectIcon = capturedProjectIcon
        val iconTargets = listOf(
            JFile(assetsDir, "icon_44.png"),
            JFile(assetsDir, "icon_50.png"),
            JFile(assetsDir, "icon_150.png"),
        )
        if (projectIcon.exists()) {
            iconTargets.forEach { target ->
                if (!target.exists()) {
                    projectIcon.copyTo(target, overwrite = true)
                }
            }
        } else {
            logger.warn("packageReleaseMsix: icons/logo.png not found; MSIX will reference assets that may be missing.")
        }

        // Determine exe and version
        val exeName = appDir.name + ".exe"
        val exeFile = JFile(appDir, exeName)
        if (!exeFile.exists()) {
            throw GradleException("Expected executable not found: $exeFile")
        }

        // Ensure 4-part numeric version for MSIX
        val baseVersion = capturedVersion
        val msixVersion = baseVersion.split('-')[0].let { v ->
            val parts = v.split('.')
            val padded = (parts + listOf("0", "0", "0", "0")).take(4)
            padded.joinToString(".")
        }
        logger.lifecycle("packageReleaseMsix: Resolved MSIX version: $msixVersion")

        // Read MSIX configuration from environment (for CI via GitHub Secrets)
        val msixIdentityName: String = System.getenv("MSIX_IDENTITY_NAME") ?: "KdroidFilter.AeroDL"
        val msixPublisher: String = System.getenv("MSIX_PUBLISHER") ?: "CN=D541E802-6D30-446A-864E-2E8ABD2DAA5E"
        val msixCertCN: String = System.getenv("MSIX_CERT_CN")
            ?: msixPublisher.removePrefix("CN=").trim()
        val msixPfxPassword: String = System.getenv("MSIX_PFX_PASSWORD") ?: "ChangeMe-Temp123!"

        fun createManifest(ver: String): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <Package
          xmlns="http://schemas.microsoft.com/appx/manifest/foundation/windows10"
          xmlns:uap="http://schemas.microsoft.com/appx/manifest/uap/windows10"
          xmlns:rescap="http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities"
          IgnorableNamespaces="uap rescap">

          <Identity
            Name="$msixIdentityName"
            Publisher="$msixPublisher"
            Version="$ver"
            ProcessorArchitecture="x64" />

          <Properties>
            <DisplayName>AeroDl</DisplayName>
            <PublisherDisplayName>KDroidFilter</PublisherDisplayName>
            <Logo>assets\icon_50.png</Logo>
          </Properties>

          <Dependencies>
            <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.17763.0" MaxVersionTested="10.0.22621.0" />
          </Dependencies>

          <Resources>
            <Resource Language="en" />
          </Resources>

          <Applications>
            <Application Id="App"
                         Executable="$exeName"
                         EntryPoint="Windows.FullTrustApplication">
              <uap:VisualElements
                DisplayName="AeroDl"
                Description="AeroDl"
                BackgroundColor="transparent"
                Square150x150Logo="icons\logo.png"
                Square44x44Logo="icons\logo.png">
              </uap:VisualElements>
            </Application>
          </Applications>

          <Capabilities>
            <Capability Name="privateNetworkClientServer"/>
            <rescap:Capability Name="runFullTrust"/>
          </Capabilities>
        </Package>
    """.trimIndent()

        val manifestFile = JFile(appDir, "AppxManifest.xml")
        manifestFile.writeText(createManifest(msixVersion))

        // Run makeappx pack
        val outputBase = baseReleaseDir.parentFile
        val msixDir = JFile(outputBase, "msix")
        if (!msixDir.exists()) msixDir.mkdirs()

        // Derive MSIX filename from the generated EXE filename (same base, .msix)
        val exeDir = JFile(outputBase, "exe")
        val exeFileName: String? = if (exeDir.exists()) {
            exeDir.listFiles()
                ?.firstOrNull { it.isFile && it.name.lowercase(Locale.getDefault()).endsWith(".exe") }?.name
        } else null
        val outputNameBase = exeFileName?.removeSuffix(".exe") ?: "${appDir.name}-${baseVersion}"
        val outputMsix = JFile(msixDir, "$outputNameBase.msix")

        val cmd = listOf(
            makeAppxPath.absolutePath,
            "pack", "/d", appDir.canonicalPath,
            "/p", outputMsix.canonicalPath,
            "/o"
        )
        logger.lifecycle("packageReleaseMsix: Running: ${cmd.joinToString(" ")}")
        val process = ProcessBuilder(cmd)
            .directory(makeAppxPath.parentFile)
            .redirectErrorStream(true)
            .start()
        val out = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (out.isNotBlank()) logger.lifecycle(out)
        if (code != 0) throw GradleException("makeappx.exe failed with exit code $code")

        logger.lifecycle("packageReleaseMsix: Created ${outputMsix.absolutePath}")

        // ---------------------------------------------------------------------
        // Create a companion PowerShell script that self-signs, signs the MSIX,
        // and installs it. This script lives next to the .msix output.
        // ---------------------------------------------------------------------

        // Best-effort locate signtool.exe now (also re-checked in the PS1 script)
        val signtoolCandidates = windowsKitsDir.walkTopDown()
            .filter { it.name.equals("signtool.exe", ignoreCase = true) }
            .toList()
        val signtoolPath = signtoolCandidates
            .filter { it.parentFile.path.contains("x64", ignoreCase = true) }
            .maxByOrNull { sdkVersionScore(it) }
            ?: signtoolCandidates.maxByOrNull { sdkVersionScore(it) } // may be null if SDK missing

        // Create two PowerShell helper scripts: one to sign, one to install
        val signScript = JFile(msixDir, "Sign-AeroDl.ps1")
        val installScript = JFile(msixDir, "Install-AeroDl.ps1")

        // Note: escape $ for Kotlin interpolation by using ${'$'}
        val signPs1 = """
        <#
          Sign-AeroDl.ps1
          - Creates or reuses a self-signed Code Signing certificate (CN configurable)
          - Exports KDroidFilter.cer and KDroidFilter.pfx next to this script
          - Locates signtool.exe from Windows SDK and signs the MSIX package

          Usage:
            ./Sign-AeroDl.ps1 -PackagePath "path\\to\\AeroDl_X_Y_Z.msix" -CN "D541E802-6D30-446A-864E-2E8ABD2DAA5E" -PfxPassword "your-pass"
        #>
        param(
          [Parameter(Mandatory=${'$'}false)] [string]${'$'}PackagePath = "${outputMsix.canonicalPath}",
          [Parameter(Mandatory=${'$'}false)] [string]${'$'}CN = "$msixCertCN",
          [Parameter(Mandatory=${'$'}false)] [string]${'$'}PfxPassword = "ChangeMe-Temp123!"
        )
        ${'$'}ErrorActionPreference = "Stop"

        if (-not (Test-Path ${'$'}PackagePath)) { throw "MSIX not found: ${'$'}PackagePath" }

        Write-Host "== AeroDl: generate self-signed cert and sign MSIX =="

        # Find or create a code-signing certificate for the given CN (CurrentUser store)
        ${'$'}store = New-Object System.Security.Cryptography.X509Certificates.X509Store("My","CurrentUser")
        ${'$'}store.Open([System.Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite)
        ${'$'}cert = ${'$'}store.Certificates | Where-Object {
            ${'$'}_.Subject -eq "CN=${'$'}CN" -and ${'$'}_.HasPrivateKey -and (
              ${'$'}_.EnhancedKeyUsageList | Where-Object { ${'$'}_.FriendlyName -like "*Code Signing*" }
            )
        } | Select-Object -First 1
        if (-not ${'$'}cert) {
          Write-Host "Creating self-signed code signing certificate for CN=${'$'}CN ..."
          ${'$'}cert = New-SelfSignedCertificate `
            -Type CodeSigningCert `
            -Subject "CN=${'$'}CN" `
            -CertStoreLocation "Cert:\CurrentUser\My" `
            -KeyAlgorithm RSA -KeyLength 2048 `
            -NotAfter (Get-Date).AddYears(2)
        } else {
          Write-Host "Reusing existing code-signing certificate for CN=${'$'}CN"
        }

        ${'$'}scriptDir = Split-Path -Parent ${'$'}MyInvocation.MyCommand.Path
        ${'$'}cerPath = Join-Path ${'$'}scriptDir "KDroidFilter.cer"
        ${'$'}pfxPath = Join-Path ${'$'}scriptDir "KDroidFilter.pfx"

        # Export public and private keys (for signing)
        Export-Certificate -Cert ${'$'}cert -FilePath ${'$'}cerPath | Out-Null
        ${'$'}secPwd = ConvertTo-SecureString ${'$'}PfxPassword -AsPlainText -Force
        Export-PfxCertificate -Cert ${'$'}cert -FilePath ${'$'}pfxPath -Password ${'$'}secPwd | Out-Null

        # Optionally trust the public certificate for current user (not required for signing)
        try { Import-Certificate -FilePath ${'$'}cerPath -CertStoreLocation Cert:\CurrentUser\TrustedPeople | Out-Null } catch {}

        # Locate signtool.exe (fallback to SDK search)
        ${'$'}signtool = ${'$'}null
        ${
            if (signtoolPath != null) "if (Test-Path '${
                signtoolPath.canonicalPath.replace(
                    "\\",
                    "\\\\"
                )
            }') { \$signtool = '${signtoolPath.canonicalPath.replace("\\", "\\\\")}' }" else ""
        }
        if (-not ${'$'}signtool) {
          ${'$'}c = Get-ChildItem "${
            windowsKitsDir.canonicalPath.replace(
                "\\",
                "\\\\"
            )
        }" -Recurse -Filter signtool.exe -ErrorAction SilentlyContinue |
                Where-Object { ${'$'}_.FullName -match "x64" } | Sort-Object FullName -Descending | Select-Object -First 1
          if (${'$'}c) { ${'$'}signtool = ${'$'}c.FullName }
        }
        if (-not ${'$'}signtool) { throw "signtool.exe not found. Please install Windows 10/11 SDK." }
        Write-Host "Using signtool: ${'$'}signtool"

        # Sign the MSIX
        & ${'$'}signtool sign /fd SHA256 /f ${'$'}pfxPath /p ${'$'}PfxPassword ${'$'}PackagePath
        Write-Host "Signing completed: ${'$'}PackagePath"
        """.trimIndent()

        signScript.writeText(signPs1)
        logger.lifecycle("packageReleaseMsix: Wrote ${signScript.absolutePath}")

        val installPs1 = """
        <#
          Install-AeroDl.ps1
          - Imports KDroidFilter.cer into CurrentUser TrustedPeople
          - Installs the specified MSIX package

          Usage:
            ./Install-AeroDl.ps1 -PackagePath "path\\to\\AeroDl_X_Y_Z.msix" -CerPath ".\\KDroidFilter.cer"
        #>
        param(
          [Parameter(Mandatory=${'$'}false)] [string]${'$'}PackagePath = "${outputMsix.canonicalPath}",
          [Parameter(Mandatory=${'$'}false)] [string]${'$'}CerPath
        )
        ${'$'}ErrorActionPreference = "Stop"

        if (-not (Test-Path ${'$'}PackagePath)) { throw "MSIX not found: ${'$'}PackagePath" }

        ${'$'}scriptDir = Split-Path -Parent ${'$'}MyInvocation.MyCommand.Path
        if (-not ${'$'}CerPath) { ${'$'}CerPath = Join-Path ${'$'}scriptDir "KDroidFilter.cer" }

        if (-not (Test-Path ${'$'}CerPath)) {
          throw "Certificate file not found: ${'$'}CerPath. Please run Sign-AeroDl.ps1 first to generate and sign."
        }

        Write-Host "== AeroDl: trust certificate and install MSIX =="

        # Install certificate into CurrentUser TrustedPeople (no admin required)
        Import-Certificate -FilePath ${'$'}CerPath -CertStoreLocation Cert:\CurrentUser\TrustedPeople | Out-Null
        Write-Host "Trusted certificate: ${'$'}CerPath"

        # Install the MSIX
        Add-AppxPackage -Path ${'$'}PackagePath
        Write-Host "Success: package installed."
        """.trimIndent()

        installScript.writeText(installPs1)
        logger.lifecycle("packageReleaseMsix: Wrote ${installScript.absolutePath}")
    }
}
