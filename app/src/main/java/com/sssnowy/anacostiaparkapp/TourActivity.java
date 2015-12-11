package com.sssnowy.anacostiaparkapp;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class TourActivity extends AppCompatActivity {
    private int currentZone = -1;
    private ImageButton playButton;
    private LinearLayout linearLayoutTranscript;
    private int cnt = 0;
    private Handler audioHandler;
    private Runnable audioRunnable;
    private TreeMap<Integer, String> transcript;
    private MusicService musicService;
    private boolean serviceBound = false;
    private ServiceConnection serviceConnection;

    double[][][] polygons = {
            {{38.819745, -77.170083},
            {38.819711, -77.168661},
            {38.819189, -77.166832},
            {38.816568, -77.168307},
            {38.817754, -77.170129}},
            {{38.820163, -77.169949},
            {38.819164, -77.166381},
            {38.822182, -77.165636},
            {38.822169, -77.169965}}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //initialize
        playButton = (ImageButton)findViewById(R.id.playButton);
        linearLayoutTranscript = (LinearLayout)findViewById(R.id.linearLayoutTranscript);
        audioHandler = new Handler();
        audioRunnable = new Runnable() {
            @Override
            public void run() {
                if(musicService.isPlaying()) {
                    highlightTranscript();
                    audioHandler.postDelayed(this, 100);
                }
            }
        };



        /*
        note: dropdown for zone?

        If location changes,
            If enters new zone,
                If audio is playing,
                    (should not happen)
                If audio is not playing,
                    audioProgress reset to 0
                    populate transcript map
                    populate linear layout
                    play new zone audio
                    postdelayed highlight function
                currentZone = zone
        If playButton is clicked,
            If audio is not playing,
                If transcript filled,
                    play audio
                    postdelayed highlight function
            If audio is playing,
                Audio paused
        If new zone manually selected,
            audioProgress reset to 0
            populate transcript map
            populate linear layout
        */

        //--------------------------------------------------------------------------------------------------------Listeners
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            //If location changes,
            @Override
            public void onLocationChanged(Location location) {
                Button triggerButton = (Button)findViewById(R.id.triggerButton);
                triggerButton.setText(location.getLatitude() + "," + location.getLongitude());
                Toast.makeText(TourActivity.this, "loc changed", Toast.LENGTH_SHORT).show();
                int zone = getZone(location.getLatitude(), location.getLongitude());
                //If enters new zone,
                if(currentZone != zone){
                    //If audio is not playing,
                    if(!musicService.isPlaying()) {
                        //populate transcript map
                        transcript = getTranscriptFromTextFile(getFilenameFromZone(zone));
                        //populate linear layout
                        populateLinearLayoutTranscript();
                        //play new zone audio
                        musicService.setAudio(getApplicationContext(), getResidFromZone(zone));
                        musicService.playAudio();
//                        playAudio(getResidFromZone(zone));
                        playButton.setBackgroundResource(R.drawable.pause);
                        //postdelayed highlight function
                        audioHandler.postDelayed(audioRunnable, 1000);
                    }
                    //currentZone = zone
                    currentZone = zone;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                Toast.makeText(TourActivity.this, "on", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(TourActivity.this, "off", Toast.LENGTH_SHORT).show();
            }
        };

        if(checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(TourActivity.this, "turn on your GPS", Toast.LENGTH_SHORT).show();
        }


        //----------------------------------------------------------------------------------------------------If play button is clicked,
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startService(new Intent(MusicService.ACTION_PLAY));
//                Intent msIntent = new Intent(TourActivity.this, MusicService.class)
//                msIntent.setAction("com.sssnowy.anacostiaparkapp.action.ACTION_PLAY");
//                startService(msIntent);
                //----------------------------------------------------------------------------------------------------If audio is not playing,
                if (!musicService.isPlaying()) {
                    //----------------------------------------------------------------------------------------------------If transcript filled,
                    if (linearLayoutTranscript.getChildCount() > 0) {
                        //----------------------------------------------------------------------------------------------------play audio
                        musicService.playAudio();
                        playButton.setBackgroundResource(R.drawable.pause);
                        //----------------------------------------------------------------------------------------------------postDelayed highlight function
                        audioHandler.postDelayed(audioRunnable, 1000);
                    }
                    //----------------------------------------------------------------------------------------------------If audio is playing,
                } else {
                    //----------------------------------------------------------------------------------------------------pause audio
                    musicService.pauseAudio();
                    playButton.setBackgroundResource(R.drawable.play);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //--------------------------------------------------------------------------------------------------------Music Service Connection
        Intent msIntent = new Intent(getApplicationContext(), MusicService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Toast.makeText(TourActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
                MusicService.LocalBinder localBinder = (MusicService.LocalBinder)service;
                musicService = localBinder.getServiceInstance();
                serviceBound = true;
                audioHandler.post(audioRunnable);
                Log.e("mylogs","Service Connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(TourActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
                musicService = null;
                serviceBound = false;
                Log.e("mylogs", "Service Disconnected");
            }
        };
        bindService(msIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        audioHandler.removeCallbacks(audioRunnable);
        if(serviceBound){
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    public int getZone(double latitude, double longitude){
        //TJ 38.8184974,-77.168681
        for(int cnt = 0; cnt < polygons.length; cnt++) {
            int intersections = numberOfLinesCrossed(polygons[cnt], latitude, longitude);
            Toast.makeText(TourActivity.this, intersections + "", Toast.LENGTH_SHORT).show();
            if(intersections % 2 == 1) {
                return cnt;
            }
        }
        return 1;
    }

    public int getResidFromZone(int zone){
        Button triggerButton = (Button)findViewById(R.id.triggerButton);
        triggerButton.setText(triggerButton.getText() + " " + zone);
        if(zone == 0){
            return R.raw.greyarea;
        } else if(zone == 1){
            return R.raw.empirestateofmind;
        } else if(zone == 2){
            return R.raw.paradise;
        } else {
            return R.raw.paradise;
        }
    }

    public String getFilenameFromZone(int zone){
        if(zone == 0){
            return "greyarea.txt";
        } else if(zone == 1){
            return "empirestateofmind.txt";
        } else if(zone == 2){
            return "paradise.txt";
        } else {
            return "paradise.txt";
        }
    }

//    public void playAudio(int resid) {
//        mp.reset();
//        mp = MediaPlayer.create(TourActivity.this, resid);
//        mp.start();
//    }

    public int numberOfLinesCrossed(double[][] polygon, double latitude, double longitude){
        int intersections = 0;
        for(int cnt = 0; cnt < polygon.length; cnt++){
            double[] point1 = polygon[cnt];
            double[] point2 = polygon[(cnt + 1) % polygon.length];
            //if the latitudes are the same, aka the slope is horizontal
            if(point1[0] == point2[0]){
                continue;
            }
            //get slope & y-int of line between point1 and point2
            double m = (point2[0] - point1[0])/(point2[1] - point1[1]);
            double b = point1[0] - m * point1[1];
            //find the x value of the intersection point between the horizontal line that runs through the user location and the point1/point2 line
            double x = (1/m)*(latitude - b);
            //if x is in the range both lines
            if((point1[1] - x < 0 ^ point2[1] - x < 0) && x > longitude){
                intersections += 1;
            }
        }
        return intersections;
    }

    public TreeMap<Integer, String> getTranscriptFromTextFile(String filename){
        BufferedReader bufferedReader = null;
        final TreeMap<Integer, String> transcript = new TreeMap<>();
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open(filename)));
            String line;
            while((line = bufferedReader.readLine()) != null){
                String[] splitLine = line.split("=");
                transcript.put(Integer.parseInt(splitLine[0]), splitLine[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch(IOException e){

                }
            }
        }
        return transcript;
    }

    public void populateLinearLayoutTranscript(){
        String[] transcriptArray = transcript.values().toArray(new String[transcript.size()]);
        for(int cnt = 0; cnt < transcriptArray.length; cnt++){
            TextView textView = new TextView(this);
            textView.setText(transcriptArray[cnt]);
            textView.setTextColor(Color.parseColor("#66FFFFFF"));
            textView.setTextSize(18);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setPadding(5, 5, 5, 5);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayoutTranscript.addView(textView);
        }
    }

    public void highlightTranscript(){
        final int indexOfChild = getIndexFromAudioProgress();
        if(indexOfChild != 0) {
            linearLayoutTranscript.getChildAt(indexOfChild - 1).setBackgroundColor(ContextCompat.getColor(this, R.color.aws_colorDark));
            linearLayoutTranscript.getChildAt(indexOfChild - 1).setPadding(0, 5, 0, 5);
            ((TextView)linearLayoutTranscript.getChildAt(indexOfChild - 1)).setTextColor(Color.parseColor("#66FFFFFF"));
            ((TextView)linearLayoutTranscript.getChildAt(indexOfChild - 1)).setTextSize(18);
            ((TextView)linearLayoutTranscript.getChildAt(indexOfChild - 1)).setGravity(Gravity.CENTER_HORIZONTAL);
        }
        linearLayoutTranscript.getChildAt(indexOfChild).setBackgroundResource(R.drawable.rounded_corner);
        //linearLayoutTranscript.getChildAt(indexOfChild).setBackgroundColor(Color.parseColor("#666666"));
        //linearLayoutTranscript.getChildAt(indexOfChild).setPadding(0, 10, 0, 10);
        ((TextView)linearLayoutTranscript.getChildAt(indexOfChild)).setTextColor(Color.parseColor("#FFFFFF"));
        ((TextView)linearLayoutTranscript.getChildAt(indexOfChild)).setTextSize(20);
        ((TextView)linearLayoutTranscript.getChildAt(indexOfChild)).setGravity(Gravity.CENTER);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ((ScrollView) findViewById(R.id.scrollViewTranscript)).scrollTo(0, linearLayoutTranscript.getChildAt(indexOfChild).getTop() - ((ScrollView) findViewById(R.id.scrollViewTranscript)).getHeight() / 2);
            }
        });
    }

    public int getIndexFromAudioProgress(){
        Integer[] transcriptTimes = transcript.keySet().toArray(new Integer[transcript.keySet().size()]);
        for(int cnt = 0; cnt < transcriptTimes.length; cnt++){
            if(transcriptTimes[cnt] > musicService.getCurrentPosition()){
                return cnt - 1;
            }
        }
        return transcriptTimes.length - 1;
    }


}
