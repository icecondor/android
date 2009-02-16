package com.icecondor.nest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class BirdOverlay extends Overlay {
	String appTag = "BirdOverlay";
	Location last_local_fix, last_pushed_fix;
	Point me = new Point();
	Paint me_paint = new Paint();
	Paint me_pushed_paint = new Paint();
	java.text.DecimalFormat accuracy_format;
	java.text.DecimalFormat latlong_format;
	
	public Location getLastLocalFix() {
		return last_local_fix;
	}
	
	public void setLastLocalFix(Location last_fix) {
		this.last_local_fix = last_fix;
	}
	
	public Location getLastPushedFix() {
		return last_pushed_fix;
	}
	
	public void setLastPushedFix(Location last_fix) {
		this.last_pushed_fix = last_fix;
	}
	
	BirdOverlay() {
		Log.i(appTag, "BirdOverlay constructed");
		me_paint.setColor(Color.BLUE);
		me_pushed_paint.setColor(Color.GREEN);
		latlong_format = new java.text.DecimalFormat("###.######");
		accuracy_format = new java.text.DecimalFormat("####");
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		int h = canvas.getHeight();
		//int w = canvas.getWidth();
		//int mid_y = h /2;
		//int mid_x = w /2;
		Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		String msg = null;
		if (last_local_fix == null) {
			msg = "waiting for fix";
		} else {
			GeoPoint geo_me = new GeoPoint(
                    (int)(last_local_fix.getLatitude()*1000000), 
                    (int)(last_local_fix.getLongitude()*1000000));
			GeoPoint geo_pushed_me = new GeoPoint(
                    (int)(last_local_fix.getLatitude()*1000000), 
                    (int)(last_local_fix.getLongitude()*1000000));

			mapView.getProjection().toPixels(geo_me, me);
			canvas.drawCircle(me.x, me.y, 5, me_paint);
			mapView.getProjection().toPixels(geo_pushed_me, me);
			canvas.drawCircle(me.x, me.y, 2, me_pushed_paint);
			
			msg = latlong_format.format(last_local_fix.getLatitude()) + " " + latlong_format.format(last_local_fix.getLongitude()) +
			      "    " + Util.timeAgoInWords(last_local_fix.getTime()) + "  " + accuracy_format.format(last_local_fix.getAccuracy())
			      + "m. acc.";
		}
		canvas.drawText(msg, 3, h - 53, mPaint);
	}
}
