package com.yan.ijkplayertest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.yan.ijkplayertest.ijk.IJKOnConfigurationChanged;
import com.yan.ijkplayertest.ijk.IJKOnInflateCallback;
import com.yan.ijkplayertest.ijk.IJKVideoPlayer;
import com.yan.ijkplayertest.ijk.IJKVideoRatio;

/**
 * Created by yan on 2017/12/27 0027
 */

public class ControlPanelView extends FrameLayout implements IJKOnInflateCallback, IJKOnConfigurationChanged {
    private static final long DURING = 5000;

    private IJKVideoPlayer ijkVideoPlayer;
    private TextView tvScale;
    private TextView tvRatio;

    public ControlPanelView(@NonNull Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.control_panel, this, true);
        findViewById(R.id.tv_next).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ijkVideoPlayer.setVideoPath("http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8");
            }
        });

        tvScale = findViewById(R.id.tv_scale);
        tvScale.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ijkVideoPlayer.triggerOrientation();
            }
        });

        final IJKVideoRatio[] viewRatios = new IJKVideoRatio[]{
                IJKVideoRatio.RATIO_FILL
                , IJKVideoRatio.RATIO_ADAPTER
                , IJKVideoRatio.RATIO_16_9
                , IJKVideoRatio.RATIO_4_3
        };
        final String[] strs = new String[]{
                getResources().getString(R.string.ratio_full)
                , getResources().getString(R.string.ratio_adapter)
                , getResources().getString(R.string.ratio_16_9)
                , getResources().getString(R.string.ratio_4_3)
        };
        tvRatio = findViewById(R.id.tv_ratio);
        tvRatio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = 0;
                for (int i = 0; i < viewRatios.length; i++) {
                    if (ijkVideoPlayer.getIjkVideoRatio() == viewRatios[i]) {
                        index = i;
                        break;
                    }
                }
                index = (index + 1) % viewRatios.length;
                ijkVideoPlayer.setIjkVideoRatio(viewRatios[index]);
                tvRatio.setText(strs[index]);
            }
        });
    }

    private void textFullSmall() {
        if (ijkVideoPlayer.isScreenPortrait()) {
            tvScale.setText(R.string.all_screen);
        } else {
            tvScale.setText(R.string.small_screen);
        }
    }

    private void loadIJKPlayer(IJKVideoPlayer ijkVideoPlayer) {
        this.ijkVideoPlayer = ijkVideoPlayer;
        this.ijkVideoPlayer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(getVisibility() != VISIBLE);
            }
        });
    }

    private void showPanel(boolean isShow) {
        removeCallbacks(asynHide);
        if (isShow) {
            setVisibility(VISIBLE);
            postDelayed(asynHide, DURING);
        } else {
            setVisibility(GONE);
        }
    }

    private final Runnable asynHide = new Runnable() {
        @Override
        public void run() {
            setVisibility(GONE);
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            showPanel(true);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        showPanel(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(asynHide);
    }

    @Override
    public void onInflate(IJKVideoPlayer ijkVideoPlayer, FrameLayout.LayoutParams layoutParams) {
        loadIJKPlayer(ijkVideoPlayer);
        layoutParams.height = -2;
        layoutParams.width = -1;
        layoutParams.gravity = Gravity.BOTTOM;
    }

    @Override
    public void onConfigurationChanged() {
        textFullSmall();
    }

}
