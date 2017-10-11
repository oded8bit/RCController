package com.hitake.www.rccontroller;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * Scans for cars by sending a RC_IS_CAR message.
 * This class has 2 modes of operations:
 * 1. Trying to connect to a specific IP address (last IP)
 * 2. Scanning the LAN network
 */
public class CarScanner extends AsyncTask<String, Void, String> {
    int mDstPort = 0;                           // Connection port
    CarListener mListener = null;               // Gets status updates
    boolean isScanLastIp = false;               // Selects between the modes
    Socket mSocket = null;                      // Socket
    private Handler mHandler = null;            // Handler to send messages to UI thread

    // The car commands
    public final static String RC_OK = "OK";
    public final static String RC_IS_CAR = "? 1";
    public final static String RC_LEFT = "L";
    public final static String RC_RIGHT = "R";
    public final static String RC_STRAIGHT = "S";
    public final static String RC_RPM = "P";

    // min/max allowed values for commands
    public final static int RC_MIN_ROT_VALUE = -10;
    public final static int RC_MAX_ROT_VALUE = 10;
    public final static int RC_MIN_RPM_VALUE = 1;
    public final static int RC_MAX_RPM_VALUE = 10;

    private final int SCAN_TIMEOUT = 500;

    CarScanner(CarListener aListener, Handler handler) {
        mListener = aListener;
        mHandler = handler;
        mDstPort = RCPrefs.getInstance().getIntPref("portnum",RCPrefs.DEF_PORT_NUM);
   }

    // Mode 2 - scan network.
    // ipAddress is the base address   "X.X.X."
    protected void connectToCar(String ipAddress) {
        closeSocekt(mSocket);
        mSocket = null;
        isScanLastIp = false;
        execute(new String[] {ipAddress, RC_IS_CAR});
    }

    // Mode 1 - connect to last IP
    // ipAddress is the full IP
    protected void connectToLastCar(String ipAddress) {
        closeSocekt(mSocket);
        mSocket = null;
        isScanLastIp = true;
        String lastIp = ipAddress;
        execute(new String[] {lastIp, RC_IS_CAR});
    }

    // Not in use
    // A generic method for sending commands to the car
    /*protected void sendCommand(String ipAddress, String cmd, String param) {
        execute(new String[] {ipAddress, cmd, param});
    }*/

    @Override
    // The main Task method
    synchronized protected String doInBackground(String... params) {
        if (isScanLastIp)
            return executeLastIp(params);
        else
            return executeCarScan(params);
    }

    // Not in use
    // Send message to the U Ithread
    private void sendMessage(String msg) {
        int status = StatusMessage.MSG_SERVER_MSG;
        Message m = mHandler.obtainMessage(status, new StatusMessage(RC_IS_CAR, msg));
        m.sendToTarget();
    }

    // Mode 1
    private String executeLastIp(String [] params) {
        String response = null;
        String carIP = "";
        Socket socket = null;

        carIP = params[0];
        try {
            socket = new Socket();
            InetSocketAddress socketAddr = new InetSocketAddress(carIP, mDstPort);
            socket.connect(socketAddr, SCAN_TIMEOUT);
            response = doConnection(socket, carIP);
            if (response != null && (response.equals(RC_OK) || response.equals(RC_IS_CAR))) {
                return carIP;
            }
        } catch (UnknownHostException e) {
            //e.printStackTrace();
            carIP = null;
        } catch (IOException e) {
            //e.printStackTrace();
            carIP = null;
        } catch (Exception e) {
            carIP = null;
        } finally {
            closeSocekt(socket);
        }
        return "";
    }

    // Mode 2
    private String executeCarScan(String [] params) {
        String baseAddr = params[0];
        String response = null;
        String carIP = null;
        Socket socket = null;

        for (int i = 1; i <= 255; i++) {
            carIP = baseAddr + String.valueOf(i);
            //sendMessage("Trying to connect to "+carIP);
            try {
                socket = new Socket();
                InetSocketAddress socketAddr = new InetSocketAddress(carIP, mDstPort);
                socket.connect(socketAddr, SCAN_TIMEOUT);
                response = doConnection(socket, carIP);
                if (response != null && (response.equals(RC_OK) || response.equals(RC_IS_CAR))) {
                    break;
                }
            } catch (UnknownHostException e) {
                //e.printStackTrace();
                carIP = null;
            } catch (IOException e) {
                //e.printStackTrace();
                carIP = null;
            } catch (Exception e) {
                carIP = null;
            } finally {
                closeSocekt(socket);
            }
        } // for
        return carIP;
    }

    private String doConnection(Socket socket, String carIP) throws UnknownHostException, IOException, Exception {
        socket.setSoTimeout(SCAN_TIMEOUT);
        InputStream is = socket.getInputStream();
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println(RC_IS_CAR);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        return br.readLine();
    }

    private void closeSocekt(Socket sok) {
        if (sok != null) {
            try {
                sok.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPostExecute(String result) {
        mListener.onServerResponse(RC_IS_CAR, result, isScanLastIp);
        super.onPostExecute(result);
    }

}

