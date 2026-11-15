# MR SHADOW DEVICE INFO

A privacy-conscious Android device-information and developer-contact utility. It displays information available through standard Android APIs, provides a local developer-app catalog, and is designed for device information review rather than root/bootloader/firmware-related actions.

## Build
- Android Studio not required for GitHub Actions
- GitHub Actions builds the APK automatically on push or workflow dispatch
- Java 17 + Android SDK 35
- Gradle 8.9

Open this repository as a Gradle project and run `./gradlew assembleDebug` locally if needed.

## Developer app catalog
Edit `app/src/main/assets/apps.json` to publish developer-managed app entries. The initial catalog is empty by design; no fake URLs are included.

## Secure report sharing
The app does not include a Telegram bot token, backend secret, or fake report success message.
The user sees a full preview before sharing and may choose to share through Android Sharesheet.
No backend is configured yet.

## Privacy and limitations
No IMEI, IMSI, serial number, phone number, SIM identifiers, location, account identifiers, credentials, or personal files are collected.
No root or bootloader unlock status is guaranteed; it reads only what Android exposes.
Camera metadata is not collected automatically; it is shown as unavailable.
The app does not root, unlock, bootloader-unlock, flash, exploit, or install APKs silently.

## How to add the app
1. Create a branch from main.
2. Add the above files.
3. Commit the message: `Add MR SHADOW DEVICE INFO app and GitHub Actions APK build`
4. Push to GitHub.
5. Open the Actions tab and wait until the APK is built.
6. Download the artifact `mr-shadow-device-info-debug-apk`.

## Artifacts
The build job uses `actions/upload-artifact` to upload the APK as:
`app/build/outputs/apk/debug/app-debug.apk`
