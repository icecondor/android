package com.icecondor.nest;

import java.io.IOException;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class Start extends Activity implements ServiceConnection,
												Constants {
	static final String appTag = "Start";

	Intent pigeon_intent;
	PigeonService pigeon;
	SharedPreferences settings;
	Intent next_intent;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
		settings = getSharedPreferences(PREFS_NAME, 0);
        pigeon_intent = new Intent(this, Pigeon.class);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        startPigeon();
        check_for_new_version();
    }

    private void check_for_new_version() {
        long version_check_date = settings.getLong(SETTING_LAST_VERSION_CHECK, 0);
        if (version_check_date < (System.currentTimeMillis() - DAY_IN_SECONDS)) {
        	// request version data
			HttpClient client = new DefaultHttpClient();
			String url_with_params = ICECONDOR_VERSION_CHECK_URL;
			Log.i(appTag, "GET " + url_with_params);
			HttpGet get = new HttpGet(url_with_params);
			HttpResponse response;
			try {
				response = client.execute(get);
				HttpEntity entity = response.getEntity();
				String json = EntityUtils.toString(entity);
				Log.i(appTag, "http response: " + response.getStatusLine() +
						      " "+json);
				try {
					JSONObject version_info = new JSONObject(json);
					int remote_version = version_info.getInt("version");
					if (ICECONDOR_VERSION < remote_version) {
						Uri new_version_url = Uri.parse(version_info.getString("url"));
						Log.i(appTag, "Upgrade! -> "+new_version_url);
					    next_intent = new Intent(Intent.ACTION_VIEW, new_version_url);
					}
					Log.i(appTag, "current version "+ICECONDOR_VERSION+" remote version "+remote_version);
				} catch (JSONException e) {
				}

			} catch (ClientProtocolException e) {
			} catch (IOException e) {
			}

        }    	
    }
    
    private void restorePreferences() {
		Log.i(appTag, "restorePreferences()");

		Editor editor = settings.edit();

        // Set the unique ID
		String uuid;
		if(settings.contains("uuid")) {
			uuid = settings.getString("uuid", null);
			Log.i(appTag, "retrieved UUID of "+uuid);
		} else {
			uuid = "urn:uuid:"+UUID.randomUUID().toString();
			editor.putString("uuid", uuid);
			editor.commit();
			Log.i(appTag, "no UUID in preferences. generated "+uuid);
		}
	}

	private void startPigeon() {
		// Start the pigeon service
    	Intent pigeon_service = new Intent(this, Pigeon.class);
        startService(pigeon_service);
        bindService(pigeon_intent, this, 0); // 0 = do not auto-start
	}
	
	private void stopPigeon() {
		Log.i(appTag, "stopPigeon");
		unbindService(this);
		stopService(new Intent(this, Pigeon.class));
	}
	
	public void onPause() {
		super.onPause();
		unbindService(this);
		finish();
	}
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
        restorePreferences();
        // handoff to the Radar
        if(next_intent == null) {
        	next_intent = new Intent(this, Radar.class);
        }
        startActivity(next_intent);
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
		
	}

}
