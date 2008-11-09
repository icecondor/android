package com.icecondor.nest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.RectF;
import android.location.Location;
import android.os.RemoteException;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class BirdOverlay extends Overlay {
	PigeonService pigeon;
	Location last_fix;
	
	public Location getLast_fix() {
		return last_fix;
	}
	
	public void setLast_fix(Location last_fix) {
		this.last_fix = last_fix;
	}
	
	BirdOverlay(PigeonService _pigeon) {
		pigeon = _pigeon;
	}
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		int h = canvas.getHeight();
		int w = canvas.getWidth();
		int mid_y = h /2;
		int mid_x = w /2;
		Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		String msg = null;
		if (last_fix == null) {
			msg = "waiting for fix";
		} else {
			msg = last_fix.getLatitude() + " " + last_fix.getLongitude();
		}
		canvas.drawText(msg, 3, h - 53, mPaint);		
	}
}
