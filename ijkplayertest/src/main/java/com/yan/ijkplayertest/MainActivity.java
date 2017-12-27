package com.yan.ijkplayertest;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.yan.ijkplayertest.ijk.IJKVideoPlayer;


/**
 * Created by yan on 2017/12/27 0027
 */

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    IJKVideoPlayer ijkVideoPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ijkVideoPlayer = findViewById(R.id.ijk_player);
        ijkVideoPlayer.attachPanel(new ControlPanelView(this));
        ijkVideoPlayer.setVideoPath("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8");

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ijkVideoPlayer.onConfigurationChanged();
    }
}
