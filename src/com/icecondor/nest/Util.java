package com.icecondor.nest;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Util {
	public static String DateTimeIso8601(long offset) {
		DateFormat date_pattern =  new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
		String date = date_pattern.format(new Date(offset));
	    // remap the timezone from 0000 to 00:00 (starts at char 22)
        return date;//.substring (0, 22) + ":" + date.substring (22);
		//return "dateholder";
	}
}
