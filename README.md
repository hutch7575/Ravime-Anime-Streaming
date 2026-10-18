# Ravime Anime Streaming

**Ravime** is an experimental Android companion application in the **RAVEN** family, alongside [Ravyn Music Player](https://github.com/hutch7575/Ravyn-Music-Player).

The app adopts the RAVEN Classic (2016-era Android) visual language and offers a native navigation shell around the publicly accessible website at [Re:ANIME](https://reanime.to). Ravime does **not** own or operate that service.

## Current implementation — 0.3.0

- Native Android application written in Kotlin and Jetpack Compose.
- RAVEN Classic surfaces, compact typography, restrained corners and bottom navigation.
- RAVEN Classic redesigned home with a Ravyn-inspired greeting, compact content sections and an updated three-tab navigation dock.
- Native Your Library/profile hub and native settings screen, both integrated with the existing persistent Re:ANIME WebView session.
- Fullscreen video handoff to the Android window and rotation support.
- Android file picker for site-supported community uploads.
- External navigation opens the user's browser; no native JavaScript bridge or credential scraping.
- Live, reversible RAVEN Green theme injection into Re:ANIME's actual pages. Styles cover discovery, search, anime details, watch pages, schedule, community, user profiles and settings without replacing page logic.\n- Real website homepage opens directly at launch; native top and bottom navigation remain visible for fast switching.\n- Native WebView autoplay control, RAVEN website-theme switch, and browser-data clear confirmation.\n- Quick links to Re:ANIME's actual account and ad-disable preferences (requires sign-in).\n- GitHub Actions debug APK build.

**Important:** The detailed Re:ANIME browsing and playback pages are website-rendered with a Ravime visual layer (not reimplemented natively). A genuinely native catalogue, anime details, progress management or chat requires an authorised, documented API (or cooperation from the service). Playback, login, chat and Watch2Gether compatibility must be tested on a real Android device. Nothing here guarantees all website features work.

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

Ravime's accent is **RAVEN Green** (`#A5D66B`) throughout navigation, controls, progress feedback, tiles and the app icon. Compact headings, subdued gradient headers and 10dp panel radii are drawn from Ravyn's current source.\n\nWebsite account, ads, playback and sync preferences remain on Re:ANIME's `/profile` page. CSS and presentation-only DOM class tagging are injected into the first-party website document to theme those existing controls. No form fields, player URLs or site requests are replaced. Signing in is required to change site-owned preferences; Ravime never claims native access to private profile data or settings.

## Important limitations and rights

Ravime is an **unofficial, independent third-party interface** and is **not affiliated with or endorsed by Re:ANIME**. It does not redistribute anime, bundle media, intercept encrypted streams, bypass DRM or impersonate the service. Web video providers, third-party sign-in and some integrations may not work inside WebView.

Before publishing in an app store or distributing broadly, obtain any required website-owner authorisation, permission to use service branding/assets and permission for the material presented. Website access and feature availability can change at any time.

See [Architecture and roadmap](docs/ARCHITECTURE.md).

## Theme integration and limitations

The app overlays RAVEN CSS on the real Re:ANIME document (assets `ravime.css` and `ravime.js`). A MutationObserver retags content after client-side navigation. This is deliberately presentation-only: links, forms, comments, menus, episode changes and settings still run the website's original code. **Themed does not mean natively reimplemented.**

- The theme is enabled by default and can be disabled at **Settings → Website appearance**. Disable it if a site change causes a broken layout.
- Video iframes and playback internals are intentionally untouched; playback depends on third-party players and Android WebView.
- Account sign-in, ad preferences, social messages and library changes remain on the website; site functionality may vary by browser/device.
- The theme applies to the first-party Re:ANIME document, not to unrelated third-party frames or external sign-in providers.
- Exact component-level visual parity cannot be guaranteed until tested against live website builds and on Android devices.
