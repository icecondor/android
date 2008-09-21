package com.icecondor.nest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class Nest extends Activity {
	TabHost myTabHost;
	static final String appTag = "IceNest";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.myTabHost = (TabHost)this.findViewById(R.id.th_set_menu_tabhost);
        this.myTabHost.setup();
        TabSpec ts1 = myTabHost.newTabSpec("TAB1");
        ts1.setIndicator("caption 1", getResources().getDrawable(R.drawable.icon));
        ts1.setContent(R.id.grid_set_menu_page1);
        this.myTabHost.addTab(ts1);
        
        TabSpec ts2 = myTabHost.newTabSpec("TAB2");
        ts2.setIndicator("caption 2", getResources().getDrawable(R.drawable.icon));
        ts2.setContent(R.id.grid_set_menu_page2);
        this.myTabHost.addTab(ts2);
        
        this.myTabHost.setCurrentTab(0);
        
        // Start the pigeon service
        Intent pigeon_service = new Intent(this, Pigeon.class);
        startService(pigeon_service);
        Log.i(appTag, "in create: Pigeon service start");
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