package com.hitake.www.rccontroller;

/**
 * Created by odedc on 07-Jul-16.
 */
public class StatusMessage {

    final static int MSG_SERVER_MSG = 19923;
    final static int MSG_SERVER_ERR = 19924;

    public StatusMessage(String cmd, String msg, boolean flag) {
        this(cmd,msg);
        mFlag = flag;
    }

    public StatusMessage(String cmd, String msg) {
        mCommand = cmd;
        mMsg = msg;
    }

    public String mCommand = "";
    public String mMsg = "";
    public boolean mFlag = false;
}
