package com.yan.ijkplayertest.ijk;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by yan on 2017/12/27 0027
 */

public class IJKVideoPlayer extends FrameLayout implements TextureView.SurfaceTextureListener, IJKOnConfigurationChanged, IJKCallbacks {
    private static final String TAG = "IJKVideoPlayer";

    private IMediaPlayer mediaPlayer;

    private TextureView textureView;
    private Surface surface;

    private int videoWidth;
    private int videoHeight;

    private final float DEFAULT_SCREEN_RATIO = 16F / 9F;
    private float screenRatio = DEFAULT_SCREEN_RATIO;
    private IJKVideoRatio ijkVideoRatio = IJKVideoRatio.RATIO_FILL;

    private IJKCallbacksAdapter listener;

    private String videoPath;
    private boolean isLive;

    private Context context;

    private List<IJKOnConfigurationChanged> configurationChangeds;


    public IJKVideoPlayer(@NonNull Context context) {
        this(context, null);
    }

    public IJKVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec((int) (width / screenRatio + 0.5F), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setSurfaceParam(videoWidth, videoHeight);
    }

    /**
     * create a new ijkPlayer
     */
    private void createPlayer() {
        mediaRelease();

        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024 * 16);
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 50000);
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
        if (isLive) {
            // Param for living
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        } else {
            // Param for playback
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 0);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 0);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1);
        }

        mediaPlayer = ijkMediaPlayer;
        mediaPlayer.setSurface(surface);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    private void createSurface() {
        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT, Gravity.CENTER);
        textureView.setLayoutParams(layoutParams);
        super.addView(textureView);

        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) != textureView) {
                getChildAt(i).bringToFront();
            }
        }
    }

    private void loadVideo() {
        createPlayer();

        try {
            mediaPlayer.setDataSource(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.prepareAsync();
    }

    private void mediaRelease() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.setSurface(null);
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSurfaceParam(int videoWidth, int videoHeight) {
        if (textureView == null || videoWidth == 0 || videoHeight == 0) {
            return;
        }
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;

        int surfaceWidth;
        int surfaceHeight;

        float fixedRatio = 0;
        switch (ijkVideoRatio) {
            case RATIO_16_9:
                fixedRatio = 16F / 9F;
            case RATIO_4_3:
                if (fixedRatio == 0) {
                    fixedRatio = 4F / 3F;
                }
                if (screenRatio > fixedRatio) {
                    surfaceHeight = getMeasuredHeight();
                    surfaceWidth = (int) (surfaceHeight * fixedRatio + 0.5F);
                } else {
                    surfaceWidth = getMeasuredWidth();
                    surfaceHeight = (int) (surfaceWidth / fixedRatio + 0.5F);
                }
                break;
            case RATIO_FILL:
                surfaceWidth = getMeasuredWidth();
                surfaceHeight = getMeasuredHeight();
                break;
            default://RATIO_ADAPTER
                if (videoWidth > videoHeight * screenRatio) {
                    float times = (float) getMeasuredWidth() / videoWidth;
                    surfaceWidth = getMeasuredWidth();
                    surfaceHeight = (int) (videoHeight * times + 0.5F);
                } else {
                    float times = (float) getMeasuredHeight() / videoHeight;
                    surfaceHeight = getMeasuredHeight();
                    surfaceWidth = (int) (videoWidth * times + 0.5F);
                }
        }

        LayoutParams layoutParams = (LayoutParams) textureView.getLayoutParams();
        if (layoutParams.width == surfaceWidth && layoutParams.height == surfaceHeight) {
            return;
        }
        layoutParams.width = surfaceWidth;
        layoutParams.height = surfaceHeight;
        textureView.setLayoutParams(layoutParams);
    }

    private void toolBarShowTrigger(boolean isShow) {
        if (context instanceof AppCompatActivity) {
            android.support.v7.app.ActionBar supportActionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (supportActionBar != null) {
                if (isShow) {
                    supportActionBar.show();
                } else {
                    supportActionBar.hide();
                }
            }
        } else if (getActivity() != null) {
            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                if (isShow) {
                    actionBar.show();
                } else {
                    actionBar.hide();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged() {
        final boolean isScreenPortrait = isScreenPortrait();

        screenRatioTrigger(isScreenPortrait);
        toolBarShowTrigger(isScreenPortrait);
        notifyBarModeTrigger(isScreenPortrait);

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = isScreenPortrait ? -2 : -1;
        setLayoutParams(layoutParams);

        if (configurationChangeds == null) {
            return;
        }
        for (IJKOnConfigurationChanged configurationChanged : configurationChangeds) {
            configurationChanged.onConfigurationChanged();
        }
    }

    private void screenRatioTrigger(boolean isScreenPortrait) {
        if (isScreenPortrait) {
            screenRatio = DEFAULT_SCREEN_RATIO;
        } else {
            final int width = context.getResources().getDisplayMetrics().widthPixels;
            final int height = context.getResources().getDisplayMetrics().heightPixels;
            screenRatio = (float) width / height;
        }
    }

    private void notifyBarModeTrigger(boolean isSmall) {
        if (getActivity() == null) {
            return;
        }
        if (!isSmall) {
            //fullScreen
            WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getActivity().getWindow().setAttributes(lp);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            //smallScreen
            WindowManager.LayoutParams attr = getActivity().getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getActivity().getWindow().setAttributes(attr);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    private Activity getActivity() {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }


    // --------------------------- api -------------------------

    /**
     * @param path the path of the video.
     */
    public void setVideoPath(String path, boolean isLive) {
        this.isLive = isLive;
        if (videoPath == null) {
            //first play
            videoPath = path;
            createSurface();
        } else {
            videoPath = path;
            loadVideo();
        }
    }

    public void setListener(IJKCallbacksAdapter listener) {
        this.listener = listener;
    }

    public void setIjkVideoRatio(IJKVideoRatio ijkVideoRatio) {
        this.ijkVideoRatio = ijkVideoRatio;
        setSurfaceParam(videoWidth, videoHeight);
    }

    @Override
    public final void addView(View child) {
        try {
            throw new Exception("can not call this method");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void attachPanel(View v) {
        super.addView(v);
        if (v instanceof IJKOnInflateCallback) {
            ((IJKOnInflateCallback) v).onInflate(this);
        }
        if (v instanceof IJKOnConfigurationChanged) {
            addConfigurationChanged((IJKOnConfigurationChanged) v);
        }
    }

    public void addConfigurationChanged(IJKOnConfigurationChanged onConfigurationChanged) {
        if (configurationChangeds == null) {
            configurationChangeds = new ArrayList<>();
        }
        configurationChangeds.add(onConfigurationChanged);
    }

    public void triggerOrientation() {
        if (getActivity() == null) {
            return;
        }
        if (isScreenPortrait()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public boolean isScreenPortrait() {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public void reset() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
    }

    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void seekTo(long l) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(l);
        }
    }

    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    public IJKVideoRatio getIjkVideoRatio() {
        return ijkVideoRatio;
    }

    //---------------------- textureView callbacks -------------------------
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable: ");
        surface = new Surface(surfaceTexture);
        loadVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged: ");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed: ");
        mediaRelease();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    //---------------------- mediaPlayer callbacks -------------------------
    @Override
    public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int width, int height, int sarNum, int sarDen) {
        Log.e(TAG, "onVideoSizeChanged: " + width + "    " + height + "    " + sarNum + "   " + sarDen);
        if (listener != null) {
            listener.onVideoSizeChanged(iMediaPlayer, width, height, sarNum, sarDen);
        }
        setSurfaceParam(width, height);
    }

    @Override
    public void onSeekComplete(IMediaPlayer iMediaPlayer) {
        Log.e(TAG, "onSeekComplete: ");
        if (listener != null) {
            listener.onSeekComplete(iMediaPlayer);
        }
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        Log.e(TAG, "onPrepared: " + iMediaPlayer.getVideoHeight() + "   " + iMediaPlayer.getVideoWidth());
        if (listener != null) {
            listener.onPrepared(iMediaPlayer);
        }
        setSurfaceParam(iMediaPlayer.getVideoWidth(), iMediaPlayer.getVideoHeight());
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
        Log.e(TAG, "onInfo: " + what);

        switch (what) {
            case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                Log.e(TAG, "media_info_video_track_lagging:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                Log.e(TAG, "media_info_video_rendering_start:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.e(TAG, "media_info_buffering_start:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.e(TAG, "media_info_buffering_end:");
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                Log.e(TAG, "media_info_network_bandwidth: " + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.e(TAG, "media_info_bad_interleaving:");
                break;
            case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                Log.e(TAG, "media_info_not_seekable:");
                break;
            case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                Log.e(TAG, "media_info_metadata_update:");
                break;
            case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                Log.e(TAG, "media_info_unsupported_subtitle:");
                break;
            case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                Log.e(TAG, "media_info_subtitle_timed_out:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                Log.e(TAG, "media_info_video_rotation_changed: " + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                Log.e(TAG, "media_info_audio_rendering_start:");
                break;
        }
        if (listener != null) {
            return listener.onInfo(iMediaPlayer, what, extra);
        }
        return true;
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
        Log.e(TAG, "onError: ");
        if (listener != null) {
            return listener.onError(iMediaPlayer, what, extra);
        }
        return false;
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        Log.e(TAG, "onCompletion: ");
        if (listener != null) {
            listener.onCompletion(iMediaPlayer);
        }
        iMediaPlayer.start();
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
        Log.e(TAG, "onBufferingUpdate: " + percent);
        if (listener != null) {
            listener.onBufferingUpdate(iMediaPlayer, percent);
        }
    }

}