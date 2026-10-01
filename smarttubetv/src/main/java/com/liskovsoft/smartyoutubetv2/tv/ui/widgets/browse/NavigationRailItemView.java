package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.leanback.widget.NonOverlappingLinearLayout;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Reflows with the rail width without replacing Leanback's focus listeners. */
public final class NavigationRailItemView extends NonOverlappingLinearLayout {
    private Boolean lastExpanded;
    public NavigationRailItemView(Context context, AttributeSet attrs) { super(context, attrs); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        // Leanback's shadow wrapper measures its child with wrap-content. Use the
        // rail container's assigned width so hiding the label cannot shrink the row forever.
        android.view.ViewParent parent = getParent();
        while (parent instanceof View) {
            View ancestor = (View) parent;
            if (ancestor.getId() == androidx.leanback.R.id.browse_headers_root) {
                int width = ancestor.getLayoutParams().width;
                if (width > 0) widthSpec = MeasureSpec.makeMeasureSpec(Math.max(dp(44), width - dp(16)), MeasureSpec.EXACTLY);
                break;
            }
            parent = parent.getParent();
        }
        super.onMeasure(widthSpec, heightSpec);
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boolean expanded = w > dp(100);
        if (lastExpanded != null && lastExpanded == expanded) return;
        lastExpanded = expanded;
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL | (expanded ? android.view.Gravity.START : android.view.Gravity.CENTER_HORIZONTAL));
        setPadding(dp(expanded ? 16 : 4), 0, dp(8), 0);
        TextView label = findViewById(R.id.header_label);
        ImageView icon = findViewById(R.id.header_icon);
        if (label != null) {
            label.setVisibility(expanded ? View.VISIBLE : View.GONE);
            label.setTextSize(14);
            label.setMaxWidth(dp(140));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) label.getLayoutParams();
            lp.topMargin = 0;
            lp.setMarginStart(dp(12));
            label.setLayoutParams(lp);
        }
        if (icon != null) icon.setImageTintList(androidx.core.content.ContextCompat.getColorStateList(getContext(), R.color.filtertv_navigation_text));
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
