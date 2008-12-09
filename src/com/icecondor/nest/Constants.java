package com.icecondor.nest;

public interface Constants {
	public static final int ICECONDOR_VERSION = 20081209;
	public static final String ICECONDOR_VERSION_CHECK_URL = "http://icecondor.com/version"; // use preference
	public static final String ICECONDOR_WRITE_URL = "http://icecondor.com/locations.json"; // use preference
	public static final String ICECONDOR_READ_URL = "http://icecondor.com/locations.json"; // use preference
	public static final long ICECONDOR_READ_INTERVAL = 60000;

	public static final String SETTING_PIGEON_TRANSMITTING = "pigeon_transmitting";
	public static final String SETTING_LAST_VERSION_CHECK = "version_check_date";
	public static final String SETTING_RECORD_FREQUENCY = "record frequency";
	public static final String SETTING_TRANSMISSION_FREQUENCY = "transmission frequency";
	public static final String SETTING_OPENID = "openid";
	
	public static final long DAY_IN_MILLISECONDS = 1000*60*60*24;

}
