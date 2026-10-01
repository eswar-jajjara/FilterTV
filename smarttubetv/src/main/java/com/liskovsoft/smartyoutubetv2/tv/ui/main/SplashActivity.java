package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.content.Intent;
import android.os.Bundle;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;

public class SplashActivity extends MotherActivity implements SplashView {
    private static final String TAG = SplashActivity.class.getSimpleName();
    private Intent mNewIntent;
    private SplashPresenter mPresenter;
    private final android.os.Handler mLaunchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable mLaunch = () -> mPresenter.onViewInitialized();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNewIntent = getIntent();

        mPresenter = SplashPresenter.instance(this);
        mPresenter.setView(this);
        if (Intent.ACTION_MAIN.equals(mNewIntent.getAction())) {
            android.widget.FrameLayout root = new android.widget.FrameLayout(this);
            root.setBackgroundColor(android.graphics.Color.rgb(15, 15, 15));
            android.widget.LinearLayout brand = new android.widget.LinearLayout(this);
            brand.setGravity(android.view.Gravity.CENTER);
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageResource(com.liskovsoft.smartyoutubetv2.tv.R.mipmap.app_icon);
            int size = (int) (72 * getResources().getDisplayMetrics().density);
            brand.addView(icon, new android.widget.LinearLayout.LayoutParams(size, size));
            android.widget.TextView title = new android.widget.TextView(this);
            title.setText("FilterTV"); title.setTextColor(android.graphics.Color.WHITE); title.setTextSize(32);
            brand.addView(title);
            root.addView(brand, new android.widget.FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER));
            setContentView(root);
            brand.setAlpha(0.35f); brand.setScaleX(0.94f); brand.setScaleY(0.94f);
            brand.animate().alpha(1).scaleX(1).scaleY(1).setDuration(220).start();
            mLaunchHandler.postDelayed(mLaunch, 220);
        } else mPresenter.onViewInitialized();

        //finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = intent;
        setIntent(intent);
        mLaunchHandler.removeCallbacks(mLaunch);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        mLaunchHandler.removeCallbacks(mLaunch);
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    public void finishView() {
        try {
            finish();
        } catch (NullPointerException e) {
            // NullPointerException: Attempt to invoke virtual method 'void com.android.server.wm.DisplayContent.moveStack(com.android.server.wm.TaskStack, boolean)'
            e.printStackTrace();
        }
    }
}
