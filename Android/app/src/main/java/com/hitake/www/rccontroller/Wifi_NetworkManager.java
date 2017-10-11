package com.hitake.www.rccontroller;

import android.content.Context;
import android.net.ConnectivityManager;

import android.net.NetworkInfo;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The main manager of all networking
 * Responsible for scanning for cars as well as communicate with the WiFi library and
 * managing all the commands being sent to the server
 */
public class Wifi_NetworkManager extends NetworkManager  {

    private WifiManager mWifiManager = null;
    private ConnectivityManager mConnectManager = null;
    private PersistentTCPClient mTcpClient = null;      // The TCP connection with the server
    private String mCarIp = null;                       // IP of the car, if available
    private Handler mTcpHandler = null;                 // Allowing TCP thread to communicate
    // with the UI thread
    private Handler mScannerHandler = null;             // Allowing scanner thread to communicate
    // with the UI thread

    public Wifi_NetworkManager(NetworkListener aListener) {
        super(aListener);
        // Check WiFi support
        mWifiManager = (WifiManager) aListener.getActivity().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            mListener.onNetworkError("This device does not support WiFi");
            mListener.onDisconnect();
        } else {
            mWifiManager.setWifiEnabled(true);
            mConnectManager = (ConnectivityManager)(mListener.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE));
        }
    }

     protected void disconnect() {
        if (mTcpClient != null)
            mTcpClient.stopClient();
        mTcpClient = null;
        super.disconnect();
    }

    // If a last car IP is saved, we try connecting to this IP first. If not successful, we scan
    // the network for a car using CarScanner
    protected void connect(boolean tryLastIp) {
        mListener.onConnecting();
        mSensitivityDivider = getSensitivityDivider();
        NetworkInfo info = mConnectManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            onNetworkError("Not connected to WiFi network");
            return;
        }
        if (info.getType() != ConnectivityManager.TYPE_WIFI) {
            onNetworkError("Not connected to WiFi network");
            return;
        }
        mWifiManager = (WifiManager) mListener.getActivity().getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        // Check that we are connected to the right SSID
        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
            String ssid = connectionInfo.getSSID();
            ssid = ssid.replace("\"","");
            String def_ssid = RCPrefs.getInstance().getStringPref("netssid","");
            if (!ssid.equalsIgnoreCase(def_ssid)) {
                onNetworkError("Not connected to SSID "+def_ssid);
                return;
            }
        }
        String ipbase = null;
        String myIP = null;
        try {
            byte[] bytes = BigInteger.valueOf(connectionInfo.getIpAddress()).toByteArray();
            reverseArray(bytes);
            InetAddress addr = InetAddress.getByAddress(bytes);
            ipbase = myIP = addr.getHostAddress();
            // ipbase should be in the form of "X.X.X." to allow us to scan the network for cars
            ipbase = removeLastIpAddr(ipbase);
        }
        catch (UnknownHostException e) {
            onNetworkError("Error finding subnet");
            return;
        }
        String lastip = RCPrefs.getInstance().getStringPref("lastip","");
        CarScanner c = createScanner();
        if (tryLastIp && lastip != "") {
            mListener.onProgress("Trying to connect to last car at "+lastip+"...");
            c.connectToLastCar(lastip);
        }
        else {
            mListener.onProgress("Scanning LAN for cars (controller at "+myIP+")");
            c.connectToCar(ipbase);
        }
    }

    // IP addresses are received in a reverse order from INet
    private byte[] reverseArray(byte [] bytes) {
        for (int start=0, end=bytes.length-1; start<=end; start++, end--) {
            byte aux = bytes[start];
            bytes[start] = bytes[end];
            bytes[end] = aux;
        }
        return bytes;
    }

    private String removeLastIpAddr(String addr) {
        return addr.substring(0,addr.lastIndexOf('.')+1);
    }

    @Override
    public void onServerResponse(String cmd, String str, boolean flag) {
        if (cmd == null)
            return;
        if (cmd.equals(CarScanner.RC_IS_CAR)) {
            // This was a car scanning command
            if (str != null && !mFoundCar) {
                if (str != "") {
                    // Found the car. Next time a command is sent, a TCP client will be created
                    mCarIp = str;
                    mFoundCar = true;
                    RCPrefs.getInstance().setStringPref("lastip", mCarIp);
                    mListener.onConnect("Car found at IP " + mCarIp);
                }
                else if (flag && str == "") {
                    // Will cause scanning for cars on the LAN network
                    connect(false);
                }
            }
            else if (str == null)
                onNetworkError("Could not find cars on LAN network");
        }
        // Server reponse RC_OK
        else if (str != null && str.equals(CarScanner.RC_OK)) {

        }
        // Server response as ECHO
        else if (str != null && str.equals(cmd)) {
        }
        else
            onNetworkError("Server response null (cmd: "+cmd+")");
    }

    @Override
    public void onServerError(String cmd, String str) {
        if (cmd.equals(CarScanner.RC_IS_CAR)) {
            onNetworkError("ERROR: Could not find cars on LAN");
        }
        else if (str == null){
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

        createTcpClient();
        mTcpClient.sendCommand(cmd + " " + String.valueOf(Math.abs(value)));
        return value;
    }

    // Sends an RPM command to the car
    protected void sendRPMCommand(int value, boolean reverse) {
        value = getRPMValue(value, reverse);
        String cmd = CarScanner.RC_RPM;

        if (!shouldSendCommand(cmd, value))
            return ;

        createTcpClient();
        mTcpClient.sendCommand(cmd + " " + String.valueOf(value));
    }

    private CarScanner createScanner() {
        mScannerHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message inputMessage) {
                synchronized (mLock) {
                    StatusMessage msg = (StatusMessage) inputMessage.obj;
                    switch (inputMessage.what) {
                        case StatusMessage.MSG_SERVER_ERR:
                            break;
                        case StatusMessage.MSG_SERVER_MSG:
                            onServerResponse(msg.mCommand, msg.mMsg, msg.mFlag);
                            break;
                        default:
                            super.handleMessage(inputMessage);
                    }
                }
            }

        };
        return new CarScanner(this, mScannerHandler);
    }

    private void createTcpClient() {
        if (mTcpClient == null) {
            mTcpHandler = new Handler(Looper.getMainLooper()) {
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
            int port = RCPrefs.getInstance().getIntPref("portnum",RCPrefs.DEF_PORT_NUM);
            mTcpClient = new PersistentTCPClient(mTcpHandler, this, mCarIp, port);
            mPersistentClient = mTcpClient;
            Thread thread = new Thread(mTcpClient);
            thread.start();
        }
    }
}
