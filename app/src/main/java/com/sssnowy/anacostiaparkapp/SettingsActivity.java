package com.sssnowy.anacostiaparkapp;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ((Button)findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getApplicationContext().getSharedPreferences(TourActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply();
                TourActivity.currentZone = -2;
            }
        });
    }

}
