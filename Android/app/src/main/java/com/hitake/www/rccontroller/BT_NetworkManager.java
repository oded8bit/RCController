/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Set;

/**
 * Network Manager for BLE connections.
 *
 * This class is not in use!
 */
public class BT_NetworkManager extends NetworkManager  {

    private final String BT_CAR_NAME = "RCCar_Oded";

    private BluetoothAdapter        mBTAdapter;
    private Set<BluetoothDevice>    mPairedDevices;
    private PersistentBTClient      mBTClient;          // The client
    private String mCarAddress = null;                       // IP of the car, if available
    private Handler mBtHandler = null;                  // Allowing TCP thread to communicate
                                                        // with UI thread

    public BT_NetworkManager(NetworkListener aListener) {
        super(aListener);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter == null) {
            mListener.onNetworkError("BT not supported");
        }
        else {
            if (!mBTAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mListener.netStartActivityForResult(enableBtIntent);
            }
        }
    }

    protected void connect(boolean flag) {
        String address = getBondedDeviceList(BT_CAR_NAME);
        if (address == null) {
            onNetworkError("The Car is not paired with this phone.");
            return;
        }
        mFoundCar = true;
        mCarAddress = address;
        createBtClient();
    }

    protected void disconnect() {
        mFoundCar = false;
        mBTAdapter.disable();
        mPairedDevices = null;
        mCarAddress = null;
        super.disconnect();
    }

    /*
    final CarBroadcastReceiver mBTReceiver = new CarBroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name and the MAC address of the object to the arrayAdapter
                mDevices.add(device.getName() + "\n" + device.getAddress());
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mListener.netUnregisterReceiver(mBTReceiver);
                findCarCompleted(mDevices);
            }
        }
    };

    protected void findCarCompleted(ArrayList<String> devices) {
        mListener.onProgress("Scan completed.");
    }

    private void findCar() {
        if (mBTAdapter.isDiscovering())
            mBTAdapter.cancelDiscovery();
        else {
            mListener.onProgress("Scanning for cars...");
            mBTAdapter.startDiscovery();
            mBTReceiver.init();
            mListener.netRegisterReceiver(mBTReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    private ArrayList<String> getBondedDeviceList() {
        mPairedDevices = mBTAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList<String>();

        for(BluetoothDevice bt : mPairedDevices)
            list.add(bt.getName());
        return list;
    }
    */

    private String getBondedDeviceList(String name) {
        mPairedDevices = mBTAdapter.getBondedDevices();
        for(BluetoothDevice bt : mPairedDevices) {
            if (bt.getName().equalsIgnoreCase(name))
                return bt.getAddress();
        }
        return null;
    }

    public void onServerResponse(String cmd, String str, boolean flag) {
    }

    @Override
    public void onServerError(String cmd, String str) {
        if (str == null){
            onNetworkError("Server response null (cmd: "+cmd+")");
        }
        else
            onNetworkError(str);
    }

    // Sends a Rotation command to the car
    protected int sendRotationCommand(int value) {
        value = getRotationValue(value);
        String cmd = getRotationCommand(value);

        if (!shouldSendCommand(cmd, value))
            return value;

        mBTClient.sendCommand(cmd+" "+String.valueOf(value));
        return value;
    }

    // Sends an RPM command to the car
    protected void sendRPMCommand(int value, boolean reverse) {
        value = getRPMValue(value, reverse);
        String cmd = CarScanner.RC_RPM;

        if (!shouldSendCommand(cmd, value))
            return ;

        mBTClient.sendCommand(cmd+" "+String.valueOf(value));
        return;
    }

    private void createBtClient() {
        if (mBTClient == null) {
            mBtHandler = new Handler(Looper.getMainLooper()) {
                public void handleMessage(Message inputMessage) {
                    StatusMessage msg = (StatusMessage)inputMessage.obj;
                    switch (inputMessage.what) {
                        case StatusMessage.MSG_SERVER_ERR:
                            onServerError(msg.mCommand, msg.mMsg);
                            break;
                        case StatusMessage.MSG_SERVER_MSG:
                            onServerResponse(msg.mCommand, msg.mMsg, msg.mFlag) ;
                            break;
                        default:
                            super.handleMessage(inputMessage);
                    }
                }
            };
            mBTClient = new PersistentBTClient(mBtHandler, this, mCarAddress);
            mPersistentClient = mBTClient;
            Thread thread = new Thread(mBTClient);
            thread.start();
        }
    }

}


