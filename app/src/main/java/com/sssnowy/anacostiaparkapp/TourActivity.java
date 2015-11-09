package com.sssnowy.anacostiaparkapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.TriggerEvent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import java.io.FileDescriptor;
import java.io.IOException;

public class TourActivity extends AppCompatActivity {
    private int currentZone = -1;
    public MediaPlayer mp;
    ImageButton playButton;
    private int cnt = 0;
    double[][][] polygons = {{{38.818441, -77.168650},
           // {38.818050, -77.168066},
            {38.818631, -77.167492},
            {38.818500, -77.167223},
            {38.817069, -77.167970},
            {38.817238, -77.168886},
            {38.817660, -77.169636}}};

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

        mp = MediaPlayer.create(TourActivity.this, R.raw.paradise);
        playButton = (ImageButton)findViewById(R.id.playButton);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mp.isPlaying()){
                    mp.pause();
                    playButton.setBackgroundResource(R.drawable.play);
                } else {
                    mp.start();
                    playButton.setBackgroundResource(R.drawable.pause);
                }
            }
        });

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            /*
            If location changes,
                If enters new zone,
                    If audio is playing,
                        (should not happen) skip this zone
                    If audio is not playing,
                        play new zone audio
                If do not enter new zone,
                    do nothing
            */
            @Override
            public void onLocationChanged(Location location) {
                Button triggerButton = (Button)findViewById(R.id.triggerButton);
                triggerButton.setText(location.getLatitude() + "," + location.getLongitude());
                Toast.makeText(TourActivity.this, "loc changed", Toast.LENGTH_SHORT).show();
                int zone = getZone(location.getLatitude(), location.getLongitude());
                if(currentZone != zone){
                    if(!mp.isPlaying()) {
                        playAudio(getResidFromZone(zone));
                    }
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
    }

    public int getZone(double latitude, double longitude){
        //TJ 38.8184974,-77.168681
        //polygon
        /*
        p1  38.818341, -77.168650
        p2  38.818050, -77.168066
        p3  38.818631, -77.167492
        p4  38.818500, -77.167223
        p5  38.817069, -77.167970
        p6  38.817238, -77.168886
        p7  38.817660, -77.169636

         */

        for(int cnt = 0; cnt < polygons.length; cnt++) {
            int intersections = numberOfLinesCrossed(polygons[cnt], latitude, longitude);
            Toast.makeText(TourActivity.this, intersections + "", Toast.LENGTH_SHORT).show();
            if(intersections % 2 == 1) {
                return cnt;
            }
        }
        return 1;

/*
        if(latitude < 38.8184974 && longitude < -77.168681){
            return 0;
        } else if(latitude > 38.8184974 && longitude < -77.168681){
            return 1;
        } else if(latitude < 38.8184974 && longitude > -77.168681){
            return 2;
        } else if(latitude > 38.8184974 && longitude > -77.168681) {
            return 3;
        } else {
            return -1;
        }*/
    }

    public int getResidFromZone(int zone){
        Button triggerButton = (Button)findViewById(R.id.triggerButton);
        triggerButton.setText(triggerButton.getText() + " " + zone);
        if(zone == 0){
            return R.raw.paradise;
        } else if(zone == 1){
            return R.raw.barnum;
        } else if(zone == 2){
            return R.raw.empire;
        } else {
            return R.raw.empire;
        }
    }

    public void playAudio(int resid){
        mp.stop();
        mp.reset();
        mp = MediaPlayer.create(TourActivity.this, resid);
        mp.start();
    }

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
}
