package com.sssnowy.anacostiaparkapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by 2016dzhao on 12/4/2015.
 */
public class MusicService extends Service {
    private MediaPlayer mp;
    private IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        mp = new MediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void setAudio(Context c, final int resid){
        mp.reset();
        mp = MediaPlayer.create(c, resid);
        mp.seekTo(0);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TourActivity.AUDIO_ZONES_SHARED_PREFERENCES, Context.MODE_PRIVATE);
                sharedPreferences.edit().putBoolean("resid" + resid, true).apply();
                Log.e("mylogs","Audio Finished Playing");
                mp.seekTo(0);
            }
        });
    }

    public void playAudio(){
        mp.start();
    }

    public void pauseAudio(){
        mp.pause();
    }

    public boolean isPlaying(){
        return mp.isPlaying();
    }

    public int getCurrentPosition(){
        return mp.getCurrentPosition();
    }

    public int getAudioLength(){
        return mp.getDuration();
    }

    public void seekTo(int progress){
        mp.seekTo(progress);
    }

    public class LocalBinder extends Binder {
        public MusicService getServiceInstance(){
            return MusicService.this;
        }
    }
}
