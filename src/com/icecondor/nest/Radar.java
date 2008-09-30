package com.icecondor.nest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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
