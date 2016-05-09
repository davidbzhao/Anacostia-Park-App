package com.sssnowy.anacostiaparkapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class EventInformationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_information);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        Log.e("mylogs", url);

        AsyncTask<String, String, String> displayEventInformationAsyncTask = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                String url = params[0];
                Document document = null;
                try {
                    document = Jsoup.connect(url).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String title = document.title();
                title = title.replace("•","").substring(0, title.indexOf("|")).trim();
                publishProgress(title);

                for(Element element : document.getElementById("submiddle").getElementsByTag("img")){
                    element.remove();
                }
                return document.getElementById("submiddle").toString();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                getActionBar().setTitle(values[0]);

            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.e("mylogs", s);

                WebView webView = (WebView)findViewById(R.id.eventWebView);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadDataWithBaseURL(null, s, "text/html", "utf-8", null);
            }
        };

        displayEventInformationAsyncTask.execute(url);
//        Document document = null;
//        try {
//            document = Jsoup.connect(url).get();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.e("mylogs", String.valueOf(document == null));
//
//        Log.e("mylogs", document.toString());
//        Log.e("mylogs", String.valueOf(document.getElementById("submiddle") == null));
//        Log.e("mylogs", document.getElementById("submiddle").toString());;
//        String html = Html.fromHtml(document.getElementById("submiddle").toString()).toString();

//        final WebView webView = (WebView)findViewById(R.id.eventWebView);
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.loadUrl(url);
    }

}
