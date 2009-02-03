package com.icecondor.nest;

import java.io.IOException;
import java.util.Calendar;
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class Start extends Activity implements ServiceConnection,
												Constants {
	static final String appTag = "Start";

	Intent pigeon_intent;
	PigeonService pigeon;
	SharedPreferences settings;
	Intent next_intent;
	NotificationManager notificationManager;
	boolean pigeon_bound = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
        pigeon_intent = new Intent(this, Pigeon.class);
    }
    
    @Override
    public void onResume() {
    	Log.i(appTag, "onResume");
    	super.onResume();
    	// extract the OAUTH request token if it exists
    	Uri uri = this.getIntent().getData();
    	if(uri != null) {
    		String blessed_request_token = uri.getQueryParameter("oauth_token");
    		if(blessed_request_token.length() > 0) {
	    		String access_token_and_secret[];
	    		Toast.makeText(this, "Requesting OAUTH access", Toast.LENGTH_SHORT).show();
	    		access_token_and_secret = LocationRepositoriesSqlite.getDefaultRequestToken(this);
	    		Log.i(appTag, "Returned token (ignored): "+blessed_request_token+" Access token: "+access_token_and_secret[0]+" secret:"+access_token_and_secret[1]);
	    		access_token_and_secret = LocationRepositoriesSqlite.convertToAccessTokenAndSecret(access_token_and_secret, this);
				String openid = uri.getQueryParameter("openid");
	    		String msg;
	    		if(access_token_and_secret == null) {
	    			msg = "OAUTH authentication failed.";
	    		} else {
	    			LocationRepositoriesSqlite.setDefaultAccessToken(access_token_and_secret, this);
	    			msg = "OAUTH token acquired for "+openid+".";
	    		}
				Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
				settings.edit().putString(SETTING_OPENID, openid).commit();
    		}
    	}
    		
		if(!settings.contains(SETTING_LICENSE_AGREE)) {
        	Log.i(appTag,"No licence agree");
        	showDialog(1);
        } else {
        	continueOnResume();
        }

    }
    
    public void continueOnResume() {
        startPigeon();
        new Thread( new Runnable() {public void run() {check_for_new_version();} }).start();
    }

    private void check_for_new_version() {
        long version_check_date_in_milliseconds = settings.getLong(SETTING_LAST_VERSION_CHECK, 0);
        Calendar version_check_date = Calendar.getInstance();
        version_check_date.setTimeInMillis(version_check_date_in_milliseconds);
        Log.i(appTag, "date of last update check "+ version_check_date.getTime());
        if (version_check_date_in_milliseconds < (System.currentTimeMillis() - DAY_IN_MILLISECONDS)) {
			settings.edit().putLong(SETTING_LAST_VERSION_CHECK, System.currentTimeMillis()).commit();
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
					    Intent upgrade_www_intent = new Intent(Intent.ACTION_VIEW, new_version_url);
						PendingIntent upgradeIntent = PendingIntent.getActivity(this, 0, upgrade_www_intent, 
								                                                PendingIntent.FLAG_ONE_SHOT);
						Notification notification = new Notification(R.drawable.icecube_statusbar, 
								"Upgrade Available!", System.currentTimeMillis());
						notification.setLatestEventInfo(this, "IceCondor Upgrade", 
								"Version "+remote_version+" is available.", upgradeIntent);
						notification.flags = notification.flags ^ Intent.FLAG_ACTIVITY_NEW_TASK;
						notificationManager.notify(2, notification);
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
		String openid;
		if(settings.contains(SETTING_OPENID)) {
			openid = settings.getString(SETTING_OPENID, null);
			Log.i(appTag, "retrieved OpenID of "+openid);
		} else {
        	// Prompt for a unique identifier

			// On cancel
			openid = "urn:uuid:"+UUID.randomUUID().toString();
			editor.putString(SETTING_OPENID, openid);
			editor.commit();
			Log.i(appTag, "no OpenID in preferences. generated "+openid);
		}
		
		// Set the default Record On/Off
		if(!settings.contains(SETTING_PIGEON_TRANSMITTING)) {
			// default to OFF
			editor.putBoolean(SETTING_PIGEON_TRANSMITTING, true).commit();
		}

		// Set the default position update frequency
		if(!settings.contains(SETTING_TRANSMISSION_FREQUENCY)) {
			// default to 5 minutes
			editor.putString(SETTING_TRANSMISSION_FREQUENCY, "300000").commit();
		}
		
		// Set the default RSS read frequency
		if(!settings.contains(SETTING_RSS_READ_FREQUENCY)) {
			// default to 30 minutes
			editor.putString(SETTING_RSS_READ_FREQUENCY, "1800000").commit();
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.i(appTag, "Creating Dialog "+id);

		return new AlertDialog.Builder(this)
			.setTitle("License")
			.setMessage(R.string.license)
			.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {
					Editor editor = settings.edit();
					editor.putInt(SETTING_LICENSE_AGREE, ICECONDOR_VERSION).commit();
			        continueOnResume();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {
					finish();
				}
			})
			.create();
	}

	private void startPigeon() {
		// Start the pigeon service
    	Intent pigeon_service = new Intent(this, Pigeon.class);
        startService(pigeon_service);
        pigeon_bound = bindService(pigeon_intent, this, 0); // 0 = do not auto-start
	}
	
	private void stopPigeon() {
		Log.i(appTag, "stopPigeon");
		if(pigeon_bound) {
			unbindService(this);
			stopService(new Intent(this, Pigeon.class));
		}
	}
	
	public void onPause() {
		super.onPause();
		if(pigeon_bound) {
			// its possible to pause before binding to pigeon
			unbindService(this);
		}
		finish();
	}
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
        restorePreferences();
        jumpToNextActivity();
	}

	private void jumpToNextActivity() {
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
