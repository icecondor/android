package com.icecondor.nest.ui.alist;

import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.icecondor.nest.Constants;
import com.icecondor.nest.Prefs;
import com.icecondor.nest.R;

public class SettingsFragment extends PreferenceFragment
                              implements OnSharedPreferenceChangeListener,
                                         OnPreferenceClickListener {

    private SharedPreferences sharedPrefs;
    private final String[] keys = {Constants.PREFERENCE_API_URL,
                                   Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS,
                                   Constants.PREFERENCE_AUTOSTART,
                                   Constants.PREFERENCE_SOURCE_GPS,
                                   Constants.PREFERENCE_SOURCE_CELL,
                                   Constants.PREFERENCE_SOURCE_WIFI,
                                   Constants.PREFERENCE_EVENT_CONNECTED,
                                   Constants.PREFERENCE_EVENT_CONNECTING,
                                   Constants.PREFERENCE_EVENT_DISCONNECTED,
                                   Constants.PREFERENCE_EVENT_HEARTBEAT,
                                   Constants.PREFERENCE_LOGOUT,
                                   Constants.PREFERENCE_VERSION
                                   };

    public SettingsFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "SettingsFragment onCreate");

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Connect any validators
        connectValidators();

        // logout is special
        Preference logout = findPreference(Constants.PREFERENCE_LOGOUT);
        logout.setOnPreferenceClickListener(this);
    }

    private void connectValidators() {
        findPreference(Constants.PREFERENCE_API_URL).setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String url = (String) newValue;
                    try {
                        new URI(url);
                        return true;
                    } catch (URISyntaxException e) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Invalid API URL", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Settings");
        refreshSummaries();
    }

    protected void refreshSummaries() {
        for(String key: keys) {
            refreshSummary(key);
        }
    }

    public void refreshSummary(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        String summary = "";
        try {
            summary = sharedPrefs.getString(key, "<none>");
            if(key.equals(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS)) {
                int seconds = Integer.parseInt(summary);
                int minutes = seconds/60;
                if(seconds < 60) {
                    summary = "every "+seconds+" seconds";
                } else if (seconds == 60) {
                    summary = "every minute";
                } else if (seconds > 60) {
                    summary = "every "+minutes+" minutes";
                }
            }
        } catch (java.lang.ClassCastException e) {
            boolean onOff = sharedPrefs.getBoolean(key, false);
            summary = onOff ? "On" : "Off";
        }
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if(key.equals(Constants.PREFERENCE_SOURCE_GPS)) {
            if(sharedPrefs.getBoolean(key, false) && !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                summary += " - Warning: System GPS is OFF";
            }
        }
        if(key.equals(Constants.PREFERENCE_SOURCE_CELL) || key.equals(Constants.PREFERENCE_SOURCE_WIFI)) {
            if(sharedPrefs.getBoolean(key, false)  && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                summary += " - Warning: NETWORK set to OFF";
            }
        }
        if(key.equals(Constants.PREFERENCE_VERSION)) {
            summary = Constants.VERSION;
        }
        Log.d(Constants.APP_TAG, "prefSummary for "+key);
        if(key.equals(Constants.PREFERENCE_LOGOUT)) {
            summary = ((Main)getActivity()).prefs.getAuthenticatedEmail();
        }
        Log.d(Constants.APP_TAG, "prefSummary for "+key+" is "+summary);
        preference.setSummary(summary);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        Prefs prefs = new Prefs(sharedPreferences);
        if(preference != null) {
            if(key.equals(Constants.PREFERENCE_AUTOSTART)){
                boolean onOff = sharedPreferences.getBoolean(Constants.PREFERENCE_AUTOSTART, false);
                if(onOff){

                } else {

                }
            }
            if(key.equals(Constants.PREFERENCE_API_URL)){
                URI apiUrl = URI.create(sharedPreferences.getString(Constants.PREFERENCE_API_URL, null));
                ((Main)getActivity()).resetApiUrl(apiUrl);
            }
            if(key.equals(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS)) {
                ((Main)getActivity()).resetTimersAndConnection();
                ((Main)getActivity()).configChangeRecord("frequency", ""+prefs.getRecordingFrequencyInSeconds()/60);
            }
            if(key.equals(Constants.PREFERENCE_SOURCE_GPS) ||
                    key.equals(Constants.PREFERENCE_SOURCE_CELL) ||
                    key.equals(Constants.PREFERENCE_SOURCE_WIFI) ){
                ((Main)getActivity()).resetTimersAndConnection();
                ((Main)getActivity()).configChangeRecord("source", "GPS: "+prefs.isGpsOn()+" Cell:"+prefs.isCellOn()+" Wifi:"+prefs.isWifiOn());
            }

            refreshSummary(key);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.d(Constants.APP_TAG, "onPreferenceClick "+preference.getKey());
        if(preference.getKey().equals(Constants.PREFERENCE_LOGOUT)) {
            ((Main)getActivity()).doLogout();
        }
        return false;
    }
}
