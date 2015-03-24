package com.icecondor.nest;

public class Constants {

    public static final String APP_TAG = "icecondor";
    public static final int SIGNAL_NEW_ACTIVITY = 1;
    public static final String ACTION_WAKE_ALARM = "com.icecondor.WAKE_ALARM";
    public static final String ICECONDOR_API_URL = "wss://api.icecondor.com/v2";
    public static final String VERSION = "20141218";

    /* internal app settings */
    public static final String SETTING_ON_OFF = "on_off";
    public static final String SETTING_DEVICE_ID = "device_id";

    /* user preferences */
    public static final String PREFERENCE_AUTOSTART = "autostart";
    public static final String PREFERENCE_API_URL = "api_url";
    public static final String PREFERENCE_RECORDING_FREQUENCY_SECONDS = "recording_frequency";
    public static final String PREFERENCE_SOURCE_GPS = "source_gps";
    public static final String PREFERENCE_SOURCE_CELL = "source_cell";
    public static final String PREFERENCE_SOURCE_WIFI = "source_wifi";
    public static final String PREFERENCE_VERSION = "version_string";
    public static final String PREFERENCE_LOGOUT = "logout_pref";
    public static final String PREFERENCE_PERSISTENT_RECONNECT = "persistent_reconnect";
    public static final String PREFERENCE_EVENT_CONNECTING = "event_connecting";
    public static final String PREFERENCE_EVENT_CONNECTED = "event_connected";
    public static final String PREFERENCE_EVENT_DISCONNECTED = "event_disconnected";
    public static final String PREFERENCE_EVENT_HEARTBEAT= "event_heartbeat";

}
