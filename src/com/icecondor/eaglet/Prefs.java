package com.icecondor.eaglet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.eaglet.ui.login.Main;

public class Prefs {
    private final SharedPreferences prefs;

    public Prefs(Context ctx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        ensureDefaults();
    }

    public void setDeviceId(String deviceId) {
        prefs.edit().putString(Constants.SETTING_DEVICE_ID, deviceId).commit();
    }

    public String getDeviceId() {
        return prefs.getString(Constants.SETTING_DEVICE_ID, null);
    }

    public String getApiUrl() {
        return prefs.getString(Constants.PREFERENCE_API_URL,
                               Constants.ICECONDOR_API_URL);
    }

    public int getRecordingFrequencyInSeconds() {
        String secondsStr = prefs.getString(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS, "NaN");
        return Integer.parseInt(secondsStr);
    }

    public boolean getOnOff() {
        return prefs.getBoolean(Constants.SETTING_ON_OFF, true);
    }

    private void ensureDefaults() {
        /* set default user preferences */
        Properties props = loadProperties();
        Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREFERENCE_AUTOSTART, true);
        editor.putString(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS, "60");
        String apiUrl;
        if(props.contains("api_url")) {
            apiUrl = props.getProperty("api_url");
        } else {
            apiUrl = Constants.ICECONDOR_API_URL;
        }
        editor.putString(Constants.PREFERENCE_API_URL, apiUrl);

        editor.commit();
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try {
            props.loadFromXML(new FileInputStream(new File("preference_defaults.xml")));;
        } catch (InvalidPropertiesFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            Log.d(Constants.APP_TAG, "preference_defaults.xml not found. ignoring.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    public String getAuthenticatedUserId() {
        return prefs.getString(Main.PREF_KEY_AUTHENTICATED_USER_ID, null);
    }

    public void setAuthenticatedUserId(String userId) {
        prefs.edit().putString(Main.PREF_KEY_AUTHENTICATED_USER_ID, userId).commit();
    }

    public boolean isAuthenticatedUser() {
        return prefs.getString(Main.PREF_KEY_AUTHENTICATED_USER_ID, null) != null;
    }

}
