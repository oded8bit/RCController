package com.hitake.www.rccontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Static class that holds app preferences
 */
public class RCPrefs {
    private SharedPreferences mPrefs = null;
    private static RCPrefs ourInstance = new RCPrefs();

    // Default values
    public static final int DEF_PORT_NUM = 89;
    public static final String DEF_SSIDE = "AT&T_Guest";
    public static final int DEF_MAX_ROTATION = 30;
    public static final String DEF_SENSITIVITY = "medium";
    public static final boolean DEF_USE_BT = true;

    public static RCPrefs getInstance() {
        return ourInstance;
    }

    private RCPrefs() {
    }

    protected void init(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ((mPrefs.getString("netssid","")).equals("")) {
            setStringPref("netssid",DEF_SSIDE);
            setStringPref("sensitivity",String.valueOf(DEF_PORT_NUM));
            setStringPref("portnum",DEF_SENSITIVITY);
            setStringPref("max_rotation",String.valueOf(DEF_MAX_ROTATION));
            setBooleanPref("use_bt",DEF_USE_BT);
        }
    }

    protected void setStringPref(String key,  String value) {
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(key,value);
        edit.apply();
    }

    protected void setBooleanPref(String key,  boolean value) {
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putBoolean(key,value);
        edit.apply();
    }

    protected String getStringPref(String key,  String defaultVal) {
        return mPrefs.getString(key, defaultVal);
    }

    protected boolean getBooleanPref(String key,  boolean defaultVal) {
        return mPrefs.getBoolean(key, defaultVal);
        //String s = getStringPref(key,String.valueOf(defaultVal));
        //return true;//return Boolean.valueOf(s);
    }

    protected int getIntPref(String key,  int defaultVal) {
        String s = getStringPref(key,String.valueOf(defaultVal));
        return Integer.valueOf(s);
    }
}
