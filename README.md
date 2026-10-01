# FilterTV

An experimental Android TV YouTube client based on **SmartTube**, with Brave's open-source `adblock-rust` engine on the player media request path. See [UPSTREAM.md](UPSTREAM.md) and [original documentation](README-UPSTREAM.md).

## Status and scope

YouTube browsing, search, sign-in, subscriptions, remote controls, playback and SponsorBlock are inherited from SmartTube. Ad-free YouTube behavior depends on its unofficial playback integration and ongoing upstream maintenance. Network rules cannot identify ads stitched into an allowed video stream. No guarantee is made for every video or TV.

The TV home opens on recommendation rows, including for guests when the service provides them. If a fresh profile receives no Home rows, it tries public Trending rows before Music while keeping Home selected. The navigation rail expands to show labels while focused and collapses to icons in content. A short launcher animation, search pill, red play-mark branding and player controls follow the familiar YouTube TV layout. Home adds an actual Shorts shelf; Home and the Shorts tab share a short-lived response cache. Guest rows remain dependent on service availability. The player settings panel groups Quality, Captions, Audio, Speed and Repeat next to a smaller video preview. Account switching, QR/code sign-in and recommendations still use the inherited SmartTube integration; personalized rows require signing in within FilterTV. This is a familiar layout, not an exact replica of the official YouTube app or its recommendation algorithm. The sign-in QR is rendered on-device so its one-time code is not sent to an external QR image service. Select Get a new code to restart device authorization if necessary; Back cancels. Completing Google authorization requires the account owner on their phone or browser.

The player filter uses Brave's `adblock-rust` 0.13.3 through JNI. It checks full request URLs using EasyList, EasyPrivacy, and optional personal rules. Lists download on first use and refresh every seven days when available. Until the first download finishes, only the supplied example rule is active. Rule compilation runs on a background thread; matching uses an immutable native engine. Cosmetic filtering is excluded to save memory because this is a native TV UI. Metadata, artwork, account, casting, and other upstream traffic are still outside this gate. FilterTV does not provide whole-TV or other-app filtering.

## Build

Install JDK 17, a current Rust toolchain, and Android SDK (platform 34, build-tools 30.0.3, NDK 21.0.6113669). Set JAVA_HOME and ANDROID_HOME. Clone this repository with `--recurse-submodules`. On Linux, build the native library before the APK:

```bash
bash tools/build-brave-native.sh
./gradlew assembleStfdroidDebug lintStfdroidDebug
```

The request-gate tests can run separately:

```powershell
python tools/test-filter.py
cargo test --manifest-path bravefilter/Cargo.toml --locked
```

The flavor name is retained to minimize upstream merge conflicts. Package ID: `io.github.filtertv.app`. APKs appear in `smarttubetv/build/outputs/apk/stfdroid/debug/`. GitHub Actions builds the three native ABIs and debug APK artifacts on pushes and pull requests, and checks that each native library is packaged. CI is not a signed production release process. Keep signing keys private and use the same release key for all updates.

The build applies `patches/sharedmodules-disable-debug-http-profiler.patch` to the pinned SmartTube submodule. Its debug HTTP profiler and BODY logger otherwise process video responses, which can add substantial playback overhead in test APKs. The patch does not by itself resolve every upstream playback failure; test the resulting APK on the target TV.

## Use

Download the newest successful **FilterTV APK** workflow artifact from this repository's [Actions](https://github.com/eswar-jajjara/FilterTV/actions) page, unzip it, and choose the APK named `universal`. On an Android TV / Google TV / Fire TV device, allow installation from the app used to open the APK, then install it from a USB drive or a file transfer app. With Android debugging enabled, a computer can instead run `adb install FilterTV...universal.apk`. Open FilterTV from the TV apps list and navigate with the remote. Samsung Tizen and LG webOS cannot install an Android APK. These CI debug builds may use different signing keys between runs; if an update reports a signature conflict, uninstall the earlier debug build first (this removes its app data). A stable release signing key is required for data-preserving updates.

Open **Settings → General → FilterLab** to edit personal rules, toggle media filtering, update lists, test a URL locally, and view session counters. Filtering is enabled initially, but the full lists become active only after their first successful download. The supplied rule blocks only a reserved example domain. FilterLab shows list and native-engine status. Testing uses currently compiled rules and does not send a request.

Rules:

```text
! Comments begin with !
||ads.example.test^
@@||safe.ads.example.test^
```

The Brave parser supports network rules from Adblock Plus/uBlock syntax. Unsupported lines are ignored by the upstream parser. Personal rules are limited to 64 KiB. Gate counters count evaluations, not unique requests; successful requests can be evaluated twice. Save before testing a newly edited rule.

## Request path

ExoPlayer HTTP media requests → selected transport (Cronet, OkHttp, or Java HTTP) → Brave request gate → server. Each transport also checks redirect destinations; Cronet's Java fallback carries the same gate.

Initial blocks happen before DNS. Redirect blocks happen before HTTP bytes are sent, but OkHttp may already have established a connection. Cached responses still pass the initial gate. Metadata, artwork, account, casting and other upstream requests are outside this filter. Network-engine preferences are respected. Ads stitched into an allowed video stream cannot be removed by network filtering. Blocking a shared video/ad endpoint may also break playback, so custom rules should be tested carefully.

The compact TV layout uses larger rounded thumbnails, an expanding six-item rail, thumbnail-only focus outlines, a top-left player title, and grouped controls. Static thumbnails reuse Glide memory cache and use higher-resolution YouTube artwork when available, with bounded decoding and original-URL fallback. Holding focus for 600 ms can preload one video’s playback information; it does not download video bytes. Rapid section changes are debounced and obsolete section requests are disposed. Untouched legacy defaults migrate to the compact layout; custom sidebar and player-button selections are preserved. Card shadows are disabled and obsolete duplicate image-loading code is removed. Cronet callbacks reuse one worker across player restarts. These changes reduce avoidable rendering and thread overhead; they do not guarantee a particular startup time or repair every upstream YouTube client failure.

On a launcher return after five minutes in the background, Home opens. A shorter return to the last player remains paused at its saved position until Play. Selecting a new video still starts playback. In-app activities do not count as leaving the app. The original audio is the default; explicitly chosen languages remain available. The selector filters translated audio even when there are only two language groups. Untouched legacy 720p defaults migrate to 1080p AVC where supported; explicit quality settings are preserved. The simple Quality menu lists resolutions and Audio lists the current video’s alternative tracks. Advanced settings retain codec controls.

For a smaller download, choose an ABI-specific APK only when your TV's CPU architecture is known. The universal APK includes all three supported architectures. Deleting unrelated source files does not make an installed app faster.

## Compatibility and validation

Build minimum: Android 5.0 / API 21. Configured native targets: ARMv7, ARM64 and x86. Actual codecs, native library availability, WebView and vendor firmware vary. Android TV / Google TV / Fire TV hardware testing is still required. Samsung Tizen and LG webOS are not Android.

Run `python tools/test-filter.py` for initial-request and redirect gate tests against a local HTTP server, and `cargo test --manifest-path bravefilter/Cargo.toml --locked` for Brave rule and exception matching. Before distributing, test D-pad navigation, sign-in, search, live/VOD playback, pause/seek, captions, background/resume, filtering with real lists, and update installation on real devices. Do not claim compatibility from compilation alone.

## Updates and publishing

Android transport and JNI checks run with `./gradlew :smarttubetv:connectedStfdroidDebugAndroidTest` on a connected test device or emulator. The tests use a local server to verify initial blocks, blocked redirects, allowed redirects and operation without a gate, plus the packaged Brave library's matching through JNI. Resume/audio regression tests cover the five-minute boundary, pause consumption, and original/explicit-dub selection with two audio groups. Use an isolated test profile with the default personal rule. Live service availability, sign-in, and performance on physical TVs require separate testing.

The fork does not install upstream SmartTube APK updates. Merge upstream source fixes and rebuild this app. Publish to your own repository with the source and license notices intact. GitHub build artifacts expire; a release needs an intentionally signed APK and release notes. Never commit keystores, local.properties or account data.

Brave `adblock-rust` is MPL-2.0. EasyList and EasyPrivacy are downloaded from their authors and are dual licensed GPL-3.0-or-later / CC BY-SA 3.0; see [their license](https://easylist.to/pages/licence.html). No list contents are bundled in this repository.
