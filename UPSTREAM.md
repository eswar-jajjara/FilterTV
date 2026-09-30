# Upstream attribution

FilterTV is a derivative of [SmartTube](https://github.com/yuliskov/SmartTube), not an independently written YouTube client.

Base commit: a7212c531ac06ec87108180d5d8ab1bebebe528e.
The original MIT license and copyright remain in LICENSE. Modules and dependencies retain their own licenses.
SharedModules and MediaServiceCore are pinned Git submodules. Their implementation is unmodified.
The existing Java/Leanback/ExoPlayer architecture is retained; this is not a Compose/Media3 rewrite.

FilterTV changes: separate app ID/name, media domain filtering, FilterLab diagnostics, upstream APK-update guard, and build/test workflow.
FilterTV adds its own vector launcher mark. Remaining in-app artwork and interface are inherited from SmartTube.
