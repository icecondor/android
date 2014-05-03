package com.icecondor.nest.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.icecondor.nest.R;

public class Preferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
