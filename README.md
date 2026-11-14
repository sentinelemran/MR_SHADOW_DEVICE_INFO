# MR SHADOW DEVICE INFO

A privacy-conscious native Android utility built with Kotlin, Jetpack Compose, and Material 3. It displays information available through standard Android APIs, provides a local developer-app catalog, contact links, user-reviewed report sharing, and basic settings.

## Build

- Android Studio with JDK 17
- Android SDK 35

Open this repository as a Gradle project and run `:app:assembleDebug` (or use Android Studio's Run action). Minimum Android version is API 26.

## Developer app catalog

Edit `app/src/main/assets/apps.json` to publish real app entries. The initial catalog is empty by design; no sample or fictitious download URLs are included. Each item can have `id`, `name`, `description`, `category`, optional `downloadUrl`, `websiteUrl`, `version`, and `visible` fields.

## Secure report backend

The app does not contain a Telegram bot token and does not send reports automatically. Configure `REPORT_API_BASE_URL` in `app/build.gradle.kts` with the HTTPS base URL of a separately hosted backend. It must implement `POST /v1/device-reports`, validate and rate-limit requests, sanitize and size-limit reports, and return JSON `{"success":true}` only after Telegram confirms delivery. Backend credentials and the developer's Telegram chat ID must remain server-side. Without a configured service, report submission fails visibly and honestly.

## Privacy and limitations

No IMEI, serial number, phone number, SIM identifiers, location, account identifiers, credentials, or personal files are collected. Camera specs use Camera2 metadata only; the camera is never opened for capture. Root and bootloader status are labeled "Not reliably detectable" rather than guessed. Unavailable properties are shown as such. Export uses Android's Storage Access Framework document picker; sharing uses the Android Sharesheet.

This is a device-information and developer-contact utility, not a rooting, bootloader-unlocking, or firmware-flashing tool. Bengali localization, remote catalog management, and a production report backend require product/backend configuration before public release.
