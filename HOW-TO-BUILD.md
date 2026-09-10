# How to Build Komi Store (GithubStore)

This document explains how to build Komi Store from source on a clean machine (Linux, macOS
or Windows). It is written for people who have never built this project before, and it does
not assume any particular username, home directory, or filesystem layout: everywhere a path
appears, substitute your own.

Komi Store is a **Kotlin Multiplatform / Compose Multiplatform** app. The same codebase
produces an **Android** app (`:composeApp` -> APK) and a **Desktop/JVM** app (Windows / macOS
/ Linux). Pick the target you need in section 4.

> TL;DR (Android debug APK)
> ```bash
> # JDK 21+ is mandatory (see section 1.1)
> export JAVA_HOME=/path/to/jdk-21
> export ANDROID_HOME=/path/to/Android/Sdk
> ./gradlew :composeApp:assembleDebug
> ```
> Output APK: `composeApp/build/outputs/apk/debug/`

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | **21+** | Mandatory to run the build (Temurin recommended). Note: the *bytecode target* is Java 17 (`jvmTarget = JVM_17`), but the toolchain that runs Gradle must be 21+. |
| Android SDK | Platform **36**, Build-Tools **36.x** | Only for the Android target. `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`. Not needed for a Desktop-only build. |
| Gradle | **8.14.3** | Do **not** install manually. The bundled wrapper (`./gradlew`) downloads the correct version on first run. |
| Disk space | ~6 GB free | 37 Gradle modules, an included build (`build-logic`), plus the dependency cache. |
| RAM | 8 GB+ recommended | `gradle.properties` sets `-Xmx4096M`, parallel + configuration cache on. |

No credentials, tokens, or private registries are required. Everything resolves from Maven
Central, Google's Maven repository, and the Compose Multiplatform repositories.

### 1.1 Install a JDK 21+

The version matters more than the vendor: Temurin, Zulu, Corretto, Liberica, Microsoft Build
of OpenJDK and the JetBrains Runtime all work.

- Linux (Debian/Ubuntu): `sudo apt install openjdk-21-jdk`
- Linux (Fedora/RHEL): `sudo dnf install java-21-openjdk-devel`
- Linux (Arch): `sudo pacman -S jdk21-openjdk`
- macOS (Homebrew): `brew install openjdk@21`
- Windows: `winget install EclipseAdoptium.Temurin.21.JDK`, or the Adoptium MSI.
- Any OS, no admin rights: unpack a JDK 21 archive from <https://adoptium.net/> anywhere writable.

If Android Studio is installed, it ships a JetBrains Runtime 21 you can point at instead of
installing anything:

| OS | Bundled JBR path |
|----|------------------|
| Linux | `<android-studio-dir>/jbr` (often `/opt/android-studio/jbr` or `/usr/local/android-studio/jbr`) |
| macOS | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` |
| Windows | `C:\Program Files\Android\Android Studio\jbr` |

Point `JAVA_HOME` at it and verify (must print `21.x`):

```bash
# Linux / macOS - substitute your own path
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
"$JAVA_HOME/bin/java" -version
```
```powershell
# Windows (PowerShell), current session only
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

To avoid exporting it every shell, set it once in your **home** Gradle file
(`~/.gradle/gradle.properties`, or `%USERPROFILE%\.gradle\gradle.properties`), which is outside
the repo:

```properties
org.gradle.java.home=/absolute/path/to/jdk-21
```

> This project uses an **included build** (`build-logic`). Gradle properties are not inherited
> by included builds, so if you tune heap/JDK there, keep both in sync or pass values on the
> command line.

### 1.2 Install the Android SDK (Android target only)

Skip this for a Desktop-only build. If you have Android Studio, the SDK is already installed;
otherwise install the command-line tools from
<https://developer.android.com/studio#command-line-tools-only>, then:

```bash
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
sdkmanager --licenses
```

Point the build at the SDK, using **either** an environment variable:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"          # Linux
export ANDROID_HOME="$HOME/Library/Android/sdk"  # macOS
```
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"   # Windows
```

**or** a `local.properties` file in the project root (git-ignored, machine-specific):

```properties
# Linux / macOS
sdk.dir=/absolute/path/to/Android/Sdk
```
```properties
# Windows - forward slashes or escaped backslashes
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
```

`local.properties` wins over the environment variable.

---

## 2. Getting the code

```bash
git clone https://github.com/kotlin-benchmark/komi-store.git
cd komi-store
```

Windows: if you hit `Filename too long`, run `git config --global core.longpaths true` once.

---

## 3. Project layout

Komi Store is heavily modularized (37 Gradle modules). The pieces you touch most:

| Path | What it is |
|------|------------|
| `composeApp/` | The application module. `src/commonMain` (shared UI/logic), `src/androidMain` (Android entry point `MainActivity`, `AndroidManifest.xml`), `src/jvmMain` (Desktop entry point `DesktopAppKt`). |
| `core/` | Shared infrastructure split into `domain` / `data` / `presentation`. |
| `feature/<name>/` | Feature modules, each split into `domain` / `data` / `presentation` (apps, auth, details, search, dev-profile, ...). |
| `build-logic/` | Included build with the convention plugins that configure every module (JVM target, Android, Compose). Compiled first. |
| `gradle/libs.versions.toml` | Central version catalog, including the SDK levels. |

---

## 4. Building

On Windows use `gradlew.bat` (or `.\gradlew.bat`) everywhere below; the extensionless
`gradlew` is the POSIX script and will not run in cmd.exe/PowerShell.

### 4.1 Android (debug APK) - the usual

```bash
./gradlew :composeApp:assembleDebug
```

### 4.2 Compile-only check (faster, no packaging)

Type-check just the Android variant of the app module and its dependencies:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Type-check a single module after editing it, e.g.:

```bash
./gradlew :core:data:compileDebugKotlinAndroid
```

### 4.3 Android release APK

```bash
./gradlew :composeApp:assembleRelease
```
Release is minified/shrunk. Signing is only configured if you provide a keystore; without
one, a release APK is produced unsigned (fine for local inspection).

### 4.4 Desktop / JVM

```bash
# run the desktop app directly (mainClass zed.rainxch.githubstore.DesktopAppKt)
./gradlew :composeApp:run

# native installers
./gradlew :composeApp:packageDeb    # Linux
./gradlew :composeApp:packageDmg    # macOS
./gradlew :composeApp:packageExe    # Windows
```
The Desktop target does **not** need the Android SDK.

### 4.5 Everything / formatting

```bash
./gradlew build            # builds & checks all modules (slow)
./gradlew ktlintFormat     # auto-format all modules
./gradlew ktlintCheck      # lint without fixing
```

### 4.6 Fully clean, reproducible build

```bash
./gradlew clean :composeApp:assembleDebug --no-build-cache --no-configuration-cache --rerun-tasks
```

---

## 5. Output and installing

```
composeApp/build/outputs/apk/debug/       # Android APK(s)
composeApp/build/compose/binaries/        # Desktop installers
```

List what was produced without guessing:

```bash
find composeApp/build/outputs/apk -name '*.apk'
```
```powershell
Get-ChildItem -Recurse composeApp\build\outputs\apk -Filter *.apk
```

Install the Android APK on a device/emulator:

```bash
adb install -r composeApp/build/outputs/apk/debug/<the-apk>.apk
```

---

## 6. Troubleshooting

### 6.1 Wrong JDK: toolchain errors, `Unsupported class file major version`, Kotlin/AGP complaints

Build with JDK 21+. Check what Gradle actually resolved, then reset the daemon so a new JDK is
picked up:

```bash
./gradlew --version     # look at the "JVM:" line
./gradlew --stop
```

If `JAVA_HOME` is awkward to manage, set `org.gradle.java.home` in `~/.gradle/gradle.properties`
(section 1.1).

### 6.2 `SDK location not found` (Android target)

Neither `ANDROID_HOME` nor `local.properties` is set for this shell/checkout. See 1.2. An env
var exported in one terminal does not exist in another, and IDEs do not inherit your shell
profile - `local.properties` is the more reliable option.

### 6.3 `Failed to find target with hash string 'android-36'` / missing build-tools

```bash
sdkmanager --update
sdkmanager "platforms;android-36" "build-tools;36.0.0"
sdkmanager --licenses
```
If platform 36 is not offered, update the cmdline-tools package itself
(`sdkmanager "cmdline-tools;latest"`) and retry.

### 6.4 Configuration cache / build cache errors after editing build files

`org.gradle.configuration-cache=true` and `org.gradle.caching=true` are on. After editing
`.gradle.kts` files or the version catalog:

```bash
./gradlew :composeApp:assembleDebug --no-configuration-cache
```
If results look stale rather than broken: `./gradlew clean ... --no-build-cache`.

### 6.5 `Cannot lock ... has already been locked` / stuck build

```bash
./gradlew --stop
pkill -f GradleDaemon            # Linux/macOS, if it persists
```
```powershell
Get-Process java | Where-Object { $_.Path -like '*gradle*' } | Stop-Process   # Windows
```

### 6.6 Out-of-memory: `Java heap space`, `GC overhead limit exceeded`, or a killed daemon

Large modular Compose build with `org.gradle.parallel=true`. Give it more heap or trade speed
for footprint:

```bash
./gradlew :composeApp:assembleDebug -Dorg.gradle.jvmargs=-Xmx6g
./gradlew :composeApp:assembleDebug --no-parallel --no-daemon -Dorg.gradle.jvmargs=-Xmx4g
```
On Linux, a daemon that dies with no message is usually the OOM killer - check `dmesg | tail`.
Remember `build-logic/gradle.properties` is separate from the root one.

### 6.7 Desktop packaging complains about the JDK vendor

The repo sets `compose.desktop.packaging.checkJdkVendor=false` in `gradle.properties` to relax
that check. If you still hit vendor errors, build the Desktop installer with a full JDK 21
distribution (not a JRE).

### 6.8 First build is very slow

Expected. The first run downloads Gradle 8.14.3, the Android Gradle Plugin, Kotlin, Compose
Multiplatform, then configures 37 modules and an included build. Later builds reuse the cache
in your home directory (`~/.gradle`, or `%USERPROFILE%\.gradle`).

### 6.9 Corporate proxy / offline

Standard Gradle proxy properties in `~/.gradle/gradle.properties`:

```properties
systemProp.https.proxyHost=proxy.example.com
systemProp.https.proxyPort=8080
```
For a fully offline build the dependency cache must already be populated; then add `--offline`.

---

## 7. Verifying a checkout builds at all

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew --version                         # confirms JVM 21 and Gradle 8.14.3
./gradlew :composeApp:compileDebugKotlinAndroid   # type-checks the Android app module
./gradlew :composeApp:assembleDebug               # produces the APK
```

If step 2 passes but step 3 fails, the problem is packaging or SDK setup, not the source code.
