package com.icecondor.nest;

import java.util.ArrayList;
import java.util.ListIterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

public class FlockOverlay extends ItemizedOverlay<BirdItem> {

	private ArrayList<BirdItem> birds = new ArrayList<BirdItem>();
	private String appTag = "FlockOverlay";
	private BirdItem last_bird;
	private Context app;
	
	public FlockOverlay(Drawable defaultMarker, Context app) {
		super(boundCenterBottom(defaultMarker));
		this.app = app;
	}
	
	public void add(BirdItem bird, Drawable marker) {
		bird.setMarker(boundCenterBottom(marker));
		birds.add(bird);
		populate();
	}
	
	@Override
	protected BirdItem createItem(int i) {
		return birds.get(i);
	}

	@Override
	public int size() {
		return birds.size();
	}

	public boolean contains(String guid) {
		ListIterator<BirdItem> list = birds.listIterator();
		while(list.hasNext()) {
			BirdItem bird = list.next();
			if(bird.getTitle().equals(guid))
				return true;
		}
		return false;
	}
	
	public boolean onTap(GeoPoint p, MapView mapView) {
		Log.i(appTag, "got TAP for map location "+p);
		if(last_bird != null) {
			setFocus(null);
		}
		return true;
	}
	
	public boolean onTap(int index) {
		Log.i(appTag, "got TAP on item#"+index);
		last_bird = birds.get(index);
		setFocus(last_bird);
		Toast.makeText(app, last_bird.getSnippet(), Toast.LENGTH_SHORT).show();
		return true;
	}
	
	public void draw(Canvas canvas, MapView view, boolean shadow ) {
		super.draw(canvas, view, shadow);
		//draw other stuffs...
	} 
}
