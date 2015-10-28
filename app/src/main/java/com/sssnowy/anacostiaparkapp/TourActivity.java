package com.sssnowy.anacostiaparkapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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
import android.widget.Toast;
import java.io.FileDescriptor;
import java.io.IOException;

public class TourActivity extends AppCompatActivity {
    private int currentZone = -1;
    public MediaPlayer mp;
    private int cnt = 0;

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
/*
        Button trigger = (Button)findViewById(R.id.triggerButton);
        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cnt == 0){
                    playAudio(R.raw.barnum);
                    Toast.makeText(TourActivity.this, "Barnum", Toast.LENGTH_SHORT).show();
                } else if(cnt == 1){
                    playAudio(R.raw.empire);
                    Toast.makeText(TourActivity.this, "Empire", Toast.LENGTH_SHORT).show();
                } else {
                    playAudio(R.raw.paradise);
                    Toast.makeText(TourActivity.this, "Paradise", Toast.LENGTH_SHORT).show();
                }
                cnt = (cnt + 1) % 3;
            }
        });*/


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
                Toast.makeText(TourActivity.this, "loc changed", Toast.LENGTH_SHORT).show();
                int zone = getZone(location.getLatitude(), location.getLongitude());
                if(currentZone != zone){
                    if(!mp.isPlaying()) {
                        playAudio(getResidFromZone(zone));
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
        if(checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(TourActivity.this, "turn on your GPS", Toast.LENGTH_SHORT).show();
        }
    }

    public int getZone(double latitude, double longitude){
        //TJ 38.8184974,-77.168681
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
        }
    }

    public int getResidFromZone(int zone){
        Button triggerButton = (Button)findViewById(R.id.triggerButton);
        triggerButton.setText(zone + "");
        if(zone == 0){
            return R.raw.paradise;
        } else if(zone == 1){
            return R.raw.empire;
        } else if(zone == 2){
            return R.raw.barnum;
        } else {
            return R.raw.barnum;
        }
    }

    public void playAudio(int resid){
        mp.stop();
        mp.reset();
        mp = MediaPlayer.create(TourActivity.this, resid);
        mp.start();
    }

    /*int zone = getZone(location.getLatitude(), location.getLongitude());
                if(currentZone == zone){
                    zone = 1;
                }
                Toast.makeText(TourActivity.this, zone + "", Toast.LENGTH_SHORT).show();
                if (currentZone != zone) {
                    currentZone = zone;
                    mp.stop();
                    mp.release();
                    Toast.makeText(TourActivity.this, "playAudio", Toast.LENGTH_SHORT).show();
                    if(zone == 0){
                        try {
                            mp.setDataSource(TourActivity.this, Uri.parse("android.resource//com.sssnowy.anacostiaparkapp/raw/paradise"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            mp.setDataSource(TourActivity.this, Uri.parse("android.resource//com.sssnowy.anacostiaparkapp/raw/barnum"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mp.start();
                }*/
}
