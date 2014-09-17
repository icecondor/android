package com.icecondor.eaglet.service;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.db.Point;

public class CellReceiver implements LocationListener {
    private final Condor condor;

    public CellReceiver(Condor condor) {
        this.condor = condor;
    }

    @Override
    public void onLocationChanged(Location location) {
        // distinguish cell and wifi points based on accuracy
        condor.onLocationChanged(new Point(location));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
