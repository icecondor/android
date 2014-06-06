package com.icecondor.eaglet.ui.alist;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "SettingsFragment onCreate");

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
