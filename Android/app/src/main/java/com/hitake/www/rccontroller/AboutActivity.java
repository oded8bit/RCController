/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

import java.util.Locale;

/*
 * AboutActivity
 *
 * The About wrapper activity which presents an XML file from project assets
 *
 * Written by: Oded Cnaan
 * Date: July 2016
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_about);

        // Get the locale substring to access the localised assets
        String localPrefix = Locale.getDefault().getLanguage().substring(0, 2).toLowerCase(Locale.US);

        // Load the website as the only action for this activity
        WebView webView = (WebView) findViewById(R.id.webViewAbout);
        webView.loadUrl("file:///android_asset/about.html");

        // Enable the logo in the top left corner to bring the user back to another activity.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
