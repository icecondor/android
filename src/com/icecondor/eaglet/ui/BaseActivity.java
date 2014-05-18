package com.icecondor.eaglet.ui;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;

import com.icecondor.eaglet.R;

public class BaseActivity extends ActionBarActivity {
    protected void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, fragment).commit();
    }
}
