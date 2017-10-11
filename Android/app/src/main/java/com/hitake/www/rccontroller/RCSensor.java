package com.hitake.www.rccontroller;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by odedc on 04-Jul-16.
 */
public class RCSensor implements SensorEventListener {

    public static final int SENSOR_MAX_ROTATION = 50;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    float mRotationMatrix[] = new float[9];
    float mInclinationMatrix[] = new float[9];
    float mOrientation[] = new float[3];
    float[] mGravity = new float[3];
    float[] mGeomagnetic = new float[3];

    SensorListener mListener = null;

    public RCSensor(SensorListener aListener) {
        mListener = aListener;
        mSensorManager = (SensorManager) aListener.getSensorService();
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    protected void onResume() {
        /*register the sensor listener to listen to the gyroscope sensor, use the
        callbacks defined in this class, and gather the sensor information as quick
        as possible*/
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onStop() {
        //unregister the sensor listener
        mSensorManager.unregisterListener(this);
    }

    protected void onPause() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if ((mGravity == null) || (mGeomagnetic == null))
            return;

        boolean success = SensorManager.getRotationMatrix(mRotationMatrix, mInclinationMatrix, mGravity, mGeomagnetic);
        if (success) {

            SensorManager.getOrientation(mRotationMatrix, mOrientation);

            /*float azimuth = (float)Math.toDegrees(mOrientation[0]);
            float pitch = (float)Math.toDegrees(mOrientation[1]);
            float roll = (float)Math.toDegrees(mOrientation[2]);*/

            //int azimuth = (int)Math.toDegrees(mOrientation[0]);
            int pitch = (int)Math.toDegrees(mOrientation[1]);
            //int roll = (int)Math.toDegrees(mOrientation[2]);

            if (pitch < - SENSOR_MAX_ROTATION)
                pitch = -SENSOR_MAX_ROTATION;
            else if (pitch > SENSOR_MAX_ROTATION)
                pitch = SENSOR_MAX_ROTATION;
//Log.e("RC","Pitch "+pitch);
            mListener.updateRotation(pitch);
        }
    }}
