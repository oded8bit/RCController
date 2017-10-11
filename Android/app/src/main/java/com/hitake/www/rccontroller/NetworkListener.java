package com.hitake.www.rccontroller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by odedc on 04-Jul-16.
 */
public interface NetworkListener {

    public void onConnect(String msg);
    public void onDisconnect();
    public void onConnecting();
    public void onNetworkError(String err);
    public void onProgress(String str);
    public Activity getActivity();
    public void netStartActivityForResult(Intent intent);
    public void netRegisterReceiver(BroadcastReceiver rec, IntentFilter intent);
    public void netUnregisterReceiver(BroadcastReceiver rec);
}
