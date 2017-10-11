/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * This class is not in use!
 */
public class PersistentBTClient extends PersistentClient {

    BluetoothSocket mSocket = null;                  // Socket
    InputStream mInputStream = null;
    PrintWriter mPrintWriter = null;
    BufferedReader mBufferedReader = null;

    // Unique UUID for this application
    private static final UUID RCCAR_UUID =
            UUID.fromString("fa87c1d0-afbc-11de-8a39-0820200c9a66");


    public PersistentBTClient(Handler handler, CarListener listener, String address) {
        super(handler, listener, address,0);
    }

    @Override
    public void run() {
        mRun = true;
        String response = "";
        String cmd = "";
        BluetoothAdapter bTAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            bTAdapter.cancelDiscovery();
            sendMessage(false,CarScanner.RC_IS_CAR,"Connecting to car on "+mServerAddress+"...");
            BluetoothDevice device = bTAdapter.getRemoteDevice(mServerAddress);
            // BluetoothSocket socket
            // =device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            Method m = null;
            try {
                m = device.getClass().getMethod("createRfcommSocket",
                        new Class[] { int.class });
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mSocket = (BluetoothSocket) m.invoke(device, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mSocket.connect();
            mPrintWriter = new PrintWriter(mSocket.getOutputStream(), true);
            mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));
            mInputStream = mSocket.getInputStream();

            while (mRun && mQueue.remainingCapacity() > 0) {
                cmd = mQueue.take();
                if (cmd.equals(STOP_THREAD))
                    break;
                mPrintWriter.write(cmd);
                //response = mBufferedReader.readLine();
                //if (response != null && response != "")
                //    sendMessage(false,cmd,response);
            }
        } catch (Exception e) {
            Log.d("TAG", "Exception during write", e);
        }
        finally {
            closeSocekt(mSocket);
        }
    }

    private void closeSocekt(BluetoothSocket sok) {
        stopClient();
        if (sok != null) {
            try {
                sok.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



}
