package com.liskovsoft.smartyoutubetv2.common.filter;

/** JNI boundary for Brave's adblock-rust network matcher. */
public final class BraveFilterEngine {
    private static final String SOURCE = "https://www.youtube.com/";
    private static final boolean AVAILABLE;
    static {
        boolean loaded;
        try {
            System.loadLibrary("filtertv_adblock");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }
    private BraveFilterEngine() {}
    public static boolean available() { return AVAILABLE; }
    public static boolean replaceRules(String rules) {
        return AVAILABLE && nativeReplaceRules(rules);
    }
    public static boolean shouldBlock(String url, String method) {
        return AVAILABLE && nativeShouldBlock(url, SOURCE, "media", method);
    }
    private static native boolean nativeReplaceRules(String rules);
    private static native boolean nativeShouldBlock(String url, String source, String requestType, String method);
}
