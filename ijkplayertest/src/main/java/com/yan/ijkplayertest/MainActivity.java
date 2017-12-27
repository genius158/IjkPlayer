package com.yan.ijkplayertest;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

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

        RecyclerView recyclerView = findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false)) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView tv = holder.itemView.findViewById(android.R.id.text1);
                tv.setTextColor(Color.parseColor("#999999"));
                tv.setText("TEXT TEST " + position);
            }

            @Override
            public int getItemCount() {
                return 18;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ijkVideoPlayer.onConfigurationChanged();
    }

    @Override
    public void onBackPressed() {
        if (ijkVideoPlayer.isScreenPortrait()) {
            super.onBackPressed();
        } else {
            ijkVideoPlayer.triggerOrientation();
        }
    }
}
