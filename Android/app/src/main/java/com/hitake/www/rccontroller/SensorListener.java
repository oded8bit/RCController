package com.hitake.www.rccontroller;

import android.content.Context;

/**
 * Created by odedc on 04-Jul-16.
 */
public interface SensorListener {
    public void updateRotation(int aRotation);
    public Object getSensorService();
}
