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
	Location last_fix;
	Point me = new Point();
	Paint me_paint = new Paint();
	java.text.DecimalFormat accuracy_format;
	java.text.DecimalFormat latlong_format;
	
	public Location getLast_fix() {
		return last_fix;
	}
	
	public void setLast_fix(Location last_fix) {
		Log.i("draw", "new last fix"+last_fix);
		this.last_fix = last_fix;
	}
	
	BirdOverlay() {
		Log.i(appTag, "BirdOverlay constructed");
		me_paint.setColor(Color.BLUE);
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
		if (last_fix == null) {
			msg = "waiting for fix";
		} else {
			GeoPoint geo_me = new GeoPoint(
                    (int)(last_fix.getLatitude()*1000000), 
                    (int)(last_fix.getLongitude()*1000000));
			mapView.getProjection().toPixels(geo_me, me);
			canvas.drawCircle(me.x, me.y, 5, me_paint);
			
			msg = latlong_format.format(last_fix.getLatitude()) + " " + latlong_format.format(last_fix.getLongitude()) +
			      "    " + Util.timeAgoInWords(last_fix.getTime()) + "  " + accuracy_format.format(last_fix.getAccuracy())
			      + "m. acc.";
		}
		canvas.drawText(msg, 3, h - 53, mPaint);
	}
}
