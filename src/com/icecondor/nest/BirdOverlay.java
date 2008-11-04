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
		
		String msg = "";
		try {
			Location fix = pigeon.getLastFix();
			if(fix == null) {
				msg = "waiting for fix";
			}
		} catch (RemoteException e) {
			msg = "pigeon not running";
		}
		canvas.drawText(msg, 3, h-53, mPaint);		
	}
}
