package com.hitake.www.rccontroller;


import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Manages BLE Shield communications
 *
 * Although this class inherits from NetworkManager, it behaves a bit differently since
 * the actual connection is not managed by a PersistentClient class (with a socket) but via
 * a service (RBLService)
 */
public class BLE_NetworkManager extends NetworkManager{

    private final static String TAG = BLE_NetworkManager.class.getSimpleName();

    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    boolean mBleSupported = false;
    Handler mUIHandler = null;

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                sendMessageToUI(true,"Unable to initialize Bluetooth");
                mBleSupported = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public BLE_NetworkManager(NetworkListener aListener) {
        super(aListener);
    }

    protected void create(Activity activity) {
        super.create(activity);
        if (!mActivity.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            sendMessageToUI(true,"Ble not supported");
            return;
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            sendMessageToUI(true,"Ble not supported");
            return;
        }

        // Used to send messages to the UI thread
        mUIHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message inputMessage) {
                StatusMessage msg = (StatusMessage)inputMessage.obj;
                switch (inputMessage.what) {
                    case StatusMessage.MSG_SERVER_ERR:
                        mListener.onNetworkError(msg.mMsg);
                        break;
                    case StatusMessage.MSG_SERVER_MSG:
                        mListener.onProgress(msg.mMsg);
                        break;
                    default:
                        super.handleMessage(inputMessage);
                }
            }
        };

        mBleSupported = true;
        Intent gattServiceIntent = new Intent(mActivity, RBLService.class);
        boolean success = mActivity.bindService(gattServiceIntent, mServiceConnection, mActivity.BIND_AUTO_CREATE);
        if (success)
            sendMessageToUI(false,"BLE service binded");
        else
            sendMessageToUI(true,"Could not bind BLE service");
    }

    // Receives status messages from BLE connection
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connState = false;
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                sendMessageToUI(false,"Connected to car.");
                mFoundCar = true;

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte [] data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
            }
        }
    };

    public void connect(boolean flag) {
        if (scanFlag == false) {
            scanLeDevice();

            Timer mTimer = new Timer();
            mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    if (mDevice != null) {
                        sendMessageToUI(false,"Connecting (#2)...");
                        mDeviceAddress = mDevice.getAddress();
                        mBluetoothLeService.connect(mDeviceAddress);
                        scanFlag = true;
                    } else {
                        mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                sendMessageToUI(true,"Couldn't find the car!");
                            }
                        });
                    }
                }
            }, SCAN_PERIOD);
        }

        System.out.println(connState);
        if (connState == false) {
            sendMessageToUI(false,"Connecting (#1)...");
            mBluetoothLeService.connect(mDeviceAddress);
        } else {
            sendMessageToUI(false,"Disconnected");
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            connState = false;
            mFoundCar = false;
        }
    }

    protected void disconnect() {
        stop();
    }

    protected void stop() {
        flag = false;
        try {
            mActivity.unregisterReceiver(mGattUpdateReceiver);
        }
        catch (IllegalArgumentException e) {}
        sendMessageToUI(false,"Disconnected from car");
    }

    protected void destroy() {
        if (mServiceConnection != null)
            mActivity.unbindService(mServiceConnection);
    }

    protected void resume() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        sendMessageToUI(false,"Receiver registered");
    }

    public void sendCommand(String cmd) {
        byte buf [] = cmd.getBytes();

        characteristicTx.setValue(buf);
        mBluetoothLeService.writeCharacteristic(characteristicTx);
    }

    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        connState = true;
        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 32, j = 0; i >= 17; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = bytesToHex(serviceUuidBytes);
                    if (stringToUuidString(serviceUuid).equals(
                            RBLGattAttributes.BLE_SHIELD_SERVICE
                                    .toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            //finish();
            return;
        }

        //mActivity.onActivityResult(requestCode, resultCode, data);
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

        if (!shouldSendCommandBLE(cmd, value))
            return value;

        sendCommand(cmd + " " +value);
        return value;
    }

    // Sends an RPM command to the car
    protected void sendRPMCommand(int value, boolean reverse) {
        value = getRPMValue(value, reverse);
        String cmd = CarScanner.RC_RPM;

        if (!shouldSendCommandBLE(cmd, value))
            return ;

        sendCommand(cmd + " " +value);
        return;
    }

    @Override
    public void onServerResponse(String cmd, String str, boolean flag) {
    }


    protected boolean shouldSendCommandBLE(String cmd, int value) {
        if (!mFoundCar || mDevice == null)
            return false;

        if (mCommandRotation.equals(cmd,value))
            return false;

        mCommandRotation.mValue = value;
        mCommandRotation.mCommand = cmd;
        return true;
    }

    // Sends a message to the UI thread
    protected void sendMessageToUI(boolean isError, String msg) {
        int status = (isError) ? StatusMessage.MSG_SERVER_ERR : StatusMessage.MSG_SERVER_MSG;
        Message m = mUIHandler.obtainMessage(status, new StatusMessage("",msg));
        m.sendToTarget();
    }


}
