package com.icecondor.eaglet.ui.alist;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.Prefs;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.login.Main;

public class SettingsFragment extends PreferenceFragment
                              implements OnSharedPreferenceChangeListener,
                                         OnPreferenceClickListener {

    private SharedPreferences sharedPrefs;
    private final String[] keys = {Constants.PREFERENCE_API_URL,
                                   Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS};
    private IInAppBillingService mBillingService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "SettingsFragment onCreate");

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        Preference logout = findPreference("logout_pref_notused");
        logout.setOnPreferenceClickListener(this);
        Preference buy_high_rate = findPreference("purchase_high_recording_rate");
        buy_high_rate.setOnPreferenceClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSummaries();
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getActivity().bindService(serviceIntent, mBillingServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBillingService != null) {
            getActivity().unbindService(mBillingServiceConn);
        }
    }

    protected void refreshSummaries() {
        for(String key: keys) {
            refreshSummary(key);
        }
    }

    public void refreshSummary(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        String summary = sharedPrefs.getString(key, "<none>");
        if(key.equals(Constants.PREFERENCE_RECORDING_FREQUENCY_SECONDS)) {
            int seconds = Integer.parseInt(summary);
            int minutes = seconds/60;
            if(seconds < 60) {
                summary = "every "+seconds+" seconds";
            } else if (seconds == 60) {
                summary = "every minute";
            } else if (seconds > 60) {
                summary = "every "+minutes+" minutes";
            }
        }
        preference.setSummary(summary);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        if(preference != null) {
            refreshSummary(key);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.d(Constants.APP_TAG, "onPreferenceClick "+preference.getKey());
        if(preference.getKey().equals("logout_pref_notused")) {
            Prefs prefs = new Prefs(this.getActivity());
            Log.d(Constants.APP_TAG, "logging out "+prefs.getAuthenticatedUsername());
            prefs.clearAuthenticatedUser();
            Intent intent = new Intent(this.getActivity(), Main.class);
            startActivity(intent);
        }
        if(preference.getKey().equals("purchase_high_recording_rate")) {
            Log.d(Constants.APP_TAG, "settingsFragment buying high recording rate");
            inAppBuy("high-recording-rate-1");
        }
        return false;
    }

    public void inAppBuy(String sku) {
        try {
            Bundle buyIntentBundle = mBillingService.getBuyIntent(3, getActivity().getPackageName(),
                    sku, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                    1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (SendIntentException e) {
            e.printStackTrace();
        }
    }

    ServiceConnection mBillingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(Constants.APP_TAG, "settingsFragment billing service connected");
            mBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
           IBinder service) {
            Log.d(Constants.APP_TAG, "settingsFragment billing service connected");
            mBillingService = IInAppBillingService.Stub.asInterface(service);
        }
     };
}
