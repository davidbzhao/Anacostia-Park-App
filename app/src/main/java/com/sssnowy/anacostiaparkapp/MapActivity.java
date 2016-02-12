package com.sssnowy.anacostiaparkapp;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

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

        // Add a marker in Sydney and move the camera
        LatLng tjlatlng = new LatLng(38.817328, -77.169412);
        mMap.addMarker(new MarkerOptions().position(tjlatlng).title("Marker in TJ"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tjlatlng, 15f));

//        BitmapDescriptor image = BitmapDescriptorFactory.fromResource(R.drawable.map);
//        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(38.817328, -77.169412), new LatLng(38.818726, -77.167678));
//        GroundOverlay groundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
//                .image(image)
//                .positionFromBounds(latLngBounds)
//                .transparency(0.5f));

        mMap.addPolygon(new PolygonOptions().add(
                new LatLng(38.819745, -77.170083),
                new LatLng(38.819711, -77.168661),
                new LatLng(38.819189, -77.166832),
                new LatLng(38.816568, -77.168307),
                new LatLng(38.817754, -77.170129))
                .strokeColor(ContextCompat.getColor(this, R.color.zoneBorder))
                .strokeWidth(4f)
                .fillColor(ContextCompat.getColor(this, R.color.zoneFill)));

        mMap.addPolygon(new PolygonOptions().add(
                new LatLng(38.820163, -77.169949),
                new LatLng(38.819164, -77.166381),
                new LatLng(38.822182, -77.165636),
                new LatLng(38.822169, -77.169965))
                .strokeColor(ContextCompat.getColor(this, R.color.zoneBorder))
                .strokeWidth(4f)
                .fillColor(ContextCompat.getColor(this, R.color.zoneFill)));

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mMap.addCircle(new CircleOptions()
                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                    .fillColor(ContextCompat.getColor(MapActivity.this, R.color.colorAccent))
                    .radius(5.0));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }
}
