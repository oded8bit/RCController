/*
 * Copyright (c) Oded Cnaan 2016.
 */
package com.hitake.www.rccontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/*
 * CarControllerActivity
 *
 * The main Activity for the controller.
 *
 * Written by: Oded Cnaan
 * Date: July 2016
 */
public class CarControllerActivity extends AppCompatActivity
        implements SensorListener, NetworkListener {

    private final String TAG = "CarControllerActivity";
    private TextView mConsole;                      // Status bar
    private RCSensor mRCSensor;                     // Rotation sensoe
    private NetworkManager mNetwork;                // Network manager
    private CustomGauge mGaugeRotation, mGaugeRPM;  // The gauges
    private SlideBar mRpmSlidebar;                  // RPM control
    private boolean mReverseDirection = false;      // Going backwards?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_controller);

        // This is the main Preferences class (singletone)
        RCPrefs.getInstance().init(getApplicationContext());

        mConsole = findViewById(R.id.consoleText);
        mNetwork = NetworkFactory.getInstance().getNetworkManager(this);
        mNetwork.create(this);
        mRCSensor = new RCSensor(this);
        mRCSensor.onResume();               // Start motion sensor

        // Listen to button toggling - connect / disconnect from car
        Switch connectSwitch = findViewById(R.id.connectSwitch);
        connectSwitch.setChecked(false);
        connectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mNetwork.connect(true);
                }
                else {
                    mNetwork.disconnect();
                }
            }
        });
        // Init gauges
        mGaugeRotation = (CustomGauge) findViewById(R.id.rotGauge);
        mGaugeRPM = (CustomGauge) findViewById(R.id.speedGauge);

        // Init RPM control which sends RPM commands to the Network Manager and updates
        // the RPM gauge
        mRpmSlidebar = (SlideBar) findViewById(R.id.verticalSeekbar);
        mRpmSlidebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mGaugeRPM.setValue(progress);
                mNetwork.sendRPMCommand(progress, mReverseDirection);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Init reverse button. Notify Network Manager if status has changed
        Button reverseButton = (Button) findViewById(R.id.reverseButton);
        reverseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!mReverseDirection) {
                        mNetwork.sendRPMCommand(mRpmSlidebar.getProgress(), !mReverseDirection);
                    }
                    mReverseDirection = true;
                }
                else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (mReverseDirection) {
                        mNetwork.sendRPMCommand(mRpmSlidebar.getProgress(), !mReverseDirection);
                    }mReverseDirection = false;
                }
                return false;
            }
        });
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

    }

    // Activity menus
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        return true;
    }

    // Activity menus
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Write message to status bar
    private void writeConsole(String text) {
        mConsole.setText(text+"\n");
    }

    //when this Activity starts
    @Override
    protected void onResume()
    {
        super.onResume();
        if (mRCSensor != null)
            mRCSensor.onResume();
        mNetwork.resume();
    }

    //When this Activity isn't visible anymore
    @Override
    protected void onStop()
    {
        mNetwork.disconnect();
        Switch sw = (Switch) findViewById(R.id.connectSwitch);
        sw.setChecked(false);
        mRCSensor.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRCSensor != null)
            mRCSensor.onPause();
        //mNetwork.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mNetwork.destroy();
    }

    ////////////////   SensorListener Interface //////////////////

    public Object getSensorService() {
        return getSystemService(SENSOR_SERVICE);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.close_text)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
        return true;
    }

    public void updateRotation(int aRotation) {
        int value = mNetwork.sendRotationCommand(aRotation);
        mGaugeRotation.setValue(-value);
    }

    ////////////////   NetworkListener Interface //////////////////
    public void onConnect(String msg) {
        writeConsole(msg);
    }

    public void onConnecting() {
        writeConsole("Connecting to WiFi network...");
    }

    public void onDisconnect() {
        Switch connectSwitch = (Switch) findViewById(R.id.connectSwitch);
        connectSwitch.setChecked(false);
        //writeConsole("Disconnected");
    }

    public Activity getActivity() {
        return (Activity)this;
    }


    public void onNetworkError(String err) {
        writeConsole("Error: "+err);
    }

    public void onProgress(String str) {
        writeConsole(str);
    }

    public void netStartActivityForResult(Intent intent) {
        startActivityForResult(intent, 0);
    }

    public void netRegisterReceiver(BroadcastReceiver rec, IntentFilter intent) {
        registerReceiver(rec, intent);
    }

    public void netUnregisterReceiver(BroadcastReceiver rec) {
        unregisterReceiver(rec);
    }
 }
