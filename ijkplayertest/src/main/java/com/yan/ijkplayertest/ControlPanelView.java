package com.yan.ijkplayertest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
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

import java.util.Map;

/**
 * Created by yan on 2017/12/27 0027
 */

public class ControlPanelView extends FrameLayout implements IJKOnInflateCallback, IJKOnConfigurationChanged {
    private static final String[] urls = new String[]{
            "http://183.252.176.25//PLTV/88888888/224/3221225925/index.m3u8"
            , "http://183.252.176.51//PLTV/88888888/224/3221225926/index.m3u8"
    };
    private String currentUrl = urls[0];

    private final ArrayMap<IJKVideoRatio, String> arrayMap;

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
                for (int i = 0; i < urls.length; i++) {
                    if (urls[i].equals(currentUrl)) {
                        ijkVideoPlayer.setVideoPath(currentUrl = urls[++i % urls.length]);
                        break;
                    }
                }
            }
        });

        tvScale = findViewById(R.id.tv_scale);
        tvScale.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ijkVideoPlayer.triggerOrientation();
            }
        });

        arrayMap = new ArrayMap<>();
        arrayMap.put(IJKVideoRatio.RATIO_FILL, getResources().getString(R.string.ratio_full));
        arrayMap.put(IJKVideoRatio.RATIO_ADAPTER, getResources().getString(R.string.ratio_adapter));
        arrayMap.put(IJKVideoRatio.RATIO_16_9, getResources().getString(R.string.ratio_16_9));
        arrayMap.put(IJKVideoRatio.RATIO_4_3, getResources().getString(R.string.ratio_4_3));
        tvRatio = findViewById(R.id.tv_ratio);
        tvRatio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = 0;
                for (Map.Entry<IJKVideoRatio, String> entry : arrayMap.entrySet()) {
                    if (ijkVideoPlayer.getIjkVideoRatio() == entry.getKey()) {
                        break;
                    }
                    index++;
                }
                index = (index + 1) % arrayMap.size();
                ijkVideoPlayer.setIjkVideoRatio(arrayMap.keyAt(index));
                tvRatio.setText(arrayMap.get(arrayMap.keyAt(index)));
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
        ijkVideoPlayer.setVideoPath(currentUrl);
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

        tvRatio.callOnClick();
    }

    @Override
    public void onConfigurationChanged() {
        textFullSmall();
    }

}
