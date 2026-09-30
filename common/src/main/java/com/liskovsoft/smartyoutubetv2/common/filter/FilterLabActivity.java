package com.liskovsoft.smartyoutubetv2.common.filter;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.net.URI;

/** Native remote-focusable diagnostics; testing a URL never sends a request. */
public final class FilterLabActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(32 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        body.setBackgroundColor(Color.rgb(18, 24, 36));
        scroll.addView(body); setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("FilterLab · media request filtering"); title.setTextSize(26); title.setTextColor(Color.WHITE); body.addView(title);
        TextView help = new TextView(this);
        help.setText("Experimental domain rules. This switch does not control YouTube ad-free playback.\nUse ||domain.example^ to block, @@||domain.example^ to allow. Changes apply to subsequent media requests.");
        help.setTextColor(Color.LTGRAY); body.addView(help);
        Switch active = new Switch(this); active.setText("Enable media domain filter"); active.setTextColor(Color.WHITE);
        active.setChecked(FilterNetwork.enabled(this)); body.addView(active);
        EditText rules = new EditText(this); rules.setTextColor(Color.WHITE); rules.setMinLines(3);
        rules.setText(FilterNetwork.rules(this)); body.addView(rules);
        TextView result = new TextView(this); result.setTextColor(Color.WHITE); result.setTextSize(20);
        Button save = new Button(this); save.setText("Save rules and switch"); body.addView(save);
        save.setOnClickListener(v -> { try {
            FilterNetwork.save(this, rules.getText().toString(), active.isChecked()); result.setText("Saved. Rules: " + new DomainFilter(rules.getText().toString()).size());
        } catch (IllegalArgumentException e) { result.setText(e.getMessage()); } });
        EditText url = new EditText(this); url.setSingleLine(true); url.setTextColor(Color.WHITE);
        url.setText("https://ads.example.test/banner.mp4"); body.addView(url);
        Button test = new Button(this); test.setText("Test URL locally"); body.addView(test);
        test.setOnClickListener(v -> { try {
            URI uri = new URI(url.getText().toString());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) throw new IllegalArgumentException("Enter an HTTP(S) URL");
            String rule = new DomainFilter(rules.getText().toString()).matchedRule(uri.getHost());
            result.setText(rule == null ? "ALLOW" : "BLOCK: " + rule);
        } catch (Exception e) { result.setText("Invalid input: " + e.getMessage()); } });
        Button stats = new Button(this); stats.setText("Refresh session counters"); body.addView(stats);
        stats.setOnClickListener(v -> result.setText("Gate evaluations: " + FilterNetwork.evaluated.get() + " · blocked: " + FilterNetwork.blocked.get()));
        body.addView(result); active.requestFocus();
    }
}
