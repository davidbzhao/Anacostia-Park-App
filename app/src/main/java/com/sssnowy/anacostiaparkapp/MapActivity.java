package com.sssnowy.anacostiaparkapp;

import android.app.LauncherActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

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
        LatLng defaultLatLng = new LatLng(38.817328, -77.169412);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 15f));

//        BitmapDescriptor image = BitmapDescriptorFactory.fromResource(R.drawable.map);
//        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(38.817328, -77.169412), new LatLng(38.818726, -77.167678));
//        GroundOverlay groundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
//                .image(image)
//                .positionFromBounds(latLngBounds)
//                .transparency(0.5f));\

        drawPolygons(getPolygons());



//        final Circle userLocation = mMap.addCircle(new CircleOptions()
//                .center(new LatLng(0, 0))
//                .fillColor(ContextCompat.getColor(MapActivity.this, R.color.colorAccent))
//                .strokeWidth(0.0f)
//                .radius(5.0));

        final Marker userLocation = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("You Are Here!")
                .flat(false));

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLocation.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
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

        if (checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5, 10, locationListener);
        } else {
            Toast.makeText(MapActivity.this, "turn on your GPS", Toast.LENGTH_SHORT).show();
        }
    }

    public double[][][] getPolygons(){
        BufferedReader bufferedReader = null;
        ArrayList<ArrayList<double[]>> temp = new ArrayList<ArrayList<double[]>>();
        for(int cnt = 0; cnt < TourActivity.NUMBER_OF_ZONES; cnt++){
            try {
                temp.add(new ArrayList<double[]>());
                bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open("zone_" + cnt + ".txt")));
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

    public void drawPolygons(double[][][] polygons){
        for(int poly = 0; poly < polygons.length; poly++){
            LatLng[] coords = new LatLng[polygons[poly].length];
            for(int point = 0; point < polygons[poly].length; point++){
                coords[point] = new LatLng(polygons[poly][point][0], polygons[poly][point][1]);
            }
            mMap.addPolygon(new PolygonOptions().add(coords)
                    .strokeColor(ContextCompat.getColor(this, R.color.zoneBorder))
                    .strokeWidth(4f)
                    .fillColor(ContextCompat.getColor(this, R.color.zoneFill)));
        }
    }
}
