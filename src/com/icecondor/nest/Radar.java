package com.icecondor.nest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class Radar extends MapActivity {
	static final String appTag = "Radar";
	MapController controller;
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radar);
        MapView mapView = (MapView) findViewById(R.id.radar_mapview);
        controller = mapView.getController();
        controller.setZoom(9);
        controller.animateTo(new GeoPoint(45500000,-122500000)); // hard coded scroll over portland
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	// copy/pasted from nest.java. extract into superclass?
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(appTag, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, 0, R.string.menu_version);
		menu.add(0, R.string.menu_first, 0, R.string.menu_first);
		menu.add(0, R.string.menu_second, 0, R.string.menu_second);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		if (item.getItemId() == R.string.menu_second) {
			startActivity(new Intent(this, Nest.class));
		}
		return false;
	}

}
