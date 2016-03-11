package com.sssnowy.anacostiaparkapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private static Marker userMarker;
    private static Circle userCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(38.817328, -77.169412), 15f));

        drawAudioZones(TourActivity.getPolygons(MapActivity.this));
        userMarker = createUserMarker();
        userCircle = createUserCircle();
        setUserMarkerToPreviousLocation();
    }

    /**
     * Update the position of the User Marker.
     * Call from Tour Activity Location Changed Listener.
     */
    public static void updateUserLocation(Location location){
        if(userMarker != null) {
            userMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        if(userCircle != null){
            userCircle.setCenter(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    public static void setUserCircleRadius(float radius) {
        if(userCircle != null) {
            userCircle.setRadius((double) radius);
        }
    }

    /**
     * Draw the audio zones on to the Map in the form of Google Maps Polygons.
     */
    public void drawAudioZones(double[][][] polygons){
        for(int poly = 0; poly < polygons.length; poly++){
            LatLng[] coords = new LatLng[polygons[poly].length];
            for(int point = 0; point < polygons[poly].length; point++){
                coords[point] = new LatLng(polygons[poly][point][0], polygons[poly][point][1]);
            }
            boolean audioZoneVisited = getApplicationContext().getSharedPreferences(TourActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE).getBoolean("resid" + TourActivity.getResidFromZone(poly), false);
            if(audioZoneVisited){
                mMap.addPolygon(new PolygonOptions().add(coords)
                        .strokeColor(ContextCompat.getColor(this, R.color.zoneBorderVisited))
                        .strokeWidth(4f)
                        .fillColor(ContextCompat.getColor(this, R.color.zoneFillVisited)));
            } else {
                mMap.addPolygon(new PolygonOptions().add(coords)
                        .strokeColor(ContextCompat.getColor(this, R.color.zoneBorder))
                        .strokeWidth(4f)
                        .fillColor(ContextCompat.getColor(this, R.color.zoneFill)));
            }
        }
    }

    public Marker createUserMarker(){
        return mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("You Are Here!")
                .flat(false));
    }

    public Circle createUserCircle(){
        return mMap.addCircle(new CircleOptions()
                .center(userMarker.getPosition())
                .strokeColor(ContextCompat.getColor(this, R.color.circleBorder))
                .fillColor(ContextCompat.getColor(this, R.color.circleFill))
                .radius(0));
    }

    public void setUserMarkerToPreviousLocation(){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(TourActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        double lastLatitude = sharedPreferences.getFloat("lastLatitude", 0.0f);
        double lastLongitude = sharedPreferences.getFloat("lastLongitude", 0.0f);
        Log.e("mylogs", lastLatitude + " : " + lastLongitude);
        userMarker.setPosition(new LatLng(lastLatitude, lastLongitude));
    }
}
