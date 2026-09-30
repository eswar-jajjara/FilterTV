#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK="${ANDROID_NDK_HOME:-${ANDROID_HOME:?Set ANDROID_HOME}/ndk/21.0.6113669}"
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
test -x "$BIN/aarch64-linux-android21-clang"

rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$BIN/aarch64-linux-android21-clang"
export CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="$BIN/armv7a-linux-androideabi21-clang"
export CARGO_TARGET_I686_LINUX_ANDROID_LINKER="$BIN/i686-linux-android21-clang"

for item in 'aarch64-linux-android arm64-v8a' 'armv7-linux-androideabi armeabi-v7a' 'i686-linux-android x86'; do
  read -r target abi <<< "$item"
  cargo build --manifest-path "$ROOT/bravefilter/Cargo.toml" --locked --release --target "$target"
  mkdir -p "$ROOT/common/src/main/jniLibs/$abi"
  cp "$ROOT/bravefilter/target/$target/release/libfiltertv_adblock.so" "$ROOT/common/src/main/jniLibs/$abi/libfiltertv_adblock.so"
done
