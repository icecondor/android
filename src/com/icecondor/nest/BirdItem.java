package com.icecondor.nest;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class BirdItem extends OverlayItem {

	public BirdItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
	}

}
