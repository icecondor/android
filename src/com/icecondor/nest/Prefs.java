package com.icecondor.nest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.nest.ui.login.Main;

public class Prefs {
    private final SharedPreferences prefs;
    private String KEY_CONFIGURED = "configured";

    public Prefs(Context ctx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        if(prefs.getBoolean(KEY_CONFIGURED, false) == false) {
            ensureDefaults();
        }
    }

    public void setDeviceId(String deviceId) {
        prefs.edit().putString(Constants.SETTING_DEVICE_ID, deviceId).commit();
    }

    public String getDeviceId() {
        return prefs.getString(Constants.SETTING_DEVICE_ID, null);
    }

    public URI getApiUrl() {
        try {
            return new URI(prefs.getString(Constants.PREFERENCE_API_URL,
                                           Constants.ICECONDOR_API_URL));
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public int getRecordingFrequencyInSeconds() {
        String secondsStr = prefs.getString(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS, "NaN");
        return Integer.parseInt(secondsStr);
    }

    public boolean isOnOff() {
        return prefs.getBoolean(Constants.SETTING_ON_OFF, true);
    }

    public void setOnOff(boolean b) {
        prefs.edit().putBoolean(Constants.SETTING_ON_OFF, b).commit();
    }

    public boolean isGpsOn() {
        return prefs.getBoolean(Constants.PREFERENCE_SOURCE_GPS, true);
    }

    public boolean isCellOn() {
        return prefs.getBoolean(Constants.PREFERENCE_SOURCE_CELL, true);
    }

    public boolean isWifiOn() {
        return prefs.getBoolean(Constants.PREFERENCE_SOURCE_WIFI, true);
    }

    public boolean isStartOnBoot() {
        return prefs.getBoolean(Constants.PREFERENCE_AUTOSTART, true);
    }

    private void ensureDefaults() {
        /* set default user preferences */
        Properties props = loadProperties();
        Editor editor = prefs.edit();
        /* Autostart: true */
        editor.putBoolean(Constants.PREFERENCE_AUTOSTART, true);

        /* On/Off: On */
        editor.putBoolean(Constants.SETTING_ON_OFF, true);

        /* Recording frequency: 3 minutes */
        editor.putString(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS, "180");

        /* API url */
        String apiUrl;
        if(props.contains("api_url")) {
            apiUrl = props.getProperty("api_url");
        } else {
            apiUrl = Constants.ICECONDOR_API_URL;
        }
        editor.putString(Constants.PREFERENCE_API_URL, apiUrl);

        /* Location Sources */
        editor.putBoolean(Constants.PREFERENCE_SOURCE_GPS, true);
        editor.putBoolean(Constants.PREFERENCE_SOURCE_CELL, true);
        editor.putBoolean(Constants.PREFERENCE_SOURCE_WIFI, true);

        editor.putBoolean(KEY_CONFIGURED, true);
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

    public void setAuthenticatedUsername(String username) {
        prefs.edit().putString(Main.PREF_KEY_AUTHENTICATED_USER_NAME, username).commit();
    }

    public String getAuthenticatedUsername() {
        return prefs.getString(Main.PREF_KEY_AUTHENTICATED_USER_NAME, null);
    }

    public void clearAuthenticatedUser() {
        prefs.edit().remove(Main.PREF_KEY_AUTHENTICATED_USER_ID).commit();
        prefs.edit().remove(Main.PREF_KEY_AUTHENTICATED_USER_NAME).commit();
    }

    public boolean isAuthenticatedUser() {
        return prefs.getString(Main.PREF_KEY_AUTHENTICATED_USER_ID, null) != null;
    }

    public void setUnvalidatedToken(String token) {
        prefs.edit().putString(Main.PREF_KEY_UNVERIFIED_TOKEN, token).commit();
    }

    public String getUnvalidatedToken() {
        return prefs.getString(Main.PREF_KEY_UNVERIFIED_TOKEN, null);
    }

    public void clearUnvalidatedToken() {
        prefs.edit().remove(Main.PREF_KEY_UNVERIFIED_TOKEN).commit();
    }

    public void setAuthenticationToken(String token) {
        prefs.edit().putString(Main.PREF_KEY_AUTHENTICATION_TOKEN, token).commit();
    }

    public String getAuthenticationToken() {
        return prefs.getString(Main.PREF_KEY_AUTHENTICATION_TOKEN, null);
    }

    public void clearAuthenticationToken() {
        prefs.edit().remove(Main.PREF_KEY_AUTHENTICATION_TOKEN).commit();
    }

}
