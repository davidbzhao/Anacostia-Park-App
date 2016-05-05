package com.sssnowy.anacostiaparkapp;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class EventsActivity extends Activity {
    private LinearLayout eventLinearLayout;
    private AsyncTask<String, Void, JSONArray> curMonthAsyncTask, nextMonthAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        eventLinearLayout = (LinearLayout) findViewById(R.id.eventLinearLayout);

        Log.e("mylogs", getAWSCalendarURL());
        Log.e("mylogs", getNextMonthAWSCalendarURL());
        curMonthAsyncTask = createAsyncTask().execute(getAWSCalendarURL());
        nextMonthAsyncTask = createAsyncTask().execute(getNextMonthAWSCalendarURL());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e("mylogs", "onCreateOptionsMenu");
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_events, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.actionRefreshEvents){
            eventLinearLayout.removeAllViews();
            curMonthAsyncTask.cancel(true);
            nextMonthAsyncTask.cancel(true);
            Log.e("mylogs", "Refreshed Events");
            curMonthAsyncTask = createAsyncTask().execute(getAWSCalendarURL());
            nextMonthAsyncTask = createAsyncTask().execute(getNextMonthAWSCalendarURL());
        }
        return super.onOptionsItemSelected(item);
    }

    private String getAWSCalendarURL(){
        DateFormat newformat = new SimpleDateFormat("yyyy-MM");
        return String.format("http://www.anacostiaws.org/calendar/%s", newformat.format(new Date()));
    }

    private String getNextMonthAWSCalendarURL(){
        DateFormat newformat = new SimpleDateFormat("yyyy-MM");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, 1);
        return String.format("http://www.anacostiaws.org/calendar/%s", newformat.format(cal.getTime()));
    }

    private AsyncTask<String, Void, JSONArray> createAsyncTask(){
        return new AsyncTask<String, Void, JSONArray>() {
            @Override
            protected JSONArray doInBackground(String... urls) {
                JSONArray eventsJSONArray = null;
                try {
                    eventsJSONArray = new JSONArray();
                    Document document = Jsoup.connect(urls[0]).get();
                    Elements elements = document.getElementsByClass("has-events");
                    for (Element hasevents : elements) {
                        String dateString = hasevents.id().substring(9);
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Integer.parseInt(dateString.substring(0, 4)), Integer.parseInt(dateString.substring(5, 7)), Integer.parseInt(dateString.substring(8)));
                        calendar.add(Calendar.MONTH, -1);
                        Date date = calendar.getTime();

                        Elements events = hasevents.getElementsByClass("view-field");
                        for (Element event : events) {
                            String eventString = event.child(0).text();
                            String url = "http://www.anacostiaws.org" + event.child(0).attr("href");
                            int titleBreak = eventString.indexOf(":");
                            if(Character.isDigit(eventString.charAt(titleBreak + 1))){
                                titleBreak = eventString.indexOf(":", titleBreak + 1);
                            }
                            if (titleBreak != -1) {
                                if (eventString.contains("-") && eventString.indexOf("-") < titleBreak) {
                                    titleBreak = eventString.indexOf("-");
                                }
                            } else {
                                titleBreak = eventString.indexOf("-");
                            }

//                            Log.e("mylogs", eventString + " === " + titleBreak + " === " + eventString.indexOf("-"));
                            JSONObject cur = new JSONObject();
                            String title = event.child(0).text();
                            String subtitle = "";
                            if (titleBreak != -1) {
                                title = eventString.substring(0, titleBreak).replace("•", "").trim();
                                subtitle = eventString.substring(titleBreak + 1).trim();
                            } else {
                                title = eventString.replace("•", "").trim();
                            }
                            cur.put("title", title);
                            cur.put("subtitle", subtitle);
                            cur.put("date", date);
                            cur.put("url", url);
                            eventsJSONArray.put(cur);
//                            Log.e("mylogs", event.child(0).text());
                        }
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return eventsJSONArray;
            }

            @Override
            protected void onPostExecute(JSONArray jsonArray) {
                super.onPostExecute(jsonArray);
                try {
                    Log.e("mylogs", jsonArray.toString(2));
                    for (int cnt = 0; cnt < jsonArray.length(); cnt++) {
                        ViewGroup eventRow = (ViewGroup) EventsActivity.this.getLayoutInflater().inflate(R.layout.event_row, eventLinearLayout, false);
                        final JSONObject event = (JSONObject) jsonArray.get(cnt);
                        ((TextView)eventRow.findViewById(R.id.primaryText)).setText(event.get("title").toString());
                        ((TextView)eventRow.findViewById(R.id.secondaryText)).setText(event.get("subtitle").toString());
                        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
                        DateFormat newformat = new SimpleDateFormat("MMM dd");
                        ((TextView) eventRow.findViewById(R.id.tertiaryText)).setText(newformat.format(format.parse(event.get("date").toString())));
                        if(format.parse(event.get("date").toString()).after(new Date())){
                            eventLinearLayout.addView(eventRow);
                            eventRow.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(EventsActivity.this, EventInformationActivity.class);
                                    try {
                                        intent.putExtra("url", event.get("url").toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                    startActivity(intent);
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
