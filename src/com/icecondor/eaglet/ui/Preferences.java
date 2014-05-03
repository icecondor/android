package com.icecondor.eaglet.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.icecondor.eaglet.R;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    SharedPreferences sharedPrefs;
    String[] keys = {"api_url"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setDefaults(sharedPrefs);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void setDefaults(SharedPreferences sharedPrefs) {
        // TODO: put preferences defaults into an XML file or something
        String key = "api_url";
        String deflt = "wss://icecondor.com/api/v2";
        if(!sharedPrefs.contains(key)) {
            sharedPrefs.edit().putString(key, deflt).commit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSummaries();
    }

    public void refreshSummaries() {
        for(String key: keys) {
            refreshSummary(key);
        }
    }

    public void refreshSummary(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        String summary = sharedPrefs.getString(key, "<none>");
        preference.setSummary(summary);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        refreshSummary(key);
    }

}
