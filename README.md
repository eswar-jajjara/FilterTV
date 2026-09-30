# FilterTV

An experimental Android TV YouTube client based on **SmartTube**, with Brave's open-source `adblock-rust` engine on the player media request path. See [UPSTREAM.md](UPSTREAM.md) and [original documentation](README-UPSTREAM.md).

## Status and scope

YouTube browsing, search, sign-in, subscriptions, remote controls, playback and SponsorBlock are inherited from SmartTube. Ad-free YouTube behavior depends on its unofficial playback integration and ongoing upstream maintenance. Network rules cannot identify ads stitched into an allowed video stream. No guarantee is made for every video or TV.

The TV home opens on recommendation rows, including for guests when the service provides them. If a fresh profile receives no Home rows, it tries public Trending rows before Music while keeping Home selected. The compact icon rail, search pill, red play-mark branding and player controls follow the familiar YouTube TV layout. The player settings panel groups Quality, Captions, Audio, Speed and Repeat next to a smaller video preview. Account switching, QR/code sign-in and recommendations still use the inherited SmartTube integration; personalized rows require signing in within FilterTV. This is a familiar layout, not an exact replica of the official YouTube app or its recommendation algorithm. The sign-in QR is rendered on-device so its one-time code is not sent to an external QR image service.

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

ExoPlayer HTTP media requests → OkHttp application gate → Brave network matcher → redirect-aware network gate → server.

Initial blocks happen before DNS. Redirect blocks happen before HTTP bytes are sent, but OkHttp may already have established a connection. Cached responses still pass the initial gate. Metadata, artwork, account, casting and other upstream requests are outside this filter. All media transports are forced to OkHttp; upstream alternate-transport settings therefore have no effect in this prototype. Ads stitched into an allowed video stream cannot be removed by network filtering. Blocking a shared video/ad endpoint may also break playback, so custom rules should be tested carefully.

## Compatibility and validation

Build minimum: Android 5.0 / API 21. Configured native targets: ARMv7, ARM64 and x86. Actual codecs, native library availability, WebView and vendor firmware vary. Android TV / Google TV / Fire TV hardware testing is still required. Samsung Tizen and LG webOS are not Android.

Run `python tools/test-filter.py` for initial-request and redirect gate tests against a local HTTP server, and `cargo test --manifest-path bravefilter/Cargo.toml --locked` for Brave rule and exception matching. Before distributing, test D-pad navigation, sign-in, search, live/VOD playback, pause/seek, captions, background/resume, filtering with real lists, and update installation on real devices. Do not claim compatibility from compilation alone.

## Updates and publishing

The fork does not install upstream SmartTube APK updates. Merge upstream source fixes and rebuild this app. Publish to your own repository with the source and license notices intact. GitHub build artifacts expire; a release needs an intentionally signed APK and release notes. Never commit keystores, local.properties or account data.

Brave `adblock-rust` is MPL-2.0. EasyList and EasyPrivacy are downloaded from their authors and are dual licensed GPL-3.0-or-later / CC BY-SA 3.0; see [their license](https://easylist.to/pages/licence.html). No list contents are bundled in this repository.
