package com.icecondor.eaglet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.eaglet.ui.alist.Main;

public class Start extends Activity {
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.APP_TAG, "Start onStart");
        Intent condorIntent = new Intent(this, Condor.class);
        startService(condorIntent); // keep this for STICKY result
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ensurePreferences(prefs);
        startActivity(new Intent(this, Main.class));
    }

    private void ensurePreferences(SharedPreferences prefs) {
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

}
