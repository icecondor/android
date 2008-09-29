package com.icecondor.nest;

import android.os.Bundle;
import android.util.Log;
import com.google.android.maps.MapActivity;

public class Radar extends MapActivity {
	static final String appTag = "Radar";
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radar);
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
