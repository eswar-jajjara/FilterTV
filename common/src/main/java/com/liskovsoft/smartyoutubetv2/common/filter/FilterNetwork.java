package com.liskovsoft.smartyoutubetv2.common.filter;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/** Media-only filtering. No URLs or account data are retained. */
public final class FilterNetwork {
    public static final String DEFAULT_RULES = "! Reserved test domain; no production blocklist bundled\n||ads.example.test^";
    private static volatile DomainFilter engine;
    private static volatile boolean enabled;
    public static final AtomicLong blocked = new AtomicLong();
    public static final AtomicLong evaluated = new AtomicLong();
    private FilterNetwork() {}
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("filtertv", Context.MODE_PRIVATE); }
    public static String rules(Context c) { return prefs(c).getString("rules", DEFAULT_RULES); }
    public static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }
    public static synchronized void load(Context c) {
        if (engine == null) { engine = new DomainFilter(rules(c)); enabled = enabled(c); }
    }
    public static synchronized void save(Context c, String rules, boolean active) {
        DomainFilter next = new DomainFilter(rules);
        prefs(c).edit().putString("rules", rules).putBoolean("enabled", active).apply();
        engine = next;
        enabled = active;
    }
    public static OkHttpClient wrap(Context c, OkHttpClient client) {
        load(c);
        Interceptor gate = new FilterInterceptor(() -> enabled ? engine : null, evaluated, blocked);
        // Application gate stops the initial request before DNS; network gate checks
        // follow-up redirects before HTTP bytes are sent (connection may be established).
        return client.newBuilder().addInterceptor(gate).addNetworkInterceptor(gate).build();
    }
}
