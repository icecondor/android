package com.icecondor.hawk.service;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.icecondor.hawk.Condor;
import com.icecondor.hawk.db.Point;

public class GpsReceiver implements LocationListener {
    private final Condor condor;

    public GpsReceiver(Condor condor) {
        this.condor = condor;
    }

    @Override
    public void onLocationChanged(Location location) {
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
