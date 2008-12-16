package com.icecondor.nest;

import java.util.ArrayList;
import java.util.ListIterator;

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

	public boolean contains(String guid) {
		ListIterator<BirdItem> list = birds.listIterator();
		while(list.hasNext()) {
			BirdItem bird = list.next();
			if(bird.getTitle().equals(guid))
				return true;
		}
		return false;
	}
}
