package com.icecondor.eaglet;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.icecondor.eaglet.ui.alist.Main;

public class Start extends Activity {
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.APP_TAG, "Start onStart");
        Intent condorIntent = new Intent(this, Condor.class);
        startService(condorIntent); // keep this for STICKY result
        new Prefs(this); // ensure defaults are set
        startActivity(new Intent(this, Main.class));
    }

}
