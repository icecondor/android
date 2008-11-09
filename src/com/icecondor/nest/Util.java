package com.icecondor.nest;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Util {
	public static String DateTimeIso8601(long offset) {
		DateFormat datePattern =  new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss+00:00");
		datePattern.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date = datePattern.format(new Date(offset));
	    // remap the timezone from 0000 to 00:00 (starts at char 22)
        return date;//.substring (0, 22) + ":" + date.substring (22);
	}
	
	public static String timeAgoInWords(long mark) {
		String ago="none";
		String unit="";
		long seconds_ago = (Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis() - mark)/1000;
		unit = "sec.";
		if (seconds_ago > 60) {
			seconds_ago = seconds_ago / 60;
			unit = "min.";
		}
		ago = ""+seconds_ago+" "+unit+" ago";
		return ago;
	}

}
