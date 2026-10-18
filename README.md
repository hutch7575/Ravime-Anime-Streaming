# Ravime Anime Streaming

**Ravime** is an experimental Android companion application in the **RAVEN** family, alongside [Ravyn Music Player](https://github.com/hutch7575/Ravyn-Music-Player).

The app adopts the RAVEN Classic (2016-era Android) visual language and offers a native navigation shell around the publicly accessible website at [Re:ANIME](https://reanime.to). Ravime does **not** own or operate that service.

## Current implementation — 0.1.0

- Native Android application written in Kotlin and Jetpack Compose.
- RAVEN Classic surfaces, compact typography, restrained corners and bottom navigation.
- Native welcome/home page with links to discovery, search, schedule, community, watch parties and account settings.
- Persistent WebView session for Re:ANIME pages, including site-managed sign-in, search, discussions and playback when supported by Android WebView.
- Fullscreen video handoff to the Android window and rotation support.
- Android file picker for site-supported community uploads.
- External navigation opens the user's browser; no native JavaScript bridge or credential scraping.
- GitHub Actions debug APK build.

**Important:** The detailed Re:ANIME browsing and playback pages are still website-rendered, not fully re-skinned. A genuinely native catalogue, anime details, progress management or chat requires an authorised, documented API (or cooperation from the service). Playback, login, chat and Watch2Gether compatibility must be tested on a real Android device. Nothing here guarantees all website features work.

## Build

Open the repository in Android Studio (JDK 17, Android SDK 35) and build the `app` module using Gradle 8.11.1. Or run the **Ravime Android APK** workflow in GitHub Actions and download its debug APK artifact.

Application ID: `app.raven.ravime`. Minimum Android: 8.0 (API 26).

## Design language

RAVEN Classic follows Ravyn's current source-level tokens:

| Token | Hex |
| --- | --- |
| Background | `#101113` |
| Panel | `#1B1D1E` |
| Raised | `#282B2D` |
| Primary text | `#F7F7F7` |
| Secondary text | `#B8BCBD` |
| Muted | `#8E9294` |

Ravime uses a temporary muted lavender accent. Artwork-based adaptive accents can replace it when genuine native artwork data is available. Square thumbnails, 10dp panel radii and compact information density carry over from Ravyn.

## Important limitations and rights

Ravime is an **unofficial, independent third-party interface** and is **not affiliated with or endorsed by Re:ANIME**. It does not redistribute anime, bundle media, intercept encrypted streams, bypass DRM or impersonate the service. Web video providers, third-party sign-in and some integrations may not work inside WebView.

Before publishing in an app store or distributing broadly, obtain any required website-owner authorisation, permission to use service branding/assets and permission for the material presented. Website access and feature availability can change at any time.

See [Architecture and roadmap](docs/ARCHITECTURE.md).
