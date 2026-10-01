package com.liskovsoft.smartyoutubetv2.common.misc;

/** Shared lifecycle decision, independent of a particular player instance. */
public final class AppResumeState {
    public static final long HOME_TIMEOUT_MS = 5 * 60 * 1000;
    private static boolean pauseRequested;
    private static boolean homeRequested;
    public static void requestHome() { homeRequested = true; }
    public static boolean consumeHome() {
        boolean result = homeRequested;
        homeRequested = false;
        return result;
    }
    private AppResumeState() {}
    public static boolean shouldReturnHome(long backgroundAt, long now) {
        return backgroundAt > 0 && now >= backgroundAt && now - backgroundAt >= HOME_TIMEOUT_MS;
    }
    public static void requestPause() { pauseRequested = true; }
    public static boolean shouldPause() { return pauseRequested; }
    public static boolean consumePause() {
        boolean result = pauseRequested;
        pauseRequested = false;
        return result;
    }
}
