# Architecture and integration boundaries

## Goal

Present a RAVEN Classic Android companion for Re:ANIME without recreating its backend or copying proprietary code.

## v0.3.0 architecture

```text
MainActivity
  |-- Android file chooser and fullscreen video
  |-- RavimeApp (Jetpack Compose)
  |     |-- RAVEN Classic native home, profile, settings and navigation
  |     +-- ReanimeWebView (persistent session)
  |            |-- https://reanime.to/home
  |            |-- https://reanime.to/search
  |            |-- https://reanime.to/community
  |            |-- https://reanime.to/profile
  |            |-- https://reanime.to/schedule
  |            +-- https://reanime.to/watch2gether
  +-- External browser for non-Re:ANIME top-level URLs
```

A single WebView instance keeps cookies, route history and session continuity while the native UI switches tabs. On first-party pages Ravime injects the RAVEN Classic CSS/DOM tagging adapter; it alters appearance, not website logic. Users can switch it off. This app contains no custom HTTP client for the service and does not attempt to reverse-engineer private endpoints.

## What is native versus web?

| Feature | Current owner |
| --- | --- |
| App home, navigation, profile hub, local browser preferences, green accent, Android fullscreen host | Ravime |
| Anime catalogue, detailed anime pages, search results | Website in WebView |
| Accounts, comments, watchlist, community and ad preferences | Website in WebView |
| Streaming player, server lists, episode switching | Website in WebView |
| Watch parties, real-time chat | Website in WebView, unverified |
| Android push notifications and native offline playback | Not implemented |

## Browser and privacy

- JavaScript and DOM storage are enabled because the website requires them.
- The app does not collect credentials, add JS interfaces or rewrite website responses.
- Top-level navigation to other hosts opens an external browser. Subframes remain under the site's control (important for video embeds).
- Cleartext HTTP top-level navigation is rejected.
- Android Safe Browsing is enabled. The app does not override SSL failures.
- Cookies and third-party cookies are enabled to support sessions/embeds; users can manage site cookies through Android app data controls.
- Files are shared only when a user chooses them through Android's system file picker.

## Roadmap

1. Validate native profile/settings shortcuts, site Disable Ads toggle after sign-in, Android login, browser back behavior, video playback, fullscreen, site messaging and file uploads.
2. Fix device-specific compatibility and refine native navigation.
3. Request/identify supported integration methods from the Re:ANIME operators.
4. If authorised, implement native search, anime information, library and user profile screens with typed data models.
5. Add more RAVEN components: artwork-adaptive accent, poster grids, anime detail transitions and compact episode lists.
6. Consider notifications/Picture-in-Picture only after supported integration and compatibility validation.

No native screen should claim that it synchronises data unless the required supported integration exists.\n\n### Website theme\n\n`app/src/main/assets/ravime.css` contains the palette, form, modal, anime-card, player, search, schedule, community and profile styling. `ravime.js` tags semantic DOM targets, listens to SPA route changes and uses a MutationObserver to keep page classes current. `RavimeWebTheme.kt` injects and reverses the style safely into the main WebView document. Video streams and third-party frames are excluded from DOM alterations. This is not a private API integration or permission bypass.
