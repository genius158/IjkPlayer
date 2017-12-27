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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.yan.ijkplayertest.ControlPanelView;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by yan on 2017/12/27 0027
 */

public class IJKVideoPlayer extends FrameLayout implements TextureView.SurfaceTextureListener, IJKOnConfigurationChanged
        , IMediaPlayer.OnBufferingUpdateListener
        , IMediaPlayer.OnCompletionListener
        , IMediaPlayer.OnPreparedListener
        , IMediaPlayer.OnInfoListener
        , IMediaPlayer.OnVideoSizeChangedListener
        , IMediaPlayer.OnErrorListener
        , IMediaPlayer.OnSeekCompleteListener {
    private static final String TAG = "IJKVideoPlayer";

    private IMediaPlayer mediaPlayer = null;

    private TextureView textureView;
    private Surface surface;

    private IJKOnConfigurationChanged ijkOnConfigurationChanged;

    private int surfaceWidth;
    private int surfaceHeight;

    private float ratio = 16F / 9F;

    private VideoPlayerListener listener;

    private String videoPath = "";

    private Context context;


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
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            ratio = (float) width / height;
            return;
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec((int) (width / ratio + 0.5F), MeasureSpec.EXACTLY));
    }

    /**
     * 创建一个新的ijkPlayer
     */
    private void createPlayer() {
        mediaRelease();

        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

        //开启硬解码
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);

        mediaPlayer = ijkMediaPlayer;
        mediaPlayer.setSurface(surface);

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    private void createSurface() {
        View panel = getChildAt(0);

        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT, Gravity.CENTER);
        textureView.setLayoutParams(layoutParams);
        super.addView(textureView);

        if (panel != null) {
            panel.bringToFront();
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

    private void setSurfaceParam(int width, int height) {
        if (textureView == null || width == 0 || height == 0) {
            return;
        }

        if (width > height * ratio) {
            float times = (float) getWidth() / width;
            surfaceWidth = getWidth();
            surfaceHeight = (int) (height * times + 0.5F);
        } else {
            float times = (float) getHeight() / height;
            surfaceHeight = getHeight();
            surfaceWidth = (int) (width * times + 0.5F);
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
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = -1;
        if (isScreenPortrait()) {
            layoutParams.height = -2;
            setLayoutParams(layoutParams);
            toolBarShowTrigger(true);
            notifyBarModeTrigger(false);
        } else {
            toolBarShowTrigger(false);
            layoutParams.width = -1;
            setLayoutParams(layoutParams);
            notifyBarModeTrigger(true);
        }
        if (ijkOnConfigurationChanged != null) {
            ijkOnConfigurationChanged.onConfigurationChanged();
        }

        // notify textureView reLayout
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right
                    , int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                setSurfaceParam(surfaceWidth, surfaceHeight);

            }
        });
    }

    private void notifyBarModeTrigger(boolean isTrans) {
        if (getActivity() == null) {
            return;
        }
        if (isTrans) {
            //设置全屏
            WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getActivity().getWindow().setAttributes(lp);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            //取消全屏
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
    public void setVideoPath(String path) {
        if (TextUtils.equals("", videoPath)) {
            //第一次播放
            videoPath = path;
            createSurface();
        } else {
            videoPath = path;
            loadVideo();
        }
    }

    public void setListener(VideoPlayerListener listener) {
        this.listener = listener;
    }

    @Override
    public final void addView(View child) {
        try {
            throw new Exception("this method can not be use");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void attachPanel(ControlPanelView controlPanelView) {
        super.addView(controlPanelView);
        if (controlPanelView instanceof IJKOnConfigurationChanged) {
            ijkOnConfigurationChanged = controlPanelView;
        }
        controlPanelView.onInflate(this, (LayoutParams) controlPanelView.getLayoutParams());
    }

    public void triggerOrientation() {
        if (getActivity() == null) {
            return;
        }
        if (isScreenPortrait()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
        Log.e(TAG, "onInfo: ");
        if (listener != null) {
            return listener.onInfo(iMediaPlayer, what, extra);
        }

        switch (what) {
            case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                break;
            case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                break;
            case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                break;
            case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                break;
            case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                break;
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