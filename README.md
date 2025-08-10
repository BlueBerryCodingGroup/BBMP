# BlueBerry Minecraft Proxy (BBMP)

A Java-based Minecraft TCP proxy with optional dark-themed packet viewer, packet filtering, and packet rewriting.  
The **P** in **BBMP** stands for **Proxy**.

---

## ✨ Features

### Proxy Core
- **Customizable Target**
  - Defaults to `play.hypixel.net:25565`
  - Override with `-ip` and `-rport` arguments
- **Handshake Rewriting**
  - Rewrites the handshake server address to the real target host  
  - Keeps the client believing it’s connected to `localhost`
- **Status Response Scrubbing**
  - Replaces all IP/host references in the server's status JSON with `localhost`
- **Raw Packet Forwarding**
  - After login, packets are passed through without modification for maximum compatibility

### Packet Viewer (Dev Mode)
- **Dark-Themed UI** powered by [FlatLaf](https://www.formdev.com/flatlaf/)
- **Packet Filtering**
  - All packets
  - Handshake/Status only
  - Raw only
- **Search & Filter**
  - Search by direction, packet name, or notes
- **Live Counters**
  - Sent & received byte totals (C→S and S→C)
- **Pause / Resume**
  - Freeze the view without stopping the proxy
- **Autoscroll Toggle**
- **CSV Export**
  - Save visible packets to a CSV file
- **Clear View**
  - Reset counters and clear the table instantly

---

## 🚀 Build

This is a Gradle Kotlin DSL project.

### Prerequisites
- Java 17+
- Gradle (wrapper included)

### Commands

```bash
# Clean & build normal + fat jar
./gradlew clean build

# Build only the fat jar
./gradlew shadowJar
```

The fat jar will be located at:
```
build/libs/bbmp-proxy-all.jar
```

---

## 🛠 Run

Basic usage:
```bash
java -jar build/libs/bbmp-proxy-all.jar
```

With custom target:
```bash
java -jar build/libs/bbmp-proxy-all.jar     -port 25565     -ip play.hypixel.net     -rport 25565
```

Enable Dev Mode packet viewer:
```bash
java -jar build/libs/bbmp-proxy-all.jar     -port 25565     -ip play.hypixel.net     -rport 25565     -devmode true
```

---

## 📄 Command-line Arguments

| Flag       | Default             | Description |
|------------|---------------------|-------------|
| `-port`    | `25565`             | Local port to listen on |
| `-ip`      | `play.hypixel.net`  | Remote server host |
| `-rport`   | `25565`             | Remote server port |
| `-devmode` | `false`             | Enable packet viewer UI |

---

## 📦 Distribution Tasks

Gradle will also produce:
- `distZip` / `distTar` archives with scripts for running BBMP  
- `startScripts` for launching via platform scripts

---

## ⚠ Disclaimer
**BlueBerry Minecraft Proxy (BBMP)** is for **educational and debugging purposes only**.  
You are responsible for complying with the terms of service of any server you connect to.
# BBMP
