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
import android.widget.SeekBar;
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
            , "http://flv2.bn.netease.com/videolib3/1604/28/fVobI0704/SD/fVobI0704-mobile.mp4"
    };
    private String currentUrl = urls[0];

    private static final long DURING = 4000;

    private final ArrayMap<IJKVideoRatio, String> arrayMap = new ArrayMap<>();

    private final TouchEventDell touchEventDell;

    private final IJKVideoPlayer ijkVideoPlayer;
    private View vBottom;
    private SeekBar sbProgress;
    private TextView tvScale;
    private TextView tvRatio;

    private Context context;

    private float controlFirstValue;

    public static ControlPanel attach(IJKVideoPlayer ijkVideoPlayer) {
        return new ControlPanel(ijkVideoPlayer);
    }

    private ControlPanel(IJKVideoPlayer ijkVideoPlayer) {
        this.ijkVideoPlayer = ijkVideoPlayer;
        this.ijkVideoPlayer.setOnTouchListener(this);
        this.ijkVideoPlayer.setListener(ijkCallbacksAdapter);
        context = ijkVideoPlayer.getContext();

        touchEventDell = new TouchEventDell();

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

        sbProgress = vBottom.findViewById(R.id.sb_progress);
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
                        int urlIndex = ++i % urls.length;
                        ijkVideoPlayer.setVideoPath(currentUrl = urls[urlIndex], urlIndex != 2);
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
                showPanel(isShow);
            }
        }, delay);
    }

    private void showPanel(boolean isShow) {
        showPanel(isShow, true);
    }

    private void showPanel(boolean isShow, boolean withDelayHide) {
        ijkVideoPlayer.removeCallbacks(asynHide);
        if (isShow) {
            setVisibility(VISIBLE);
            if (withDelayHide && ijkVideoPlayer.isPlaying()) {
                ijkVideoPlayer.postDelayed(asynHide, DURING);
            }
        } else if (ijkVideoPlayer.isPlaying()) {
            setVisibility(GONE);
        }
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
            progressUpdateTrigger(false);
            showPanel(true);
        } else {
            ijkVideoPlayer.start();
            showPanel(true, 50);
            progressUpdateTrigger(true);
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

    private int getVisibility() {
        return vBottom.getVisibility();
    }

    private void setProgress(float progress, int actionStatus) {
        if (ijkVideoPlayer.getDuration() == 0) {
            return;
        }
        if (actionStatus == 1) {
            progressUpdateTrigger(false);
            return;
        }
        final long videoDuring = ijkVideoPlayer.getDuration();
        final long change = (long) (progress * 90 * 1000);
        long seekPosition = Math.min(change + ijkVideoPlayer.getCurrentPosition(), videoDuring);
        seekPosition = Math.max(seekPosition, 0);
        sbProgress.setProgress((int) (seekPosition * 100 / videoDuring + 0.5F));
        if (actionStatus == 3) {
            ijkVideoPlayer.seekTo(seekPosition);
        }
    }

    private boolean updateProgress() {
        if (ijkVideoPlayer.getDuration() == 0) {
            return false;
        }
        sbProgress.setProgress((int) (ijkVideoPlayer.getCurrentPosition() * 100 / ijkVideoPlayer.getDuration() + 0.5F));
        return true;
    }

    /**
     * 设置当前view亮度
     *
     * @param percent 百分比
     */
    private void setLight(float percent, int actionStatus) {
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
            if (actionStatus == 1) {
                controlFirstValue = lp.screenBrightness;
                return;
            }

            float brightness = (float) Math.min(percent * 1.5 + controlFirstValue, 1F);
            lp.screenBrightness = Math.max(brightness, 0);
            activity.getWindow().setAttributes(lp);
        }
    }

    /**
     * @param percent      百分比
     * @param actionStatus 触屏事件
     */
    private void setVoice(float percent, int actionStatus) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
        final int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (actionStatus == 1) {
            controlFirstValue = percent;
            return;
        }
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final float volumeOffset = (percent - controlFirstValue) * maxVolume * 4 / 3;
        if (Math.abs(volumeOffset) > 1) {
            if (volumeOffset > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + 1, 0);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume - 1, 0);
            }
            controlFirstValue = percent;
        }
    }

    private void onPanelControl(int touchStatus, float percent, int actionStatus) {
        switch (touchStatus) {
            case 1://进度
                setProgress(percent, actionStatus);
                break;
            case 2://亮度
                setLight(percent, actionStatus);
                break;
            case 3://声音
                setVoice(percent, actionStatus);
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return touchEventDell.onTouch(v, event);
    }

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

    private void progressUpdateTrigger(boolean trigger) {
        if (trigger) {
            if (ijkVideoPlayer.isPlaying()) {
                ijkVideoPlayer.removeCallbacks(progressUpdate);
                ijkVideoPlayer.post(progressUpdate);
            }
        } else {
            ijkVideoPlayer.removeCallbacks(progressUpdate);
        }
    }

    private final Runnable progressUpdate = new Runnable() {
        @Override
        public void run() {
            if (updateProgress()) {
                ijkVideoPlayer.postDelayed(this, 500);
            }
        }
    };


    private final IJKCallbacksAdapter ijkCallbacksAdapter = new IJKCallbacksAdapter() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            switch (i) {
                case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    progressUpdateTrigger(true);
                    showPanel(true);
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    progressUpdateTrigger(false);
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                case IMediaPlayer.MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE:
                    progressUpdateTrigger(true);
                    break;
            }

            return super.onInfo(iMediaPlayer, i, i1);
        }
    };

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                ijkVideoPlayer.setVideoPath(currentUrl, !currentUrl.equals(urls[2]));
                break;
            case ON_PAUSE:
                ijkVideoPlayer.removeCallbacks(asynHide);
                break;
            case ON_DESTROY:
                source.getLifecycle().removeObserver(this);
                break;
        }
    }

    private class TouchEventDell implements View.OnTouchListener {
        private final float edgeSlop;
        private static final long TOUCH_DURING = 200;
        private long lastTime;

        private PointF lastTouchPoint = new PointF();
        private int touchStatus; // 1:进度 2:亮度 3:声音
        private float percent;
        private boolean breakTouchMoving;

        TouchEventDell() {
            edgeSlop = ViewConfiguration.get(context).getScaledEdgeSlop();
        }

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
                        if (touchStatus == 1) {
                            percent = (event.getX() - lastTouchPoint.x) / ijkVideoPlayer.getWidth();
                        } else {
                            percent = (lastTouchPoint.y - event.getY()) / ijkVideoPlayer.getHeight();
                        }
                        onPanelControl(touchStatus, percent, 2);
                        break;
                    }
                    if (Math.sqrt((event.getX() - lastTouchPoint.x) * (event.getX() - lastTouchPoint.x) + (event.getY() - lastTouchPoint.y) * (event.getY() - lastTouchPoint.y)) > edgeSlop) {
                        if (ijkVideoPlayer.getParent() != null) {
                            ijkVideoPlayer.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        if (Math.abs(event.getX() - lastTouchPoint.x) > Math.abs(event.getY() - lastTouchPoint.y)) {
                            touchStatus = 1;
                            showPanel(true, false);
                            ijkVideoPlayer.removeCallbacks(asynHide);
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
                        onPanelControl(touchStatus, percent, 1);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    breakTouchMoving = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    final boolean isMoved = touchStatus != 0;
                    onPanelControl(touchStatus, percent, 3);

                    breakTouchMoving = false;
                    percent = 0;
                    touchStatus = 0;

                    if (isMoved) {
                        showPanel(true);
                        return false;
                    }

                    playPauseTrigger(System.currentTimeMillis() - lastTime < TOUCH_DURING);
                    lastTime = System.currentTimeMillis();
                    break;
            }
            return true;
        }
    }

}