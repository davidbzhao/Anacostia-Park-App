package com.sssnowy.anacostiaparkapp;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by 2016dzhao on 3/30/2016.
 */
public class LocationService extends Service implements com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private IBinder binder = new LocalBinder();
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location userLocation;
    private boolean locationUpdatesRequested = false;
    private LocationManager locationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        googleApiClient.connect();
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!isGooglePlayServicesAvailable()) {
            Log.e("mylogs", "Wait why?");
        }

        userLocation = new Location("temp");
        userLocation.setLatitude(0);
        userLocation.setLongitude(0);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setLocationProviderStatusListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("mylogs", "API Connected");
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("mylogs", "API Connection Suspended");
        removeLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("mylogs", "API Connection Failed");
    }

    public boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        String lastLocationUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.e("mylogs", lastLocationUpdateTime + " === Location Changed === " + location.getAccuracy());
        //If location legitimately changes...
        if (location.getAccuracy() < 100) {
            getApplicationContext().getSharedPreferences(TourActivity.AUDIO_ZONES_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit()
                    .putFloat("lastLatitude", (float) location.getLatitude())
                    .putFloat("lastLongitude", (float) location.getLongitude());

            userLocation = location;
            sendLocationBroadcast(0);
        }
    }

    public void googleApiClientConnect() {
        googleApiClient.connect();
    }

    public void googleApiClientDisconnect() {
        googleApiClient.disconnect();
    }

    public void requestLocationUpdates() {
        if (googleApiClient.isConnected()) {
            PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient,
                    locationRequest,
                    this
            );
            locationUpdatesRequested = true;
        }
    }

    public void removeLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        locationUpdatesRequested = false;
    }

    public boolean isLocationUpdatesRequested() {
        return locationUpdatesRequested;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setLocationProviderStatusListener() {
        if (getPackageManager().checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, "com.sssnowy.anacostiaparkapp") != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
//        locationManager.addGpsStatusListener(new GpsStatus.Listener() {
//            @Override
//            public void onGpsStatusChanged(int event) {
//                switch (event) {
//                    case GpsStatus.GPS_EVENT_STARTED:
//                        sendLocationBroadcast(1);
//                        break;
//                    case GpsStatus.GPS_EVENT_STOPPED:
//                        sendLocationBroadcast(2);
//                        break;
//                    default:
//                        break;
//                }
//            }
//        });

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10000, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("mylogs","Default Location Listener Called");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                sendLocationBroadcast(1);
                Log.e("mylogs","Provider Enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                sendLocationBroadcast(2);
                Log.e("mylogs","Provider Disabled");
            }
        });
    }

    public void sendLocationBroadcast(int updateCode){
        Intent locIntent = new Intent(TourActivity.RECEIVE_LOCATION_UPDATE);
        locIntent.putExtra("updateCode", updateCode);
        locIntent.putExtra("latitude", userLocation.getLatitude());
        locIntent.putExtra("longitude", userLocation.getLongitude());
        LocalBroadcastManager.getInstance(this).sendBroadcast(locIntent);
    }

    public boolean isGPSOn(){
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public class LocalBinder extends Binder {
        public LocationService getServiceInstance(){
            return LocationService.this;
        }
    }
}
