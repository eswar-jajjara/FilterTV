package com.liskovsoft.smartyoutubetv2.common.misc;

import android.os.SystemClock;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/** At most one VOD information request; never prefetches video bytes. */
public final class FocusedVideoPreloader {
    private static String videoId;
    private static long expiresAt;
    private static Observable<MediaItemFormatInfo> request;
    private static Disposable warmup;
    private static Disposable connection;
    private static Runnable pending;
    private FocusedVideoPreloader() {}
    public static void focus(Video video) {
        cancelFocus();
        if (video == null || video.videoId == null || video.isLive || video.isUpcoming) return;
        String id = video.videoId;
        pending = () -> {
            Observable<MediaItemFormatInfo> source = playback(id);
            if (warmup == null || warmup.isDisposed())
                warmup = source.subscribe(info -> {}, error -> clear());
        };
        Utils.postDelayed(pending, 600);
    }
    public static synchronized Observable<MediaItemFormatInfo> playback(String id) {
        cancelFocus();
        if (request == null || !id.equals(videoId) || SystemClock.elapsedRealtime() >= expiresAt) {
            if (warmup != null) warmup.dispose();
            if (connection != null) connection.dispose();
            connection = null;
            warmup = null;
            videoId = id;
            expiresAt = SystemClock.elapsedRealtime() + 60_000;
            request = YouTubeServiceManager.instance().getMediaItemService().getFormatInfoObserve(id)
                    .replay(1).autoConnect(1, disposable -> connection = disposable);
        }
        return request;
    }
    public static void cancelFocus() {
        if (pending != null) Utils.removeCallbacks(pending);
        pending = null;
    }
    public static synchronized void clear() {
        cancelFocus();
        if (warmup != null) warmup.dispose();
        if (connection != null) connection.dispose();
        connection = null;
        warmup = null;
        request = null;
        videoId = null;
    }
}
