package com.sssnowy.anacostiaparkapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by 2016dzhao on 11/23/2015.
 */
public class MediaPlayerSingleton {
    private static MediaPlayer instance = null;
    private static int zonePlaying = -1;

    protected MediaPlayerSingleton(){}

    public static MediaPlayer getInstance(Context c) {
        Log.e("MediaPlayerSingleton","Context : " + c);
        if(instance == null){
            instance = MediaPlayer.create(c, R.raw.paradise);
        }
        return instance;
    }

    public static MediaPlayer setInstance(Context c, int resid){
        instance = MediaPlayer.create(c, resid);
        return instance;
    }

    public static int getZonePlaying(){
        return zonePlaying;
    }

    public static void setZonePlaying(int n){
        zonePlaying = n;
    }
}
