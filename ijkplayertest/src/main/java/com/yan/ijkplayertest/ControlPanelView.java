package com.yan.ijkplayertest;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.yan.ijkplayertest.ijk.IJKCallbacksAdapter;
import com.yan.ijkplayertest.ijk.IJKOnConfigurationChanged;
import com.yan.ijkplayertest.ijk.IJKOnInflateCallback;
import com.yan.ijkplayertest.ijk.IJKVideoPlayer;
import com.yan.ijkplayertest.ijk.IJKVideoRatio;

import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by yan on 2017/12/27 0027
 */

public class ControlPanelView extends FrameLayout implements IJKOnInflateCallback, IJKOnConfigurationChanged, View.OnClickListener {
    private static final String[] urls = new String[]{
            "http://183.252.176.25//PLTV/88888888/224/3221225925/index.m3u8"
            , "http://183.252.176.51//PLTV/88888888/224/3221225926/index.m3u8"
    };
    private String currentUrl = urls[0];

    private final ArrayMap<IJKVideoRatio, String> arrayMap;

    private static final long DURING = 5000;

    private IJKVideoPlayer ijkVideoPlayer;
    private View llControl;
    private TextView tvScale;
    private TextView tvRatio;

    public ControlPanelView(@NonNull Context context) {
        super(context);
        edgeSlop = ViewConfiguration.get(context).getScaledEdgeSlop();

        LayoutInflater.from(context).inflate(R.layout.control_panel, this, true);
        findViewById(R.id.tv_next).setOnClickListener(this);

        tvScale = findViewById(R.id.tv_scale);
        llControl = findViewById(R.id.ll_control);
        tvScale.setOnClickListener(this);

        arrayMap = new ArrayMap<>();
        arrayMap.put(IJKVideoRatio.RATIO_FILL, getResources().getString(R.string.ratio_full));
        arrayMap.put(IJKVideoRatio.RATIO_ADAPTER, getResources().getString(R.string.ratio_adapter));
        arrayMap.put(IJKVideoRatio.RATIO_16_9, getResources().getString(R.string.ratio_16_9));
        arrayMap.put(IJKVideoRatio.RATIO_4_3, getResources().getString(R.string.ratio_4_3));
        tvRatio = findViewById(R.id.tv_ratio);
        tvRatio.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_ratio:
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
                break;

            case R.id.tv_next:
                for (int i = 0; i < urls.length; i++) {
                    if (urls[i].equals(currentUrl)) {
                        ijkVideoPlayer.setVideoPath(currentUrl = urls[++i % urls.length]);
                        break;
                    }
                }
                break;

            case R.id.tv_scale:
                ijkVideoPlayer.triggerOrientation();
                break;
        }
        showPanel(true);
    }

    private void loadIJKPlayer(IJKVideoPlayer ijkVideoPlayer) {
        this.ijkVideoPlayer = ijkVideoPlayer;
        this.ijkVideoPlayer.setListener(ijkCallbacksAdapter);
    }

    private void textFullSmallTrigger() {
        if (ijkVideoPlayer.isScreenPortrait()) {
            tvScale.setText(R.string.all_screen);
        } else {
            tvScale.setText(R.string.small_screen);
        }
    }

    private void playTrigger() {
        if (ijkVideoPlayer.isPlaying()) {
            ijkVideoPlayer.pause();
            showPanel(true);
        } else {
            ijkVideoPlayer.start();
            showPanel(true, 50);
        }
    }

    private void showPanel(final boolean isShow, long delay) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                removeCallbacks(asynHide);
                if (isShow) {
                    llControl.setVisibility(VISIBLE);
                    if (ijkVideoPlayer.isPlaying()) {
                        postDelayed(asynHide, DURING);
                    }
                } else if (ijkVideoPlayer.isPlaying()) {
                    llControl.setVisibility(GONE);
                }
            }
        }, delay);
    }

    private void showPanel(final boolean isShow) {
        showPanel(isShow, 0);
    }

    private final IJKCallbacksAdapter ijkCallbacksAdapter = new IJKCallbacksAdapter() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            if (i == 3) {// video start to play
                showPanel(true);
            }
            return super.onInfo(iMediaPlayer, i, i1);
        }
    };

    private final Runnable asynHide = new Runnable() {
        @Override
        public void run() {
            llControl.setVisibility(GONE);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
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
        layoutParams.height = -1;
        layoutParams.width = -1;

        tvRatio.callOnClick();
    }

    @Override
    public void onConfigurationChanged() {
        textFullSmallTrigger();
    }


    //------------------------ touch event dell part ---------------------------
    private float edgeSlop;
    private static final long TOUCH_DURING = 500;
    private long lastTime;

    private PointF lastTouchPoint = new PointF();
    private int touchMoveDistance;
    private int touchStatus = 0; //1 horizontal , 2 vertical
    private boolean breakTouchMoving;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ijkVideoPlayer == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchPoint.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (breakTouchMoving) {
                    break;
                }

                if (touchStatus != 0) {
                    if (touchStatus == 1) {
                        touchMoveDistance = (int) (event.getX() - lastTouchPoint.x + 0.5F);
                    } else {
                        touchMoveDistance = (int) (event.getY() - lastTouchPoint.y + 0.5F);
                    }
                    break;
                }
                if (Math.sqrt((event.getX() - lastTouchPoint.x) * (event.getX() - lastTouchPoint.x) + (event.getY() - lastTouchPoint.y) * (event.getY() - lastTouchPoint.y)) > edgeSlop) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(event.getX() - lastTouchPoint.x) > Math.abs(event.getY() - lastTouchPoint.y)) {
                        touchStatus = 1;
                    } else {
                        touchStatus = 2;
                    }
                    lastTouchPoint.set(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                breakTouchMoving = true;
                break;
            case MotionEvent.ACTION_UP:
                if (touchStatus == 1) {
                    Log.e("MotionEvent1", "onTouchEvent: " + ((float) touchMoveDistance / getWidth()));
                } else {
                    Log.e("MotionEvent2", "onTouchEvent: " + ((float) touchMoveDistance / getHeight()));
                }

            case MotionEvent.ACTION_CANCEL:
                if (System.currentTimeMillis() - lastTime < TOUCH_DURING) {
                    playTrigger();
                } else {
                    showPanel(llControl.getVisibility() != VISIBLE);
                }
                lastTime = System.currentTimeMillis();

                breakTouchMoving = false;
                touchStatus = 0;
                break;
        }

        return true;
    }

}
