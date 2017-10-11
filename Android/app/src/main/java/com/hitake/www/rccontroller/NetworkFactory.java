/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

/**
 * A Factory class for selecting the NetworkManager (communication method) to be used
 */
public class NetworkFactory {

    private static NetworkFactory ourInstance = new NetworkFactory();
    private boolean mUseBT = false;

    private NetworkFactory() {}

    public static NetworkFactory getInstance() {
        return ourInstance;
    }

    public NetworkManager getNetworkManager(NetworkListener aListener) {
        mUseBT = RCPrefs.getInstance().getBooleanPref("use_bt",true);
        if (mUseBT)
            return new BLE_NetworkManager(aListener);
        else
            return new Wifi_NetworkManager(aListener);
    }
}
