package com.icecondor.nest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Nest extends Activity {
	
	static final String appTag = "IceNest";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Start the pigeon service
        Intent pigeon_service = new Intent(this, Pigeon.class);
        startService(pigeon_service);
        Log.i(appTag, "in create: Pigeon service start intent given");
    }

    @Override
    public void onStart() {
    	super.onStart();
    	Log.i(appTag, "onStart yeah");
    }
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(appTag, "onResume yeah");
    }
    @Override
    public void onPause() {
    	super.onPause();
    	Log.i(appTag, "onPause yeah");
    }
}