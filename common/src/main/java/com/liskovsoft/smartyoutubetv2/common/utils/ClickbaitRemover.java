package com.liskovsoft.smartyoutubetv2.common.utils;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.regex.Pattern;

public class ClickbaitRemover {
    public static final int THUMB_QUALITY_DEFAULT = 0;
    public static final int THUMB_QUALITY_START = 1;
    public static final int THUMB_QUALITY_MIDDLE = 2;
    public static final int THUMB_QUALITY_END = 3;

    private static final Pattern THUMB_QUALITY_PATTERN = Pattern.compile("/(hq1|hq2|hq3|hqdefault|mqdefault|sddefault|hq720)\\.");

    public static String updateThumbnail(String thumbUrl, int thumbQuality) {
        if (thumbUrl == null || thumbQuality == THUMB_QUALITY_DEFAULT) {
            return thumbUrl;
        }

        String quality = "hqdefault";

        switch (thumbQuality) {
            case THUMB_QUALITY_START:
                quality = "hq1";
                break;
            case THUMB_QUALITY_MIDDLE:
                quality = "hq2";
                break;
            case THUMB_QUALITY_END:
                quality = "hq3";
                break;
        }

        return Helpers.replace(thumbUrl, THUMB_QUALITY_PATTERN, "/" + quality + ".");
    }

    public static String updateThumbnail(Video video, int thumbQuality) {
        if (video == null) {
            return null;
        }

        if (video.isLive || video.isUpcoming || video.altCardImageUrl != null) { // priority to DeArrow
            return video.getCardImageUrl();
        }

        String url = updateThumbnail(video.getCardImageUrl(), thumbQuality);
        // Upgrade only standard YouTube video thumbnails. Live, Shorts, DeArrow and
        // custom frame choices retain their source. The presenter falls back on HTTP failure.
        if (thumbQuality == THUMB_QUALITY_DEFAULT && !video.isShorts && url != null
                && (url.startsWith("https://i.ytimg.com/vi/") || url.startsWith("https://i.ytimg.com/vi_webp/"))) {
            url = url.replace("/mqdefault.", "/hq720.").replace("/hqdefault.", "/hq720.");
        }
        return url;
    }
}
