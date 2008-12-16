package com.icecondor.nest;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;

public class FlockOverlay extends ItemizedOverlay<BirdItem> {

	private ArrayList<BirdItem> birds = new ArrayList<BirdItem>();
	
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

}
