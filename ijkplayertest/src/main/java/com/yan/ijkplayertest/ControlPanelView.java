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

/**
 * Created by yan on 2017/12/27 0027
 */

public class ControlPanelView extends FrameLayout implements IJKOnInflateCallback, IJKOnConfigurationChanged {
    private static final long DURING = 5000;

    private IJKVideoPlayer ijkVideoPlayer;

    public ControlPanelView(@NonNull Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.control_panel, this, true);
        findViewById(R.id.tv_next).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ijkVideoPlayer.setVideoPath("http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8");
            }
        });

        final TextView tvScale = findViewById(R.id.tv_scale);
        tvScale.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ijkVideoPlayer.triggerOrientation();
                if (ijkVideoPlayer.isScreenPortrait()) {
                    tvScale.setText(R.string.small_screen);
                } else {
                    tvScale.setText(R.string.all_screen);
                }
            }
        });
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

    Runnable asynHide = new Runnable() {
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

    }

}
