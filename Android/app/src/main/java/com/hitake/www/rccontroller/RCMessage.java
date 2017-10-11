package com.hitake.www.rccontroller;

/**
 * Created by odedc on 07-Jul-16.
 */
public class RCMessage {

    public String mCommand;
    public int mValue;

    public RCMessage() {
        mCommand = "";
        mValue = 0;
    }

    public boolean equals(String cmd, int value) {
        return (cmd.equals(mCommand) && value == mValue);
    }
}
