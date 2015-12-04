package com.sssnowy.anacostiaparkapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by 2016dzhao on 12/4/2015.
 */
public class MusicService extends Service {
    public static MediaPlayer mp;
    public static final String ACTION_PLAY = "com.sssnowy.anacostiaparkapp.action.ACTION_PLAY";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mp = new MediaPlayer();
        Log.e("onCreate-","onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("onStartCommand-", "onStartCommand" + intent.getAction());
        if(intent.getAction().equals(ACTION_PLAY)){
            setSong(MusicService.this, R.raw.empirestateofmind);
            mp.start();
        }
        return START_STICKY;
    }

    public void setSong(Context c, int resid){
        mp.reset();
        mp = MediaPlayer.create(c, resid);
    }

    @Override
    public void onDestroy() {
        Log.e("onDestroy-", "onDestroy");
        mp.release();
    }
}
