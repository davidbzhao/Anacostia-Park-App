package com.sssnowy.anacostiaparkapp;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class EventInformationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_information);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        Document document = Jsoup.parse(url);
        document.getElementById("submiddle");

        final WebView webView = (WebView)findViewById(R.id.eventWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);
    }

}
