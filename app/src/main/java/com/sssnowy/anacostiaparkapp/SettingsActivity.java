package com.sssnowy.anacostiaparkapp;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    public static final String SETTINGS_SHARED_PREFERENCES = "com.sssnowy.anacostiaparkapp.SETTINGS_SHARED_PREFERENCES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setResetZonesSettingClickEvent();
        setShowZonesSettingClickEvent();
        setCheckBoxToUser();
    }

    private void setResetZonesSettingClickEvent(){
        RelativeLayout resetZonesSetting = (RelativeLayout)findViewById(R.id.resetZonesSetting);
        resetZonesSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getApplicationContext().getSharedPreferences(TourActivity.AUDIO_ZONES_SHARED_PREFERENCES, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .putBoolean("resid" + R.raw.intro, true)
                        .apply();
            }
        });
    }

    private void setShowZonesSettingClickEvent(){
        RelativeLayout showZonesSetting = (RelativeLayout)findViewById(R.id.showZonesSetting);
        showZonesSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleShowZonesSetting();
            }
        });

        final CheckBox checkBox = (CheckBox)findViewById(R.id.checkBox);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBox.setChecked(!checkBox.isChecked());
                toggleShowZonesSetting();
            }
        });
    }
    private void toggleShowZonesSetting(){
        //Toggles checkbox
        CheckBox checkBox = (CheckBox)findViewById(R.id.checkBox);
        checkBox.setChecked(!checkBox.isChecked());
        //Toggles secondary text and save in shared preferences
        if(checkBox.isChecked()) {
            ((TextView) findViewById(R.id.audioZonesVisibleTextView)).setText(R.string.audio_zones_visible);
            getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean("showZones", true).apply();
            Log.e("mylogs", "showZones true");
        } else {
            ((TextView) findViewById(R.id.audioZonesVisibleTextView)).setText(R.string.audio_zones_not_visible);
            getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean("showZones", false).apply();
            Log.e("mylogs", "showZones false");
        }
    }

    private void setCheckBoxToUser(){
        if(getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, Context.MODE_PRIVATE).getBoolean("showZones", false)){
            toggleShowZonesSetting();
        }
    }
}
