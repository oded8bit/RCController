/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

import android.app.Activity;
import android.os.Handler;

/**
 * Created by ODEDC on 13-Jul-16.
 */
public abstract class NetworkManager implements CarListener {
    NetworkListener mListener = null;           // Receives status about network events
    int mSensitivityDivider;                    // Rotation sensitivity
    boolean mFoundCar = false;                  // If Scanner found a car
    Object mLock = new Object();                // Used by scanner
    PersistentClient    mPersistentClient = null;
    Activity mActivity = null;
    // We save the last commands to avoid sending replicated commands to the car
    RCMessage mCommandRotation = new RCMessage();   // Last rotation command sent to car
    RCMessage mCommandRPM = new RCMessage();        // Last RPM command sent to car


    public NetworkManager(NetworkListener aListener) {
        mSensitivityDivider = getSensitivityDivider();
        mListener = aListener;

    }

    protected void create(Activity activity) {
        mActivity = activity;
    }

    protected abstract void connect(boolean flag);

    protected void onNetworkError(String str) {
        mListener.onNetworkError(str);
        disconnect();
    }

    protected void disconnect() {
        mFoundCar = false;
        mListener.onDisconnect();
    }

    protected void resume() {}

    protected void destroy() {}

    protected void pause() {}

    public abstract void onServerResponse(String cmd, String str, boolean flag);

    public void onServerProgress(String msg) {
        mListener.onProgress(msg);
    }

    public void onServerError(String cmd, String str) {
        onNetworkError(str);
    }

    protected abstract void sendRPMCommand(int value, boolean reverse);

    protected abstract int sendRotationCommand(int value);

    protected String getRotationCommand(int value) {
        String cmd = CarScanner.RC_STRAIGHT;
        if (value < -2)
            cmd = CarScanner.RC_LEFT;
        else if (value > 2)
            cmd = CarScanner.RC_RIGHT;
        return cmd;
    }

    protected int getRotationValue(int value) {
        value = Integer.valueOf((int)(value / mSensitivityDivider));

        if (value > CarScanner.RC_MAX_ROT_VALUE)
            value = CarScanner.RC_MAX_ROT_VALUE;
        if (value < CarScanner.RC_MIN_ROT_VALUE)
            value = CarScanner.RC_MIN_ROT_VALUE;

        return value;
    }

    protected int getRPMValue(int value, boolean reverse) {
        if (value > CarScanner.RC_MAX_RPM_VALUE)
            value = CarScanner.RC_MAX_RPM_VALUE;
        else if (value < CarScanner.RC_MIN_RPM_VALUE)
            value = CarScanner.RC_MIN_RPM_VALUE;

        if (reverse)
            value = -value;
        return value;
    }

    protected boolean shouldSendCommand(String cmd, int value) {
        if (!mFoundCar || mPersistentClient == null)
            return false;

        if (mCommandRotation.equals(cmd,value))
            return false;

        mCommandRotation.mValue = value;
        mCommandRotation.mCommand = cmd;
        return true;
    }

    protected int getSensitivityDivider() {
        String sens = RCPrefs.getInstance().getStringPref("sensitivity",RCPrefs.DEF_SENSITIVITY);
        switch (sens) {
            case "high":
                return 4;
            case "medium":
                return 5;
            case "low":
                return 6;
        }
        return 2;
    }
}
