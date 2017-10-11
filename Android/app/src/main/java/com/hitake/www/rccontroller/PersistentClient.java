/*
 * Copyright (c) Oded Cnaan 2016.
 */

package com.hitake.www.rccontroller;

import android.os.Handler;
import android.os.Message;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Base class for persistent client connections
 * Implementation may be for WiFi or BLE
 */
public abstract class PersistentClient implements Runnable {

    final String STOP_THREAD = "STOP"; // Command to stop the thread
    final int QUEUE_SIZE = 200;         // Queue size

    boolean mRun = false;           // is running?

    Handler mHandler = null;        // Used to send messages to the UI thread
                                    // Should be created as
                                    // new Handler(Looper.getMainLooper())
    CarListener mListener = null;   // Listener - informed of server responses
    String mServerAddress = null;   // Address
    int mServerPort = 0;            // Port

    ArrayBlockingQueue<String> mQueue;  // The command queue

    public PersistentClient(Handler handler, CarListener listener, String address, int port) {
        mHandler = handler;
        mListener = listener;
        mServerAddress = address;
        mServerPort = port;
        mQueue = new ArrayBlockingQueue<String>(QUEUE_SIZE, true);
    }

    public void stopClient() {
        mRun = false;
        mQueue.add(STOP_THREAD);
    }

    // Call this method to send a string command to the server
    public void sendCommand(String cmd) {
        try {
            mQueue.put(cmd);
        }
        catch (Exception e) {}
    }

    // Sends a message to the UI thread
    protected void sendMessage(boolean isError, String cmd, String msg) {
        int status = (isError) ? StatusMessage.MSG_SERVER_ERR : StatusMessage.MSG_SERVER_MSG;
        Message m = mHandler.obtainMessage(status, new StatusMessage(cmd,msg));
        m.sendToTarget();
    }

    // To be implemented
    public abstract void run();
}
