package edu.teco.bpart.location;

/**
 * This class manages infomation about the location of the user.
 * Created by Florian on 13.03.15.
 *
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

    // Logger Tag
    private static final String TAG = "LocationProvider";

    // Maximal valid time difference between two locations
    private static final long MAX_TIME = 2000;

    // Maximal valid distance between locations
    private static final int MAX_METERS = 10;

    public LocationProvider(Context pContext) {
        mLocationManager = (LocationManager) pContext.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Returns current Latitude or -1.
     *
     * @return
     */
    public double getLatitude() {
        if (mLocation != null) {
            return mLocation.getLatitude();
        } else {
            return -181.0;
        }
    }

    /**
     * Returns current longitude or -1.
     *
     * @return
     */
    public double getLongitude() {
        if (mLocation != null) {
            return mLocation.getLongitude();
        } else {
            return -181.0;
        }
    }

    /**
     * Start listening for location updates.
     */
    public void startListening() {
        try {
            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_TIME, MAX_METERS, this.getLocationListener());
            }
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_TIME, MAX_METERS, this.getLocationListener());
            }
            Log.d(TAG, "Started listening for LocationUpdates");
        } catch (SecurityException ex) {
            Log.e(TAG, "Don't have the permissions to access this resource.");
        } catch (RuntimeException ex) {
            Log.e(TAG, "This context is missing a looper.");
        }
    }

    /**
     * Stop listening for location updates.
     */
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
                public void onLocationChanged(Location pLocation) {
                    if (mLocation == null) {
                        mLocation = pLocation;
                        Log.d(TAG, "Set new location to: " + mLocation.toString());
                    } else {
                        long diff = pLocation.getTime() - mLocation.getTime();
                        if (diff > MAX_TIME || mLocation.getAccuracy() > pLocation.getAccuracy()) {
                            mLocation = pLocation;
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
