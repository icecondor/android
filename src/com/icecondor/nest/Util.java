package com.icecondor.nest;

import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Util {
	public static String DateTimeIso8601(long offset) {
		DateFormat datePattern =  new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
		datePattern.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date = datePattern.format(new Date(offset));
	    // remap the timezone from 0000 to 00:00 (starts at char 22)
        return date;//.substring (0, 22) + ":" + date.substring (22);
	}
}
