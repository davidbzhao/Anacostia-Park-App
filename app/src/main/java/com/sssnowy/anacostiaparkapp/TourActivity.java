package com.sssnowy.anacostiaparkapp;

import android.app.Activity;
import android.app.ProgressDialog;
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
import java.sql.Connection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class TourActivity extends Activity implements com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final int NUMBER_OF_ZONES = 3;
    public static final String SHARED_PREFERENCES = "audioZonesVisited";
    private static final int TRANSCRIPT_UPDATE_INTERVAL = 388;

    private boolean transcriptTimerTaskScheduled = false;
    private boolean serviceBound = false;
    private boolean seeking = false;
    public static int currentZone;
    private int previousIndexOfChild = 0;
    private int scrollingInt = 0;
    private Integer[] transcriptTimes;

    private Location currentLocation;
    private String lastLocationUpdateTime;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private ImageButton playButton;
    private LinearLayout linearLayoutTranscript;
    private LocationManager locationManager;
    private MusicService musicService;
    private ScrollView scrollViewTranscript;
    private SeekBar audioSeekBar;
    private ServiceConnection serviceConnection;
    private TextView currentProgressTextView;
    private TextView enableGPSTextView;
    private TextView maxProgressTextView;
    private Timer transcriptTimer;
    private TimerTask transcriptTimerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);
        Log.e("mylogs", "BUILD VERSION: " + Build.VERSION.SDK_INT);

        currentZone = -2;

        playButton = (ImageButton) findViewById(R.id.playButton);
        enableGPSTextView = (TextView) findViewById(R.id.enableGPSTextView);
        linearLayoutTranscript = (LinearLayout) findViewById(R.id.linearLayoutTranscript);
        scrollViewTranscript = (ScrollView) findViewById(R.id.scrollViewTranscript);
        audioSeekBar = (SeekBar) findViewById(R.id.audioSeekBar);
        currentProgressTextView = (TextView) findViewById(R.id.currentProgressTextView);
        maxProgressTextView = (TextView) findViewById(R.id.maxProgressTextView);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableGPSTextView.setVisibility(View.INVISIBLE);
        }
        playButton.setBackgroundResource(R.drawable.play_colored);

        if(!isGooglePlayServicesAvailable()){
            Log.e("mylogs","Wait why?");
            finish();
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        setUpTimerTask();
        setUpPlayButtonListener();
        setUpSeekBarListener();
        setUpScrollViewListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Music Service Connection
        Intent msIntent = new Intent(getApplicationContext(), MusicService.class);
        serviceConnection = getServiceConnection();
        bindService(msIntent, serviceConnection, BIND_AUTO_CREATE);
        //connect location services
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //disconnect location services
        googleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(googleApiClient.isConnected()){
            PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient,
                    locationRequest,
                    this
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    protected void onDestroy() {
        if(serviceConnection != null){
            unbindService(serviceConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.e("UserAction","Back Button Pressed");
        Intent intent = new Intent(TourActivity.this, MenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("mylogs", "API Connected");
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                locationRequest,
                this
        );
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("mylogs", "API Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("mylogs", "API Connection Failed");
    }

    public boolean isGooglePlayServicesAvailable(){
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(status == ConnectionResult.SUCCESS){
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        lastLocationUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.e("mylogs", lastLocationUpdateTime + " === Location Changed === " + location.getAccuracy());
        //If location legitimately changes...
        if(location.getAccuracy() < 100){
            SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).edit();
            editor.putFloat("lastLatitude", (float)location.getLatitude()).putFloat("lastLongitude", (float)location.getLongitude()).apply();
            MapActivity.updateUserLocation(location);
            MapActivity.setUserCircleRadius(location.getAccuracy());
            int zone = getZone(location.getLatitude(), location.getLongitude(), getPolygons(TourActivity.this));
            Toast.makeText(TourActivity.this, currentZone + "/" + zone, Toast.LENGTH_SHORT).show();
            //if entered a different zone...
            if(zone != -2 && currentZone != zone){
                //if audio is not playing already...
                if (serviceBound && !musicService.isPlaying()) {
                    Log.e("mylogs","Service Bound and Not Playing Music === " + musicService.getAudioLength());
                    //if audio is not loaded, finished, or not started
                    if(musicService.getCurrentPosition() == 0 || musicService.getAudioLength() == 0) {
                        Log.e("mylogs","Position = 0");
                        currentZone = zone;
                        Toast.makeText(TourActivity.this, String.format("Zone %d", zone), Toast.LENGTH_SHORT).show();
                        updateTour(zone, true);
                    }
                }
            }
            hideProgressBar();
        }
    }

    public void setUpPlayButtonListener(){
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("UserAction", "Play Button Clicked");
                //If audio is not playing,
                if (!musicService.isPlaying()) {
                    //If transcript filled,
                    if (linearLayoutTranscript.getChildCount() > 0) {
                        //play audio
                        musicService.playAudio();
                        scheduleTranscriptTimerTask();
                        playButton.setBackgroundResource(R.drawable.pause_colored);
                        ((TextView) linearLayoutTranscript.getChildAt(linearLayoutTranscript.getChildCount() - 1)).setTextColor(Color.parseColor("#60000000"));
                    }
                    //If audio is playing,
                } else {
                    //pause audio
                    musicService.pauseAudio();
                    playButton.setBackgroundResource(R.drawable.play_colored);
                }
            }
        });
    }

    public void setUpSeekBarListener(){
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
    }

    public void setUpScrollViewListener(){
        scrollViewTranscript.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.e("UserAction", "Scroll View Touched ACTION_UP");
                    scrollingInt -= 1;
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e("UserAction", "Scroll View Touched ACTION_DOWN");
                    scrollingInt += 1;
                }
                return false;
            }
        });
    }

    public void setUpTimerTask(){
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
                                playButton.setBackgroundResource(R.drawable.play_colored);
                            }
                        }
                    }
                });
            }
        };
    }

    public void updateTour(int zone, boolean displayIfAlreadyVisited){
        Log.e("mylogs","Update Tour UI");
        if(displayIfAlreadyVisited) {
            hideProgressBar();
        }
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
        playButton.setBackgroundResource(R.drawable.pause_colored);
    }

    public int getZone(double latitude, double longitude, double[][][] polygons) {
        for (int cnt = 0; cnt < polygons.length; cnt++) {
            int intersections = numberOfLinesCrossed(latitude, longitude, polygons[cnt]);
            if (intersections % 2 == 1) {
                return cnt;
            }
        }
        return -2;
    }

    public static int getResidFromZone(int zone) {
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
                MusicService.LocalBinder localBinder = (MusicService.LocalBinder) service;
                musicService = localBinder.getServiceInstance();
                serviceBound = true;
                updateTour(-1, false);
                Log.e("mylogs", "Service Connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                musicService = null;
                serviceBound = false;
                Log.e("mylogs", "Service Disconnected");
            }
        });
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

    public void showProgressBar(){
        Log.e("mylogs","Show Progress Bar");
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    public void hideProgressBar(){
        Log.e("mylogs","Hide Progress Bar");
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }
}