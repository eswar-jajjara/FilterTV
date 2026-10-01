package com.liskovsoft.smartyoutubetv2.tv;

import android.content.Context;
import com.google.android.exoplayer2.Format;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorManager;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.MediaTrack;
import com.liskovsoft.smartyoutubetv2.common.misc.AppResumeState;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ResumeAndAudioTest {
    @Test public void portraitResolutionUsesTheShortEdgeForQualityPresets() {
        assertEquals(1080, com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorUtil.getRealHeight(
                new Format.Builder().setWidth(1080).setHeight(1920).build()));
        assertEquals(720, com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorUtil.getRealHeight(
                new Format.Builder().setWidth(720).setHeight(1280).build()));
        assertEquals(1080, com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorUtil.getRealHeight(
                new Format.Builder().setWidth(1920).setHeight(1080).build()));
    }

    @Test public void homeRequiresFiveCompleteMinutesInBackground() {
        long left = 1_000_000;
        assertFalse(AppResumeState.shouldReturnHome(left, left + 299_999));
        assertTrue(AppResumeState.shouldReturnHome(left, left + 300_000));
        assertTrue(AppResumeState.shouldReturnHome(left, left + 600_000));
        assertFalse(AppResumeState.shouldReturnHome(0, left));
        assertFalse(AppResumeState.shouldReturnHome(left, left - 1));
    }

    @Test public void pauseIsRetainedUntilTheReturningScreenConsumesIt() {
        AppResumeState.consumePause();
        AppResumeState.requestPause();
        assertTrue(AppResumeState.shouldPause());
        assertTrue(AppResumeState.consumePause());
        assertFalse(AppResumeState.shouldPause());
        assertFalse(AppResumeState.consumePause());
    }

    @Test public void twoAudioGroupsRespectOriginalAndExplicitDub() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlayerData prefs = PlayerData.instance(context);
        String previous = prefs.getAudioLanguage();
        Method filter = TrackSelectorManager.class.getDeclaredMethod("filterByLanguage", MediaTrack[][].class, MediaTrack.class);
        filter.setAccessible(true);
        TrackSelectorManager selector = new TrackSelectorManager(context);
        MediaTrack original = ExoFormatItem.fromAudioSpecs("mp4a,en (original)").getTrack();
        MediaTrack dubbed = ExoFormatItem.fromAudioSpecs("mp4a,hi (dubbed)").getTrack();
        MediaTrack[][] groups = {{original}, {dubbed}};
        MediaTrack saved = ExoFormatItem.fromAudioSpecs("mp4a,en").getTrack();
        saved.isSaved = true;
        try {
            prefs.setAudioLanguage("");
            MediaTrack[][] result = (MediaTrack[][]) filter.invoke(selector, groups, saved);
            assertEquals(1, result.length);
            assertSame(original, result[0][0]);
            result = (MediaTrack[][]) filter.invoke(selector, groups, dubbed);
            assertEquals(1, result.length);
            assertSame(dubbed, result[0][0]);
            prefs.setAudioLanguage("hi");
            result = (MediaTrack[][]) filter.invoke(selector, groups, saved);
            assertSame(dubbed, result[0][0]);
        } finally { prefs.setAudioLanguage(previous); }
    }
}
