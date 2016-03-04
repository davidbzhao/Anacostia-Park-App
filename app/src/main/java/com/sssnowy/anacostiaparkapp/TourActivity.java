package com.sssnowy.anacostiaparkapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class TourActivity extends Activity {
    public static final int NUMBER_OF_ZONES = 3;
    public static final String SHARED_PREFERENCES = "audioZonesVisited";
    private static final int TRANSCRIPT_UPDATE_INTERVAL = 388;
    private boolean transcriptTimerTaskScheduled = false;
    private boolean serviceBound = false;
    private boolean seeking = false;
    private TextView enableGPSTextView;
    private ImageButton playButton;
    private int currentZone = -2;
    private LinearLayout linearLayoutTranscript;
    private LocationManager locationManager;
    private MusicService musicService;
    private ServiceConnection serviceConnection;
    private ScrollView scrollViewTranscript;
    private int scrollingInt = 0;
    private int previousIndexOfChild = 0;
    private SeekBar audioSeekBar;
    private TextView currentProgressTextView;
    private TextView maxProgressTextView;
    private Timer transcriptTimer;
    private TimerTask transcriptTimerTask;
    private Integer[] transcriptTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);
        Log.e("mylogs", "BUILD VERSION: " + Build.VERSION.SDK_INT);

        Log.e("mylogs", "-onCreate");
        //initialize
        playButton = (ImageButton) findViewById(R.id.playButton);
        enableGPSTextView = (TextView) findViewById(R.id.enableGPSTextView);
        linearLayoutTranscript = (LinearLayout) findViewById(R.id.linearLayoutTranscript);
        scrollViewTranscript = (ScrollView) findViewById(R.id.scrollViewTranscript);
        audioSeekBar = (SeekBar) findViewById(R.id.audioSeekBar);
        currentProgressTextView = (TextView) findViewById(R.id.currentProgressTextView);
        maxProgressTextView = (TextView) findViewById(R.id.maxProgressTextView);


        setPlayButtonToPlay();
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableGPSTextView.setVisibility(View.INVISIBLE);
        }
        //--------------------------------------------------------------------------------------------------------Listeners

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //If location legitimately changes...
                if(location.getAccuracy() < 100){
                    int zone = getZone(location.getLatitude(), location.getLongitude(), getPolygons(TourActivity.this));
                    //if entered a different zone...
                    if(currentZone != zone && zone != -2){
                        //if audio is not playing already...
                        if(serviceBound && !musicService.isPlaying()){
                            updateTour(zone, true);
                        }
                        currentZone = zone;
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                enableGPSTextView.setVisibility(View.INVISIBLE);
                updateTour(-1, false);
                Toast.makeText(TourActivity.this, "on", Toast.LENGTH_SHORT).show();
                Log.e("mylogs","GPS Turned ON");
            }

            @Override
            public void onProviderDisabled(String provider) {
                enableGPSTextView.setVisibility(View.VISIBLE);
                Toast.makeText(TourActivity.this, "off", Toast.LENGTH_SHORT).show();
                Log.e("mylogs", "GPS Turned OFF");
            }
    };

        if (checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5, 10, locationListener);
        } else {
            Toast.makeText(TourActivity.this, "turn on your GPS", Toast.LENGTH_SHORT).show();
        }


        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("UserAction","Play Button Clicked");
                //If audio is not playing,
                if (!musicService.isPlaying()) {
                    //If transcript filled,
                    if (linearLayoutTranscript.getChildCount() > 0) {
                        //play audio
                        musicService.playAudio();
                        scheduleTranscriptTimerTask();
                        setPlayButtonToPause();
                        ((TextView)linearLayoutTranscript.getChildAt(linearLayoutTranscript.getChildCount() - 1)).setTextColor(Color.parseColor("#60000000"));
                    }
                    //If audio is playing,
                } else {
                    //pause audio
                    musicService.pauseAudio();
                    setPlayButtonToPlay();
                }
            }
        });

        scrollViewTranscript.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.e("UserAction", "Scroll View Touched ACTION_UP");
                    Log.e("mylogs", "ACTION_UP");
                    scrollingInt -= 1;
                    if (scrollingInt < 0) {
                        Log.e("mylogs", "WOAHWOAHWOAH!!!! That ain't right");
                    }
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e("UserAction", "Scroll View Touched ACTION_DOWN");
                    Log.e("mylogs", "ACTION_DOWN");
                    scrollingInt += 1;
                }
                return false;
            }
        });

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicService.seekTo(progress);
                    Log.e("UserAction", "Seek Bar Touched");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seeking = true;
                musicService.pauseAudio();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seeking = false;
                unhighlightTranscript();
                highlightTranscript();
                musicService.playAudio();
            }
        });


        transcriptTimer = new Timer();
        transcriptTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
                        if(!seeking) {
                            if (musicService.isPlaying()) {
                                if (linearLayoutTranscript.getChildCount() > 0) {
                                    highlightTranscript();
                                    updateSeekBar();
                                }
                            } else {
                                setPlayButtonToPlay();
                            }
                        }
                    }
                });
            }
        };
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
    protected void onDestroy() {
        Log.e("mylogs", "onDestroy");
        if(serviceConnection != null){
            unbindService(serviceConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.e("mylogs","BACK BUTTON PRESSED");
        Intent intent = new Intent(TourActivity.this, MenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    public void updateTour(int zone, boolean displayIfAlreadyVisited){
        if(!audioZoneVisited(zone)){
            setUpDisplayAndAudio(zone);
            playCurrentAudio();
        } else {
            if(displayIfAlreadyVisited) {
                setUpDisplayAndAudio(zone);
            }
        }
    }

    public void setUpDisplayAndAudio(int zone){
        populateLinearLayoutTranscript(getTranscriptFromTextFile(getFilenameFromZone(zone)));
        musicService.setAudio(getApplicationContext(), getResidFromZone(zone));
        configureSeekBar();
        previousIndexOfChild = 0;
    }

    public void playCurrentAudio(){
        musicService.playAudio();
        scheduleTranscriptTimerTask();
        setPlayButtonToPause();
    }

    public int getZone(double latitude, double longitude, double[][][] polygons) {
        for (int cnt = 0; cnt < polygons.length; cnt++) {
            int intersections = numberOfLinesCrossed(latitude, longitude, polygons[cnt]);
            Toast.makeText(TourActivity.this, intersections + "", Toast.LENGTH_SHORT).show();
            if (intersections % 2 == 1) {
                return cnt;
            }
        }
        return -2;
    }

    public int getResidFromZone(int zone) {
        switch(zone){
            case -1:
                return R.raw.intro;
            case 0:
                return R.raw.greyarea;
            case 1:
                return R.raw.empirestateofmind;
            case 2:
                return R.raw.paradise;
            default:
                return R.raw.paradise;
        }
    }

    public String getFilenameFromZone(int zone) {
        if(-2 < zone && zone < NUMBER_OF_ZONES){
            return String.format("transcript_%d.txt", zone);
        }
        return "transcript_2.txt";
    }

    public int numberOfLinesCrossed(double latitude, double longitude, double[][] polygon) {
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
                } catch (IOException ignored) {
                    Log.e("mylogs","Buffered Reader Broken");
                }
            }
        }
        return transcript;
    }

    public void populateLinearLayoutTranscript(TreeMap<Integer, String> transcript) {
        linearLayoutTranscript.removeAllViews();
        String[] transcriptArray = transcript.values().toArray(new String[transcript.size()]);
        for (String line : transcriptArray) {
            TextView textView = new TextView(this);
            textView.setText(line);
            textView.setTextColor(Color.parseColor("#60000000"));
            textView.setTextSize(18);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setPadding(5, 5, 5, 5);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayoutTranscript.addView(textView);
        }
        updateTranscriptTimes(transcript);
    }

    public void updateTranscriptTimes(TreeMap<Integer, String> transcript){
        transcriptTimes = transcript.keySet().toArray(new Integer[transcript.keySet().size()]);
    }

    public void highlightTranscript() {
        final int indexOfChild = getIndexFromAudioProgress(previousIndexOfChild);
        if(indexOfChild != previousIndexOfChild){
            if (indexOfChild != 0) {
                linearLayoutTranscript.getChildAt(indexOfChild - 1).setBackgroundColor(Color.parseColor("#00000000"));
                linearLayoutTranscript.getChildAt(indexOfChild - 1).setPadding(0, 5, 0, 5);
                ((TextView) linearLayoutTranscript.getChildAt(indexOfChild - 1)).setTextColor(Color.parseColor("#60000000"));
                ((TextView) linearLayoutTranscript.getChildAt(indexOfChild - 1)).setTextSize(18);
                ((TextView) linearLayoutTranscript.getChildAt(indexOfChild - 1)).setGravity(Gravity.CENTER_HORIZONTAL);
            }
            ((TextView) linearLayoutTranscript.getChildAt(indexOfChild)).setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            ((TextView) linearLayoutTranscript.getChildAt(indexOfChild)).setGravity(Gravity.CENTER);
            if(scrollingInt <= 0){
                scrollViewTranscript.scrollTo(0, linearLayoutTranscript.getChildAt(indexOfChild).getTop() - findViewById(R.id.scrollViewTranscript).getHeight() / 2);
            }
        }
        previousIndexOfChild = indexOfChild;
    }

    public void unhighlightTranscript(){
        for(int cnt = 0; cnt < linearLayoutTranscript.getChildCount(); cnt++){
            linearLayoutTranscript.getChildAt(cnt).setBackgroundColor(Color.parseColor("#00000000"));
            linearLayoutTranscript.getChildAt(cnt).setPadding(0, 5, 0, 5);
            ((TextView) linearLayoutTranscript.getChildAt(cnt)).setTextColor(Color.parseColor("#60000000"));
            ((TextView) linearLayoutTranscript.getChildAt(cnt)).setTextSize(18);
            ((TextView) linearLayoutTranscript.getChildAt(cnt)).setGravity(Gravity.CENTER_HORIZONTAL);
        }
    }

    //chances are the index is either the previous index or the one after the previous index, test those first, if not, binary search
    public int getIndexFromAudioProgress(int previousIndexOfChild) {
        if(musicService.getCurrentPosition() > transcriptTimes[previousIndexOfChild]){
            for (int cnt = previousIndexOfChild + 1; cnt < transcriptTimes.length; cnt++) {
                if (transcriptTimes[cnt] > musicService.getCurrentPosition()) {
                    return cnt - 1;
                }
            }
        } else {
            for (int cnt = 0; cnt < previousIndexOfChild; cnt++) {
                if (transcriptTimes[cnt] > musicService.getCurrentPosition()) {
                    return cnt - 1;
                }
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
                updateTour(-1, false);
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

    public static double[][][] getPolygons(Context c){
        BufferedReader bufferedReader = null;
        ArrayList<ArrayList<double[]>> temp = new ArrayList<>();
        for(int cnt = 0; cnt < NUMBER_OF_ZONES; cnt++){
            try {
                temp.add(new ArrayList<double[]>());
                bufferedReader = new BufferedReader(new InputStreamReader(c.getAssets().open(String.format("zone_%d.txt", cnt))));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] splitLine = line.split(",");
                    double[] coord = {Double.parseDouble(splitLine[0]), Double.parseDouble(splitLine[1])};
                    temp.get(cnt).add(coord);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        Log.e("mylogs","Buffered Reader Broken");
                    }
                }
            }
        }

        double[][][] finalPolygons = new double[temp.size()][][];
        for(int cnt = 0; cnt < temp.size(); cnt++){
            finalPolygons[cnt] = new double[temp.get(cnt).size()][2];
            for(int z = 0; z < finalPolygons[cnt].length; z++){
                finalPolygons[cnt][z][0] = temp.get(cnt).get(z)[0];
                finalPolygons[cnt][z][1] = temp.get(cnt).get(z)[1];
            }
        }

        return finalPolygons;
    }

    public void scheduleTranscriptTimerTask(){
        if(!transcriptTimerTaskScheduled) {
            transcriptTimer.scheduleAtFixedRate(transcriptTimerTask, 0, TRANSCRIPT_UPDATE_INTERVAL);
        }
        transcriptTimerTaskScheduled = true;
    }

    public void configureSeekBar(){
        maxProgressTextView.setText(formatMMSSFromMilliseconds(musicService.getAudioLength()));
        audioSeekBar.setProgress(0);
        audioSeekBar.setMax(musicService.getAudioLength());
        currentProgressTextView.setText(R.string.defaultProgress);
    }

    public void updateSeekBar(){
        currentProgressTextView.setText(formatMMSSFromMilliseconds(musicService.getCurrentPosition()));
        audioSeekBar.setProgress(musicService.getCurrentPosition());
    }

    public String formatMMSSFromMilliseconds(int milli){
        return (Math.round(milli * 0.001) / 60) + ":" + String.format("%02d",(Math.round(milli * 0.001) % 60));
    }

    public boolean audioZoneVisited(int zone){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("resid" + getResidFromZone(zone), false);
    }
}