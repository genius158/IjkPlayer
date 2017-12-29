package com.yan.ijkplayertest;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.PointF;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
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

    private void playPauseTrigger(boolean trigger) {
        if (trigger) {
            playTrigger();
        } else {
            showPanel(llControl.getVisibility() != VISIBLE);
        }
    }

    /**
     * 设置当前view亮度
     *
     * @param percentOffset 百分几偏移量
     */
    private void setLight(float percentOffset) {
        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            if (lp.screenBrightness < 0) {
                // 获取系统亮度
                ContentResolver contentResolver = activity.getContentResolver();
                try {
                    lp.screenBrightness = Settings.System.getFloat(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255;
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
            float brightness = (float) Math.min(percentOffset * 1.5 + lp.screenBrightness, 1F);
            lp.screenBrightness = Math.max(brightness, 0);
            activity.getWindow().setAttributes(lp);
        }
    }

    /**
     * 记录声音偏移量
     */
    private float volumeOffset;

    /**
     * @param percentOffset 百分比偏移量
     * @param actionUp      点击事件是否为ACTION_UP
     */
    public void setVoice(float percentOffset, boolean actionUp) {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Service.AUDIO_SERVICE);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeOffset += percentOffset * maxVolume * 4 / 3;
        if (Math.abs(volumeOffset) > 1) {
            if (volumeOffset > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + 1, 0);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume - 1, 0);
            }
            volumeOffset = 0;
        }
        if (actionUp) {
            volumeOffset = 0;
        }
    }

    private void onPanelControl(int touchStatus, float percentOffset, boolean actionUp) {
        switch (touchStatus) {
            case 1://进度
                break;
            case 2://亮度
                setLight(percentOffset);
                break;
            case 3://声音
                setVoice(percentOffset, actionUp);
                break;
        }
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
    private int touchStatus; // 1:进度 2:亮度 3:声音
    private float percentOffset;
    private float lastPercent;
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
                    final float percent;
                    if (touchStatus == 1) {
                        percent = ((event.getX() - lastTouchPoint.x + 0.5F)) / getWidth();
                    } else {
                        percent = (lastTouchPoint.y - event.getY() + 0.5F) / getHeight();
                    }
                    onPanelControl(touchStatus, percentOffset = percent - lastPercent, false);
                    lastPercent = percent;
                    break;
                }
                if (Math.sqrt((event.getX() - lastTouchPoint.x) * (event.getX() - lastTouchPoint.x) + (event.getY() - lastTouchPoint.y) * (event.getY() - lastTouchPoint.y)) > edgeSlop) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(event.getX() - lastTouchPoint.x) > Math.abs(event.getY() - lastTouchPoint.y)) {
                        touchStatus = 1;
                    } else {
                        if (event.getX() < getWidth() / 3) {
                            touchStatus = 2;
                        } else if (event.getX() > getWidth() * 2 / 3) {
                            touchStatus = 3;
                        } else {
                            touchStatus = 1;
                        }
                    }
                    lastTouchPoint.set(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                breakTouchMoving = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                final boolean isMovingTrigger = touchStatus != 0;

                onPanelControl(touchStatus, percentOffset, isMovingTrigger);

                breakTouchMoving = false;
                lastPercent = 0;
                touchStatus = 0;

                if (isMovingTrigger) {
                    return false;
                }

                playPauseTrigger(System.currentTimeMillis() - lastTime < TOUCH_DURING);
                lastTime = System.currentTimeMillis();
                break;
        }

        return true;
    }
}
