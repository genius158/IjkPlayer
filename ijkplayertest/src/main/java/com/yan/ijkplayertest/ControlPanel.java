package com.yan.ijkplayertest;


import android.app.Activity;
import android.app.Service;
import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.PointF;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import com.yan.ijkplayertest.ijk.IJKCallbacksAdapter;
import com.yan.ijkplayertest.ijk.IJKOnConfigurationChanged;
import com.yan.ijkplayertest.ijk.IJKVideoPlayer;
import com.yan.ijkplayertest.ijk.IJKVideoRatio;

import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by yan on 2017/12/27 0027
 */

public class ControlPanel implements GenericLifecycleObserver, IJKOnConfigurationChanged, View.OnClickListener, View.OnTouchListener {
    private static final String[] urls = new String[]{
            "http://183.252.176.25//PLTV/88888888/224/3221225925/index.m3u8"
            , "http://183.252.176.51//PLTV/88888888/224/3221225926/index.m3u8"
    };
    private String currentUrl = urls[0];

    private static final long DURING = 4000;

    private final ArrayMap<IJKVideoRatio, String> arrayMap = new ArrayMap<>();

    private final IJKVideoPlayer ijkVideoPlayer;
    private View vBottom;
    private TextView tvScale;
    private TextView tvRatio;

    private Context context;

    public static ControlPanel attach(IJKVideoPlayer ijkVideoPlayer) {
        return new ControlPanel(ijkVideoPlayer);
    }

    private ControlPanel(IJKVideoPlayer ijkVideoPlayer) {
        this.ijkVideoPlayer = ijkVideoPlayer;
        this.ijkVideoPlayer.setOnTouchListener(this);
        this.ijkVideoPlayer.setListener(ijkCallbacksAdapter);
        context = ijkVideoPlayer.getContext();

        edgeSlop = ViewConfiguration.get(context).getScaledEdgeSlop();

        arrayMap.put(IJKVideoRatio.RATIO_FILL, context.getResources().getString(R.string.ratio_full));
        arrayMap.put(IJKVideoRatio.RATIO_ADAPTER, context.getResources().getString(R.string.ratio_adapter));
        arrayMap.put(IJKVideoRatio.RATIO_16_9, context.getResources().getString(R.string.ratio_16_9));
        arrayMap.put(IJKVideoRatio.RATIO_4_3, context.getResources().getString(R.string.ratio_4_3));

        vBottom = LayoutInflater.from(context).inflate(R.layout.control_panel, ijkVideoPlayer, false);
        ijkVideoPlayer.attachPanel(vBottom);
        vBottom.findViewById(R.id.tv_next).setOnClickListener(this);

        tvScale = vBottom.findViewById(R.id.tv_scale);
        tvScale.setOnClickListener(this);

        tvRatio = vBottom.findViewById(R.id.tv_ratio);
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

    private void showPanel(final boolean isShow, long delay) {
        ijkVideoPlayer.postDelayed(new Runnable() {
            @Override
            public void run() {
                ijkVideoPlayer.removeCallbacks(asynHide);
                if (isShow) {
                    setVisibility(VISIBLE);
                    if (ijkVideoPlayer.isPlaying()) {
                        ijkVideoPlayer.postDelayed(asynHide, DURING);
                    }
                } else if (ijkVideoPlayer.isPlaying()) {
                    setVisibility(GONE);
                }
            }
        }, delay);
    }

    private void showPanel(final boolean isShow) {
        showPanel(isShow, 0);
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

    private void playPauseTrigger(boolean trigger) {
        if (trigger) {
            playTrigger();
        } else {
            showPanel(getVisibility() != VISIBLE);
        }
    }

    private void setVisibility(int visibility) {
        vBottom.setVisibility(visibility);
    }

    public int getVisibility() {
        return vBottom.getVisibility();
    }

    /**
     * 设置当前view亮度
     *
     * @param percentOffset 百分几偏移量
     */
    private void setLight(float percentOffset) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
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
        AudioManager audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
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


    //------------------------ START touch event dell part ---------------------------
    private float edgeSlop;
    private static final long TOUCH_DURING = 500;
    private long lastTime;

    private PointF lastTouchPoint = new PointF();
    private int touchStatus; // 1:进度 2:亮度 3:声音
    private float percentOffset;
    private float lastPercent;
    private boolean breakTouchMoving;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
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
                        percent = ((event.getX() - lastTouchPoint.x + 0.5F)) / ijkVideoPlayer.getWidth();
                    } else {
                        percent = (lastTouchPoint.y - event.getY() + 0.5F) / ijkVideoPlayer.getHeight();
                    }
                    onPanelControl(touchStatus, percentOffset = percent - lastPercent, false);
                    lastPercent = percent;
                    break;
                }
                if (Math.sqrt((event.getX() - lastTouchPoint.x) * (event.getX() - lastTouchPoint.x) + (event.getY() - lastTouchPoint.y) * (event.getY() - lastTouchPoint.y)) > edgeSlop) {
                    if (ijkVideoPlayer.getParent() != null) {
                        ijkVideoPlayer.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(event.getX() - lastTouchPoint.x) > Math.abs(event.getY() - lastTouchPoint.y)) {
                        touchStatus = 1;
                    } else {
                        if (event.getX() < ijkVideoPlayer.getWidth() / 3) {
                            touchStatus = 2;
                        } else if (event.getX() > ijkVideoPlayer.getWidth() * 2 / 3) {
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
    //------------------------ END touch event dell part ---------------------------

    @Override
    public void onConfigurationChanged() {
        textFullSmallTrigger();
    }

    private final Runnable asynHide = new Runnable() {
        @Override
        public void run() {
            setVisibility(GONE);
        }
    };

    private final IJKCallbacksAdapter ijkCallbacksAdapter = new IJKCallbacksAdapter() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            if (i == 3) {// video start to play
                showPanel(true);
            }
            return super.onInfo(iMediaPlayer, i, i1);
        }
    };

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                ijkVideoPlayer.setVideoPath(currentUrl);
                break;
            case ON_PAUSE:
                ijkVideoPlayer.removeCallbacks(asynHide);
                break;
            case ON_DESTROY:
                source.getLifecycle().removeObserver(this);
                break;
        }
    }
}