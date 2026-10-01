package com.liskovsoft.smartyoutubetv2.common.misc;

import android.os.SystemClock;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.Observable;
import java.util.concurrent.TimeUnit;

/** Shares the actual YouTube Shorts response between Home and the Shorts tab. */
public final class ShortsFeedRepository {
    private static Observable<MediaGroup> feed;
    private static long expiresAt;
    private ShortsFeedRepository() {}
    public static Observable<MediaGroup> observe() { return Observable.defer(ShortsFeedRepository::current); }
    private static synchronized Observable<MediaGroup> current() {
        if (feed == null || SystemClock.elapsedRealtime() >= expiresAt) {
            expiresAt = SystemClock.elapsedRealtime() + 120_000;
            feed = YouTubeServiceManager.instance().getContentService().getShortsObserve()
                    .timeout(15, TimeUnit.SECONDS).filter(group -> !group.isEmpty())
                    .takeLast(1).doOnError(error -> clear()).cache();
        }
        return feed;
    }
    public static synchronized void clear() { feed = null; expiresAt = 0; }
}
