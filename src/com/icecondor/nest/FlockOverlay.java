package com.icecondor.nest;

import java.util.ArrayList;
import java.util.ListIterator;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

public class FlockOverlay extends ItemizedOverlay<BirdItem> {

	private ArrayList<BirdItem> birds = new ArrayList<BirdItem>();
	private String appTag = "FlockOverlay";
	
	public FlockOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}
	
	public void add(BirdItem bird) {
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
		return true;
	}
	
	public boolean onTap(int index) {
		Log.i(appTag, "got TAP on item#"+index);
		return true;
	}
}
