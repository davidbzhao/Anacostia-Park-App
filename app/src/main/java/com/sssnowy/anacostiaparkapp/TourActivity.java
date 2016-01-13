package com.sssnowy.anacostiaparkapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.TreeMap;

public class TourActivity extends Activity {
    private boolean serviceBound = false;
    private Handler audioHandler;
    private ImageButton playButton;
    private int currentZone = -1;
    private LinearLayout linearLayoutTranscript;
    private MusicService musicService;
    private Runnable audioRunnable;
    private ServiceConnection serviceConnection;
    private TreeMap<Integer, String> transcript;
    private ScrollView scrollViewTranscript;
    private int scrollingInt = 0;

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
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "~( .. )~", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        Log.e("mylogs", "BUILD VERSION: " + Build.VERSION.SDK_INT);

        Log.e("mylogs", "-onCreate");
        //initialize
        playButton = (ImageButton) findViewById(R.id.playButton);
        linearLayoutTranscript = (LinearLayout) findViewById(R.id.linearLayoutTranscript);
        scrollViewTranscript = (ScrollView) findViewById(R.id.scrollViewTranscript);
        audioHandler = new Handler();
        audioRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService.isPlaying()) {
                    if(linearLayoutTranscript.getChildCount() > 0) {
                        highlightTranscript();
                        audioHandler.postDelayed(this, 100);
                    }
                }
            }
        };


        setPlayButtonToPlay();
//        if(savedInstanceState != null){
//            Log.e("mylogs", "savedInstanceState NOT NULL");
//        }
        //--------------------------------------------------------------------------------------------------------Listeners
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

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            //If location changes,
            @Override
            public void onLocationChanged(Location location) {
                Log.e("UserAction","Location Changed");
                Toast.makeText(TourActivity.this, "loc changed", Toast.LENGTH_SHORT).show();
                int zone = getZone(location.getLatitude(), location.getLongitude());
                //If enters new zone,
                if (currentZone != zone) {
                    //If audio is not playing,
                    if(serviceBound) {
                        if (!musicService.isPlaying()) {
                            //populate transcript map
                            transcript = getTranscriptFromTextFile(getFilenameFromZone(zone));
                            //populate linear layout
                            populateLinearLayoutTranscript();
                            //play new zone audio
                            musicService.setAudio(getApplicationContext(), getResidFromZone(zone));
                            musicService.playAudio(getApplicationContext(), getResidFromZone(zone));
//                        playAudio(getResidFromZone(zone));
                            setPlayButtonToPause();
                            //playButton.setBackgroundResource(R.drawable.pause);
                            //postdelayed highlight function
                            audioHandler.postDelayed(audioRunnable, 1000);
                        }
                        //currentZone = zone
                        currentZone = zone;
                    }
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

        if (checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(TourActivity.this, "turn on your GPS", Toast.LENGTH_SHORT).show();
        }


        //If play button is clicked,
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("UserAction","Play Button Clicked");
                //If audio is not playing,
                if (!musicService.isPlaying()) {
                    //If transcript filled,
                    if (linearLayoutTranscript.getChildCount() > 0) {
                        //play audio
                        musicService.playAudio(getApplicationContext(), getResidFromZone(currentZone));
                        setPlayButtonToPause();
//                        playButton.setBackgroundResource(R.drawable.pause);
                        //postDelayed highlight function
                        audioHandler.postDelayed(audioRunnable, 1000);
                    }
                    //If audio is playing,
                } else {
                    //pause audio
                    musicService.pauseAudio();
                    setPlayButtonToPlay();
//                    playButton.setBackgroundResource(R.drawable.play);
                }
            }
        });

        scrollViewTranscript.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    Log.e("UserAction","Scroll View Touched ACTION_UP");
                    Log.e("mylogs", "ACTION_UP");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollingInt -= 1;
                            if(scrollingInt < 0){
                                Log.e("mylogs","WOAHWOAHWOAH!!!! That ain't right");
                            }
                        }
                    }, 1000);
                } else if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.e("UserAction","Scroll View Touched ACTION_DOWN");
                    Log.e("mylogs", "ACTION_DOWN");
                    scrollingInt += 1;
                }
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Music Service Connection
        Intent msIntent = new Intent(getApplicationContext(), MusicService.class);
        serviceConnection = getServiceConnection();
        bindService(msIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.e("mylogs", "---onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("mylogs", "---onStop");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentZone", currentZone);
        Log.e("mylogs", "-----onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("mylogs", "---Pause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("mylogs", "onDestroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("mylogs", "-onRestart");
    }

    @Override
    public void onBackPressed() {
        Log.e("mylogs","BACK BUTTON PRESSED");
        Intent intent = new Intent(TourActivity.this, MenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    public int[] convertIntegerArrayToPrimitive(Integer[] classArray){
        int[] primitiveArray = new int[classArray.length];
        for(int cnt = 0; cnt < classArray.length; cnt++){
            primitiveArray[cnt] = classArray[cnt].intValue();
        }
        return primitiveArray;
    }

    public int getZone(double latitude, double longitude) {
        //TJ 38.8184974,-77.168681
        for (int cnt = 0; cnt < polygons.length; cnt++) {
            int intersections = numberOfLinesCrossed(polygons[cnt], latitude, longitude);
            Toast.makeText(TourActivity.this, intersections + "", Toast.LENGTH_SHORT).show();
            if (intersections % 2 == 1) {
                return cnt;
            }
        }
        return 1;
    }

    public int getResidFromZone(int zone) {
        if (zone == 0) {
            return R.raw.greyarea;
        } else if (zone == 1) {
            return R.raw.empirestateofmind;
        } else if (zone == 2) {
            return R.raw.paradise;
        } else {
            return R.raw.paradise;
        }
    }

    public String getFilenameFromZone(int zone) {
        if (zone == 0) {
            return "greyarea.txt";
        } else if (zone == 1) {
            return "empirestateofmind.txt";
        } else if (zone == 2) {
            return "paradise.txt";
        } else {
            return "paradise.txt";
        }
    }

    public int numberOfLinesCrossed(double[][] polygon, double latitude, double longitude) {
        int intersections = 0;
        for (int cnt = 0; cnt < polygon.length; cnt++) {
            double[] point1 = polygon[cnt];
            double[] point2 = polygon[(cnt + 1) % polygon.length];
            //if the latitudes are the same, aka the slope is horizontal
            if (point1[0] == point2[0]) {
                continue;
            }
            //get slope & y-int of line between point1 and point2
            double m = (point2[0] - point1[0]) / (point2[1] - point1[1]);
            double b = point1[0] - m * point1[1];
            //find the x value of the intersection point between the horizontal line that runs through the user location and the point1/point2 line
            double x = (1 / m) * (latitude - b);
            //if x is in the range both lines
            if ((point1[1] - x < 0 ^ point2[1] - x < 0) && x > longitude) {
                intersections += 1;
            }
        }
        return intersections;
    }

    public TreeMap<Integer, String> getTranscriptFromTextFile(String filename) {
        BufferedReader bufferedReader = null;
        final TreeMap<Integer, String> transcript = new TreeMap<>();
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open(filename)));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitLine = line.split("=");
                transcript.put(Integer.parseInt(splitLine[0]), splitLine[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {

                }
            }
        }
        return transcript;
    }

    public void populateLinearLayoutTranscript() {
        String[] transcriptArray = transcript.values().toArray(new String[transcript.size()]);
        for (int cnt = 0; cnt < transcriptArray.length; cnt++) {
            TextView textView = new TextView(this);
            textView.setText(transcriptArray[cnt]);
            textView.setTextColor(Color.parseColor("#60000000"));
            textView.setTextSize(18);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setPadding(5, 5, 5, 5);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayoutTranscript.addView(textView);
        }
    }

    public void highlightTranscript() {
        final int indexOfChild = getIndexFromAudioProgress();
        if (indexOfChild != 0) {
//            linearLayoutTranscript.getChildAt(indexOfChild - 1).setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            linearLayoutTranscript.getChildAt(indexOfChild - 1).setBackgroundColor(Color.parseColor("#00000000"));
            linearLayoutTranscript.getChildAt(indexOfChild - 1).setPadding(0, 5, 0, 5);
            ((TextView) linearLayoutTranscript.getChildAt(indexOfChild - 1)).setTextColor(Color.parseColor("#60000000"));
            ((TextView) linearLayoutTranscript.getChildAt(indexOfChild - 1)).setTextSize(18);
            ((TextView) linearLayoutTranscript.getChildAt(indexOfChild - 1)).setGravity(Gravity.CENTER_HORIZONTAL);
        }
//        linearLayoutTranscript.getChildAt(indexOfChild).setBackgroundResource(R.drawable.rounded_corner);
        //linearLayoutTranscript.getChildAt(indexOfChild).setBackgroundColor(Color.parseColor("#666666"));
        //linearLayoutTranscript.getChildAt(indexOfChild).setPadding(0, 10, 0, 10);
//        ((TextView) linearLayoutTranscript.getChildAt(indexOfChild)).setTextColor(Color.parseColor("#FF000000"));
        ((TextView) linearLayoutTranscript.getChildAt(indexOfChild)).setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
//        ((TextView) linearLayoutTranscript.getChildAt(indexOfChild)).setTextSize(20);
        ((TextView) linearLayoutTranscript.getChildAt(indexOfChild)).setGravity(Gravity.CENTER);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if(scrollingInt <= 0){
                    scrollViewTranscript.scrollTo(0, linearLayoutTranscript.getChildAt(indexOfChild).getTop() - ((ScrollView) findViewById(R.id.scrollViewTranscript)).getHeight() / 2);
                }
            }
        });
    }

    public int getIndexFromAudioProgress() {
        Integer[] transcriptTimes = transcript.keySet().toArray(new Integer[transcript.keySet().size()]);
        for (int cnt = 0; cnt < transcriptTimes.length; cnt++) {
            if (transcriptTimes[cnt] > musicService.getCurrentPosition()) {
                return cnt - 1;
            }
        }
        return transcriptTimes.length - 1;
    }

    public ServiceConnection getServiceConnection() {
        return (new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Toast.makeText(TourActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
                MusicService.LocalBinder localBinder = (MusicService.LocalBinder) service;
                musicService = localBinder.getServiceInstance();
                serviceBound = true;
                audioHandler.post(audioRunnable);
                Log.e("mylogs", "------Service Connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(TourActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
                musicService = null;
                serviceBound = false;
                Log.e("mylogs", "------Service Disconnected");
            }
        });
    }

    public void setPlayButtonToPlay(){
        if(Build.VERSION.SDK_INT < 21){
            playButton.setBackgroundResource(R.drawable.play_colored);
        } else {
            playButton.setBackgroundResource(R.drawable.play);
        }
    }

    public void setPlayButtonToPause(){
        if(Build.VERSION.SDK_INT < 21){
            playButton.setBackgroundResource(R.drawable.pause_colored);
        } else {
            playButton.setBackgroundResource(R.drawable.pause);
        }
    }
}
