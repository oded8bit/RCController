/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by ODEDC on 13-Jul-16.
 */
public class CarBroadcastReceiver extends BroadcastReceiver {
    ArrayList<String> mDevices = new ArrayList<String>();

    public void init() {
        mDevices.clear();
    }

    ArrayList<String> getDeviceList() {
        return mDevices;
    }

    public void onReceive(Context context, Intent intent) {

    }
}
