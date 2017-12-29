package com.yan.ijkplayertest.ijk;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * kinds of ijk callback
 * Created by yan on 2017/12/29 0029
 */
public interface IJKCallbacks extends IMediaPlayer.OnBufferingUpdateListener
        , IMediaPlayer.OnCompletionListener
        , IMediaPlayer.OnPreparedListener
        , IMediaPlayer.OnInfoListener
        , IMediaPlayer.OnVideoSizeChangedListener
        , IMediaPlayer.OnErrorListener
        , IMediaPlayer.OnSeekCompleteListener {
}
