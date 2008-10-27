package com.icecondor.nest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.RectF;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class BirdOverlay extends Overlay {
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// TODO Auto-generated method stub
		super.draw(canvas, mapView, shadow);
		Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//canvas.drawBitmap(new Bitmap(), 10, 10, mPaint);
	}
}
