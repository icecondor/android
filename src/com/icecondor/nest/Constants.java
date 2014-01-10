package com.icecondor.nest;

public interface Constants {
	public static final int ICECONDOR_VERSION = 20140102;
	public static final String ICECONDOR_URL = "http://icecondor.com/";
	public static final String ICECONDOR_API_URL = "ws://api.icecondor.com/";
	public static final String ICECONDOR_URL_SHORTNAME = "icecondor.com"; // use preference
	public static final String ICECONDOR_VERSION_CHECK_URL = "http://icecondor.com/version"; // use preference
	public static final String ICECONDOR_WRITE_URL = "http://icecondor.com/locations.json"; // use preference
	public static final String ICECONDOR_READ_URL = "http://icecondor.com/locations.json"; // use preference
	public static final String ICECONDOR_OAUTH_REQUEST_URL = "http://icecondor.com/oauth/request_token";
	public static final String ICECONDOR_OAUTH_AUTHORIZATION_URL = "http://icecondor.com/oauth/authorize";
	public static final String ICECONDOR_LOGIN_URL = "http://icecondor.com/mobile/login";
	public static final String ICECONDOR_OAUTH_ACCESS_URL = "http://icecondor.com/oauth/access_token";
	public static final String ICECONDOR_OAUTH_CALLBACK = "icecondor-android-app:///";
	public static final String GETSATISFACTION_URL = "http://getsatisfaction.com/icecondor";
	public static final long RADAR_REFRESH_INTERVAL = 15000;

	public static final String SETTING_PIGEON_TRANSMITTING = "pigeon_transmitting";
	public static final String SETTING_LAST_VERSION_CHECK = "version_check_date";
	//public static final String SETTING_RECORD_FREQUENCY = "record frequency";
	public static final String SETTING_RSS_READ_FREQUENCY = "rss read frequency";
	public static final String SETTING_TRANSMISSION_FREQUENCY = "transmission frequency";
	public static final String SETTING_OPENID = "openid";
	public static final String SETTING_LICENSE_AGREE = "licence agree";
	public static final String SETTING_BEEP_ON_FIX = "beep on fix";

	public static final long DAY_IN_MILLISECONDS = 1000*60*60*24;

	public static final String GPS_FIX_ACTION = "com.icecondor.nest.GPS_FIX_ACTION";
    public static final String BIRD_FIX_ACTION = "com.icecondor.nest.BIRD_FIX_ACTION";
    public static final String BIRD_UPDATE_ACTION = "com.icecondor.nest.BIRD_UPDATE_ACTION";
    public static final String AUTH_OK_ACTION = "com.icecondor.nest.AUTH_OK_ACTION";
    public static final String USER_PROFILE_UPDATE_ACTION = "com.icecondor.nest.USER_PROFILE_UPDATE_ACTION";

	public static final String APP_TAG = "icecondor";

	public static final int SERVER_CONNECT_TIMEOUT = 15;
	public static final String AVATAR_DIR = "avatars";
}
