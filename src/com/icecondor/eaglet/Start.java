package com.icecondor.eaglet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Start extends Activity {
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.APP_TAG, "Start onStart");
        Prefs prefs = new Prefs(this); // ensure defaults are set

        Intent condorIntent = new Intent(this, Condor.class);
        startService(condorIntent); // keep this for STICKY result

        String token = tokenFromUri(getIntent().getData());

        Intent nextActivity;
        if(token == null) {
            if(prefs.isAuthenticatedUser()){
                nextActivity = new Intent(this, com.icecondor.eaglet.ui.alist.Main.class);
            } else {
                nextActivity = new Intent(this, com.icecondor.eaglet.ui.login.Main.class);
            }
        } else {
            nextActivity = new Intent(this, com.icecondor.eaglet.ui.login.Main.class);
            prefs.setUnvalidatedToken(token);
        }
        nextActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(nextActivity);
    }

    private String tokenFromUri(Uri uri) {
        if(uri != null) {
            String path = uri.getPath();
            String[] parts = path.split("\\/");
            if(parts.length > 0){
                if(parts[1].equals("v2")) {
                    String token = uri.getQueryParameter("access_token");
                    if (token != null) {
                        Log.d(Constants.APP_TAG, "** found token: "+token);
                        return token;
                    }
                }
            }
        }
        return null;
    }

}
