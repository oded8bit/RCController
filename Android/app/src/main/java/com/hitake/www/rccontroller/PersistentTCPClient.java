package com.hitake.www.rccontroller;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A persistent TCP socket that allows sending commands to the car
 * To start the Runnable, you need to call
 *
 * Thread thread = new Thread(mTcpClient);
 * thread.start();
 *
 * To send a commands, call sendCommand that will put it in a queue. The queue is constantly
 * being checked for commands (FIFO).
 */
class PersistentTCPClient extends PersistentClient {

    Socket mSocket = null;                  // Socket
    InputStream mInputStream = null;        // Streams
    PrintWriter mPrintWriter = null;
    BufferedReader mBufferedReader = null;

    public PersistentTCPClient(Handler handler, CarListener listener, String serverIp, int serverPort) {
        super(handler, listener, serverIp, serverPort);
    }


    @Override
    public void run() {
        mRun = true;
        String response = "";
        String cmd = "";
        try {
            sendMessage(false,CarScanner.RC_IS_CAR,"Connecting to car on "+mServerAddress+"...");
            mSocket = new Socket(mServerAddress, mServerPort);
            mInputStream = mSocket.getInputStream();
            mPrintWriter = new PrintWriter(mSocket.getOutputStream(), true);
            mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));

            while (mRun && mQueue.remainingCapacity() > 0) {
                cmd = mQueue.take();
                if (cmd.equals(STOP_THREAD))
                    break;
                mPrintWriter.println(cmd);
                /* For efficiency, we don't read the response
                 * from the remote server
                 */
                //response = mBufferedReader.readLine();
                //if (response != null && response != "")
                //    sendMessage(false,cmd,response);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
            sendMessage(true,cmd,response);
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
            sendMessage(true,cmd,response);
        } catch (Exception e) {
            response = "IOException: " + e.toString();
            sendMessage(true,cmd,response);
        }
        finally {
            closeSocekt(mSocket);
        }
    }

    private void closeSocekt(Socket sok) {
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
