package com.yan.ijkplayertest.ijk;


import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by yan on 2017/12/27 0027
 */
public abstract class VideoPlayerListener implements IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnSeekCompleteListener {
}