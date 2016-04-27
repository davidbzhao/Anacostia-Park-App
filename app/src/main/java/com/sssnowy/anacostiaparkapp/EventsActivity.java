package com.sssnowy.anacostiaparkapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class EventsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        AsyncTask<String, Void, String> httpAsyncTask = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... urls) {
                try {
                    Document document = Jsoup.connect(urls[0]).get();
                    Elements elements = document.getElementsByClass("has-events");
                    for(Element e : elements){
                        Log.e("mylogs", e.id().substring(9));
                        Elements events = e.getElementsByClass("view-field");
                        for(Element event : events){
                            Log.e("mylogs", event.child(0).text().substring(2));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        httpAsyncTask.execute("http://www.anacostiaws.org/calendar/2016-06");
    }

}
