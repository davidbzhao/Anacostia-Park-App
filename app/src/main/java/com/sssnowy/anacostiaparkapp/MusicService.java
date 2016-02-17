package com.sssnowy.anacostiaparkapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by 2016dzhao on 12/4/2015.
 */
public class MusicService extends Service {
    private MediaPlayer mp;
    private int curResid;
    public static final String ACTION_PLAY = "com.sssnowy.anacostiaparkapp.action.ACTION_PLAY";
    private IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        mp = new MediaPlayer();
        Log.e("mylogs","Music Service: onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("mylogs", "onStartCommand ------------------------------ " + intent.getAction());
//        if(intent.getAction() != null){
//            if(intent.getAction().equals(ACTION_PLAY)){
//                setAudio(MusicService.this, R.raw.empirestateofmind);
//            }
//        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("UserAction", "Music Service: override onDestroy");
        //stopSelf();
//        mp.release();
    }

    public void setAudio(Context c, int resid){
        curResid = resid;
        mp.reset();
        mp = MediaPlayer.create(c, resid);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
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

    public int getResid(){
        return curResid;
    }

    public int getCurrentPosition(){
        return mp.getCurrentPosition();
    }

    public int getAudioLength(){
        return mp.getDuration();
    }

    public class LocalBinder extends Binder {
        public MusicService getServiceInstance(){
            return MusicService.this;
        }
    }
}
