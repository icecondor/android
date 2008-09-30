package com.icecondor.nest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Start extends Activity {
	static final String appTag = "Start";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, Radar.class));
    }
}
