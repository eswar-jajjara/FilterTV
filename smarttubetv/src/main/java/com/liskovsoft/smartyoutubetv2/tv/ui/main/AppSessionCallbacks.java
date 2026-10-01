package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.liskovsoft.smartyoutubetv2.common.misc.AppResumeState;
import com.liskovsoft.smartyoutubetv2.common.misc.FocusedVideoPreloader;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.BrowseActivity;

/** Counts app activities so opening an in-app dialog does not end the session. */
public final class AppSessionCallbacks implements Application.ActivityLifecycleCallbacks {
    public static final String RETURN_HOME = "filtertv_return_home";
    private final SharedPreferences prefs;
    private int started;
    public AppSessionCallbacks(Context context) { prefs = context.getSharedPreferences("filtertv_session", Context.MODE_PRIVATE); }
    @Override public void onActivityStarted(Activity activity) {
        if (started++ != 0) return;
        long backgroundAt = prefs.getLong("background_at", 0);
        prefs.edit().remove("background_at").apply();
        if (backgroundAt != 0) AppResumeState.requestPause();
        if (AppResumeState.shouldReturnHome(backgroundAt, System.currentTimeMillis())) {
            // The launcher must finish its initialization before selecting Home.
            if (activity instanceof SplashActivity) {
                if (Intent.ACTION_MAIN.equals(activity.getIntent().getAction())) AppResumeState.requestHome();
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent home = new Intent(activity, BrowseActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(RETURN_HOME, true);
                activity.startActivity(home);
            });
        }
    }
    @Override public void onActivityStopped(Activity activity) {
        started = Math.max(0, started - 1);
        if (started == 0) {
            prefs.edit().putLong("background_at", System.currentTimeMillis()).apply();
            AppResumeState.requestPause();
            FocusedVideoPreloader.clear();
        }
    }
    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
