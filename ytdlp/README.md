# 🚀 **AeroDL**

### The Modern YouTube-DL GUI for Windows, macOS & Linux

> **AeroDL** is a sleek, cross-platform YouTube-DL frontend built with **JetBrains Compose Multiplatform**.
> It combines the power of `yt-dlp` with a beautiful, native-feeling interface for fast and reliable video & audio downloads.

---

## 🏠 **Home**

AeroDL welcomes you with a clean and minimal home screen.
Simply paste or detect a YouTube link — AeroDL automatically recognizes it and prepares everything for download.

![Home Screenshot](/art/home.png)

---

## 🔔 **Smart Link Detection**

Instant notifications appear when a supported link is detected — open it directly in AeroDL or ignore.

![Notification Screenshot](/art/notification.png)

---

## 🎬 **Video Info View**

Preview thumbnails, read descriptions, and choose between video or audio modes before confirming your download.

![Single Download Screenshot](/art/single-download.png)

---

## 📥 **Download Manager**

Track all your downloads in real time — progress bars, speeds, sizes, and completion notifications.

![Download Manager Screenshot](art/download-manager.png)

---

## ⚙️ **Settings Panel**

Easily customize AeroDL to your workflow:
choose your browser cookies, naming presets, threads, parallel downloads, and thumbnail embedding.

![Settings Screenshot](/art/settings.png)

---

## 🪄 **Key Features**

* 🎯 **Smart link detection** — detects YouTube links from clipboard or browser.
* 🎵 **Audio & Video modes** — download MP3s or full HD/4K videos with tags.
* ⚙️ **Highly configurable** — presets, naming, threads, parallel jobs.
* 🍪 **Browser cookies integration** — import from Firefox, Chrome, etc.
* 🖥️ **Cross-platform** — works on **Windows**, **macOS**, and **Linux (KDE/GNOME)**.
* 💡 **Modern interface** — animated transitions, fluent design, dark mode.
* 🧩 **Powered by ComposeNativeTray** — lightweight native tray integration.

---

## 🧠 **How It Works**

1. **Paste or detect a link** — AeroDL automatically catches YouTube URLs.
2. **Fetch metadata** — title, duration, formats, and thumbnail.
3. **Select quality & mode** — video or audio, preset or manual.
4. **Download instantly** — see speed and progress in real time.
5. **Enjoy** — open the downloaded file or folder directly from the tray.

---

## ⚙️ **Technical Stack**

* **Kotlin Multiplatform (JVM)**
* **JetBrains Compose Desktop**
* **Yt-DLP** — backend engine
* **FFmpeg** — conversion and tagging
* **ComposeNativeTray** — native tray integration
* **Ktor** — secure networking
* **Fluent Material UI** — clean, responsive interface

---

## 🧩 **Platform Support**

| Platform             | Status   | Notes                                      |
| -------------------- | -------- | ------------------------------------------ |
| 🪟 Windows 10+       | ✅ Stable | MSIX build with self-signed cert installer |
| 🍎 macOS 13+         | ✅ Stable | Native app distributed via Homebrew        |
| 🐧 Linux (GNOME/KDE) | ✅ Stable | DE-aware window & tray handling            |

---

## 📦 **Installation**

Official distribution (coming soon):

* **Windows** → Microsoft Store / MSIX installer
* **macOS** → Homebrew tap
* **Linux** → `.deb`, `.rpm`, and AppImage packages

For testing or development:

```bash
git clone https://github.com/kdroidFilter/AeroDL
cd AeroDL
./gradlew run
```

---

## ❤️ **Support & Feedback**

AeroDL is open source and constantly evolving.
If you like it:

* ⭐ **Star** the project on [GitHub](https://github.com/kdroidFilter/AeroDL)
* 🐛 **Report bugs or suggest features** via [Issues](https://github.com/kdroidFilter/AeroDL/issues)
* 💬 **Share screenshots & feedback** to help improve it!

---

## 📄 **License**

This project is licensed under the **GPL-3.0 License** — see the LICENSE file for details.
