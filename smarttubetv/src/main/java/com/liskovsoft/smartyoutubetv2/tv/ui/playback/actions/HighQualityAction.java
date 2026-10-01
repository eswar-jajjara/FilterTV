package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * An action for displaying a HQ (High Quality) icon.
 */
public class HighQualityAction extends Action {
    public HighQualityAction(Context context) {
        super(R.id.lb_control_high_quality);
        setIcon(ContextCompat.getDrawable(context, R.drawable.icon_settings));
        setLabel1(context.getString(
                R.string.filtertv_player_settings));
    }
}
