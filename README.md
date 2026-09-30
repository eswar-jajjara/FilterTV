# FilterTV

An experimental Android TV YouTube client based on **SmartTube**, with a separate media-request filtering laboratory. See [UPSTREAM.md](UPSTREAM.md) and [original documentation](README-UPSTREAM.md).

## Status and scope

YouTube browsing, search, sign-in, subscriptions, remote controls, playback and SponsorBlock are inherited from SmartTube. Ad-free YouTube behavior depends on its unofficial playback integration and ongoing upstream maintenance, not the domain filter. No guarantee is made for every video or TV.

The TV home opens on recommendation rows, including for guests when the service provides them. The default sidebar puts Home, Shorts, Subscriptions, History and Playlists first, followed by other topics. Account switching, QR/code sign-in, search and watch controls use the inherited TV interface. This is a familiar layout, not an exact replica of the official YouTube app or its recommendation algorithm. The sign-in QR is rendered on-device so its one-time code is not sent to an external QR image service.

The added filter is an immutable Java domain matcher, **not Brave adblock-rust**. Rust/JNI, full Adblock syntax, downloaded lists, whole-app traffic filtering and a Compose/Media3 migration are future work. Do not import EasyList into this small prototype.

## Build

Install JDK 17 and Android SDK (platform 34, build-tools 30.0.3, NDK 21.0.6113669). Set JAVA_HOME and ANDROID_HOME. Clone this repository with `--recurse-submodules`, then:

```powershell
.\gradlew.bat assembleStfdroidDebug
python tools/test-filter.py
.\gradlew.bat lintStfdroidDebug
```

The flavor name is retained to minimize upstream merge conflicts. Package ID: `io.github.filtertv.app`. APKs appear in `smarttubetv/build/outputs/apk/stfdroid/debug/`. GitHub Actions builds debug APK artifacts on pushes and pull requests. CI is not a signed production release process. Keep signing keys private and use the same release key for all updates.

## Use

Install the universal debug APK using `adb install -r <apk>`. Navigate with the TV remote. Open **Settings → General → FilterLab** to edit rules, toggle filtering, test a URL locally, and view session counters. Filtering is disabled initially; the only supplied rule blocks a reserved example domain.

Rules:

```text
! Comments begin with !
||ads.example.test^
@@||safe.ads.example.test^
```

Rules match a domain and its subdomains. Allow exceptions take precedence. Unsupported syntax is rejected and leaves saved rules intact. Gate counters count evaluations, not unique requests; successful requests can be evaluated twice. Rule tests use the editor contents even before saving. They do not send network requests.

## Request path

ExoPlayer HTTP media requests → OkHttp application gate → redirect-aware network gate → server.

Initial blocks happen before DNS. Redirect blocks happen before HTTP bytes are sent, but OkHttp may already have established a connection. Cached responses still pass the initial gate. Metadata, artwork, account, casting and other upstream requests are outside this filter. All media transports are forced to OkHttp; upstream alternate-transport settings therefore have no effect in this prototype. Ads stitched into an allowed video stream cannot be removed by domain filtering.

## Compatibility and validation

Build minimum: Android 5.0 / API 21. Configured native targets: ARMv7, ARM64 and x86. Actual codecs, native library availability, WebView and vendor firmware vary. Android TV / Google TV / Fire TV hardware testing is still required. Samsung Tizen and LG webOS are not Android.

Run `python tools/test-filter.py` for domain-boundary, allow-exception, invalid-rule, initial-request and redirect tests against a local HTTP server. Before distributing, test D-pad navigation, sign-in, search, live/VOD playback, pause/seek, captions, background/resume, and update installation on real devices. Do not claim compatibility from compilation alone.

## Updates and publishing

The fork does not install upstream SmartTube APK updates. Merge upstream source fixes and rebuild this app. Publish to your own repository with the source and license notices intact. GitHub build artifacts expire; a release needs an intentionally signed APK and release notes. Never commit keystores, local.properties or account data.
