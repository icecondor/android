package com.icecondor.nest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

import com.icecondor.nest.util.Streams;

public class Util implements Constants {
	public static String DateTimeIso8601Now() {
		return Util.DateTimeIso8601(System.currentTimeMillis());
	}
	
	public static String dateTimeIso8601NowLocalShort() {
		return Util.DateTimeIso8601(System.currentTimeMillis(), TimeZone.getDefault()).substring(0,19);
	}
	
	public static String DateTimeIso8601(long offset) {
		String date_str = DateTimeIso8601(offset, TimeZone.getTimeZone("GMT"));
		if (Build.VERSION.SDK_INT >= 7) {
			// Android 2.1,2.2 timezone bug
			// http://code.google.com/p/android/issues/detail?id=8258
			date_str = date_str.substring(0, 19)+"Z";
		}
		return date_str;
	}
	
	public static String DateTimeIso8601(long offset, TimeZone tz) {
		DateFormat datePattern =  new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ssZ");
		datePattern.setTimeZone(tz);
		String date = datePattern.format(new Date(offset));
        return date;
	}
	
	public static Date DateRfc822(String date) {
		 SimpleDateFormat rfc822DateFormats[] = new SimpleDateFormat[] { 
				 new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z"), 
				 new SimpleDateFormat("EEE, d MMM yy HH:mm z"), 
				 new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"), 
				 new SimpleDateFormat("EEE, d MMM yyyy HH:mm z"), 
				 new SimpleDateFormat("d MMM yy HH:mm z"), 
				 new SimpleDateFormat("d MMM yy HH:mm:ss z"), 
				 new SimpleDateFormat("d MMM yyyy HH:mm z"), 
				 new SimpleDateFormat("d MMM yyyy HH:mm:ss z"),
				 new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
				 new SimpleDateFormat("yyyyMMdd'T'HHmmss"),
				 new SimpleDateFormat("yyyyMMdd")};
		 
		 // Substitute Z with +00:00
		 if(date.endsWith("Z")) {
			 date = date.substring(0, date.length()-1)+"+00:00";
		 }
		 
		 // Take off thousandths of a second 2012-01-27T08:01:32.354+00:00
		 if(date.length() == 29) {
		     date = date.substring(0,19) + date.substring(23,29);
		 }
		 
		 Date parsed = null;
		 for(int i=0; i < rfc822DateFormats.length; i++) {
			 try {
				parsed = rfc822DateFormats[i].parse(date);
				break;
			} catch (ParseException e) {
				if(i==rfc822DateFormats.length-1) {
					Log.i(APP_TAG, "DateRfc822 parse failed on "+date);
				}
			}
		 }
		 return parsed;
	}
	public static String timeAgoInWords(long mark) {
		String ago="none";
		long milliseconds_ago = (Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis() - mark);
		ago = millisecondsToWords(milliseconds_ago);
		return ago;
	}

	public static String millisecondsToWords(long duration) {
		long count = duration / 1000;
		String unit = "sec";
		if (count > 60) {
			count = count / 60;
			unit = "min";
		}
		return ""+count+" "+unit;
	}

    /** A name/value pair. */
    public static class Parameter implements Map.Entry<String, String> {

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        private final String key;

        private String value;

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String setValue(String value) {
            try {
                return this.value;
            } finally {
                this.value = value;
            }
        }

        @Override
        public String toString() {
            return getKey() + '=' + getValue();
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Parameter that = (Parameter) obj;
            if (key == null) {
                if (that.key != null)
                    return false;
            } else if (!key.equals(that.key))
                return false;
            if (value == null) {
                if (that.value != null)
                    return false;
            } else if (!value.equals(that.value))
                return false;
            return true;
        }
    }

	public static String DateToShortDisplay(Date date) {
		String date_string;
		String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
		String month_name = months[date.getMonth()];
		String ampm;
		int hours = date.getHours();
		if(date.getHours() > 12) {
			ampm="pm";
			hours = hours - 12;
		} else {
			ampm="am";
		}
		date_string = ""+hours+":"+date.getMinutes()+ampm+" "+month_name+" "+date.getDate();
		return date_string;
	}
	
	public static boolean profilePictureExists(String username, Context ctx) {
	    File file = profileFile(username, ctx);
	    return file.isFile();
	}

	public static String profileDirectory(Context ctx) {
        File parent_dir = ctx.getFilesDir();
        return parent_dir.getPath()+File.separator+AVATAR_DIR;
	}
	
    protected static File profileFile(String username, Context ctx) {
        return new File(profileDirectory(ctx)+File.separator+username);
    }
    
	public static void profilePictureSave(String username, InputStream imageStream, Context ctx) {
        boolean mdir = (new File(profileDirectory(ctx))).mkdir();
        Log.i(APP_TAG, ""+profileDirectory(ctx)+" "+mdir);
        File avatar = profileFile(username, ctx);
	    try {
	        FileOutputStream os = new FileOutputStream(avatar);
	        Streams.copy(imageStream, os, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

    public static FileInputStream profilePictureLoad(String username, Context ctx) {
        try {
            File avatar = profileFile(username, ctx);
            return new FileInputStream(avatar);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void profilePictureDelete(String service_extra, Context ctx) {
        profileFile(service_extra, ctx).delete();        
    }
    
	public static Drawable drawableGravatarFromUsername(String username, Context ctx) {
		TypedValue typedValue = new TypedValue();
		//density none divides by the density, density default keeps the original size
		typedValue.density = TypedValue.DENSITY_DEFAULT;
		Drawable avatar = Drawable.createFromResourceStream(null, typedValue, 
		                         Util.profilePictureLoad(username, ctx), 
		                         username);
		return avatar;
	}

}
