package com.liskovsoft.smartyoutubetv2.common.filter;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.OkHttpClient;

/** App-owned media request gate. Lists compile away from the UI thread. */
public final class FilterNetwork {
    public static final String DEFAULT_RULES = "! Personal network rules\n||ads.example.test^";
    private static final String[] LIST_URLS = {
        "https://easylist-downloads.adblockplus.org/easylist.txt",
        "https://easylist-downloads.adblockplus.org/easyprivacy.txt"
    };
    private static final String[] LIST_FILES = {"easylist.txt", "easyprivacy.txt"};
    private static final int MAX_LIST_BYTES = 8 * 1024 * 1024;
    private static final long REFRESH_MS = 7L * 24 * 60 * 60 * 1000;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "FilterTV-lists");
        thread.setDaemon(true);
        return thread;
    });
    public static final AtomicLong blocked = new AtomicLong();
    public static final AtomicLong evaluated = new AtomicLong();
    private static volatile boolean enabled;
    private static volatile boolean ready;
    private static volatile String status = "Starting";
    private static boolean started;

    private FilterNetwork() {}
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("filtertv", Context.MODE_PRIVATE); }
    public static String rules(Context c) { return prefs(c).getString("rules", DEFAULT_RULES); }
    public static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", true); }
    public static String status() { return status; }
    public static boolean ready() { return ready; }

    public static synchronized void load(Context context) {
        if (started) return;
        started = true;
        Context app = context.getApplicationContext();
        enabled = enabled(app);
        if (!BraveFilterEngine.available()) {
            status = "Native Brave engine unavailable; playback continues";
            return;
        }
        WORKER.execute(() -> {
            rebuild(app);
            if (enabled && System.currentTimeMillis() - prefs(app).getLong("lists_updated", 0) >= REFRESH_MS) updateLists(app);
        });
    }

    public static void save(Context context, String customRules, boolean active) {
        if (customRules == null || customRules.length() > 65536) throw new IllegalArgumentException("Personal rules exceed 64 KiB");
        Context app = context.getApplicationContext();
        prefs(app).edit().putString("rules", customRules).putBoolean("enabled", active).apply();
        enabled = active;
        status = "Compiling rules";
        WORKER.execute(() -> {
            rebuild(app);
            if (active && System.currentTimeMillis() - prefs(app).getLong("lists_updated", 0) >= REFRESH_MS) updateLists(app);
        });
    }

    public static void refresh(Context context) {
        Context app = context.getApplicationContext();
        status = "Downloading lists";
        WORKER.execute(() -> updateLists(app));
    }

    public static boolean testUrl(String url) { return ready && BraveFilterEngine.shouldBlock(url, "GET"); }

    public static OkHttpClient wrap(Context context, OkHttpClient client) {
        load(context);
        FilterInterceptor gate = new FilterInterceptor((url, method) -> enabled && ready && BraveFilterEngine.shouldBlock(url, method), evaluated, blocked);
        // First gate covers cached requests; network gate covers redirect destinations.
        return client.newBuilder().addInterceptor(gate).addNetworkInterceptor(gate).build();
    }

    private static void rebuild(Context context) {
        if (!BraveFilterEngine.available()) {
            ready = false;
            status = "Native Brave engine unavailable";
            return;
        }
        try {
            StringBuilder all = new StringBuilder(rules(context));
            for (String name : LIST_FILES) {
                File file = listFile(context, name);
                if (file.isFile()) all.append('\n').append(readLimited(new FileInputStream(file), MAX_LIST_BYTES));
            }
            if (BraveFilterEngine.replaceRules(all.toString())) {
                ready = true;
                status = "Brave engine ready; " + cachedListCount(context) + " cached lists";
            } else {
                status = "Could not compile Brave rules";
            }
        } catch (IOException | RuntimeException error) {
            status = "Could not load filter lists";
        }
    }

    private static int cachedListCount(Context context) {
        int count = 0;
        for (String name : LIST_FILES) if (listFile(context, name).isFile()) count++;
        return count;
    }

    private static File listFile(Context context, String name) { return new File(context.getFilesDir(), "filtertv-" + name); }

    private static void updateLists(Context context) {
        int updated = 0;
        for (int i = 0; i < LIST_URLS.length; i++) {
            try {
                URL url = new URL(LIST_URLS[i]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", "FilterTV/1.0");
                try {
                    if (connection.getResponseCode() != 200) throw new IOException("List HTTP status");
                    String list = readLimited(connection.getInputStream(), MAX_LIST_BYTES);
                    if (list.length() < 10000 || !list.contains("||")) throw new IOException("Unexpected list");
                    File target = listFile(context, LIST_FILES[i]);
                    File temp = new File(target.getPath() + ".new");
                    try (FileOutputStream output = new FileOutputStream(temp)) {
                        output.write(list.getBytes(StandardCharsets.UTF_8));
                    }
                    if (!temp.renameTo(target)) throw new IOException("Could not replace list");
                    updated++;
                } finally { connection.disconnect(); }
            } catch (IOException error) {
                // Cached lists remain active when a source is unavailable.
            }
        }
        if (updated > 0) rebuild(context);
        if (updated == LIST_URLS.length) prefs(context).edit().putLong("lists_updated", System.currentTimeMillis()).apply();
        if (updated == 0 && cachedListCount(context) == 0) status = "Lists unavailable; personal rules only";
    }

    private static String readLimited(InputStream input, int limit) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0, count;
            while ((count = stream.read(buffer)) != -1) {
                total += count;
                if (total > limit) throw new IOException("Filter list too large");
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        }
    }
}
