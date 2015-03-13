package edu.teco.bpart.location;

/**
 * Created by Florian on 13.03.15.
 */

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;


public class LocationProvider {

    // Always keep best location or null
    private Location mLocation;
    // Need LocationManager to register Listener
    private LocationManager mLocationManager;
    // Listener, which gets called on new Location
    private LocationListener mLocationListener;

    private static final String TAG = "LocationProvider";

    // Maximal valid time difference between two location timestamps
    private static final long MAX_DIFF = 2000;

    public LocationProvider(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public double getLatitude() {
        if (mLocation != null) {
            return mLocation.getLatitude();
        } else {
            return -1.0;
        }
    }

    public double getLongitude() {
        if (mLocation != null) {
            return mLocation.getLongitude();
        } else {
            return -1.0;
        }
    }

    public void startListening() {
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this.getLocationListener());
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.getLocationListener());
        Log.d(TAG, "Started listening for LocationUpdates");
    }

    public void stopListening() {
        if (mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
            Log.d(TAG, "Stopped listening for LocationUpdates");
        }
    }

    private LocationListener getLocationListener() {
        if (this.mLocationListener != null) {
            return this.mLocationListener;
        } else {
            LocationListener locationListener = new LocationListener() {
                // Called when a new location is found by the location provider.
                public void onLocationChanged(Location location) {
                    if (mLocation == null) {
                        mLocation = location;
                        Log.d(TAG, "Set new location to: " + mLocation.toString());
                    } else {
                        long diff = location.getTime() - mLocation.getTime();
                        if (diff > MAX_DIFF || mLocation.getAccuracy() > location.getAccuracy()) {
                            mLocation = location;
                            Log.d(TAG, "Set new location to: " + mLocation.toString());
                        }
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };
            this.mLocationListener = locationListener;
            return locationListener;
        }
    }
}
