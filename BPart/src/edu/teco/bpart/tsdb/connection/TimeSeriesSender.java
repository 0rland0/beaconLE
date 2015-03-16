package edu.teco.bpart.tsdb.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import edu.teco.bpart.tsdb.TimeSeries;

/**
 * This class is used to send new values to the server.
 *
 * @author orlando
 * @author Florian Dreschner
 */
public class TimeSeriesSender {

    // Connector to the TSDB server
    private final static TSDBConnector mTSDB = new TSDBConnector("http://cumulus.teco.edu:52001/data/", "");
    // Logger Tag
    private final static String TAG = "TimeSeriesSender";
    // Network change receiver
    private BroadcastReceiver mNetworkStateReceiver;

    private Context mContext;
    // Current online state.
    private boolean mIsOnline = false;

    public TimeSeriesSender(Context pContext) {
        this.mContext = pContext;
        mIsOnline = isOnline();
        mNetworkStateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                mIsOnline = isOnline();
                if (mIsOnline) {
                    mTSDB.processQueue();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetworkStateReceiver, filter);
    }

    /**
     * @param pLux
     * @param pDeviceName
     */
    public void sendLuxToServer(int pLux, String pDeviceName) {
        this.sendLuxToServer(pLux, pDeviceName, -181.0, -181.0);
    }

    /**
     * Create a new time series object and queue it.
     *
     * @param pLux
     * @param pDeviceName
     * @param pLatitude
     * @param pLongitude
     */
    public void sendLuxToServer(int pLux, String pDeviceName, double pLatitude, double pLongitude) {

        TimeSeries<Integer> timeSeries = new TimeSeries<Integer>("mociot.light", pDeviceName);

        // Add some data points to the time series. Timestamp is current time + x.
        long currentTime = System.currentTimeMillis() / 1000;
        timeSeries.addDataPoint(currentTime, pLux);

        // Add two tags to the timeseries.
        // Those key-value pairs can be any string.
        if (pLatitude >= -180.0 && pLongitude >= -180.0) {
            timeSeries.addTag("lat", pLatitude);
            timeSeries.addTag("long", pLongitude);
        }

        // Write all the data to the TSDB.
        try {
            mTSDB.queueTS(timeSeries);
            if (mIsOnline) {
                mTSDB.processQueue();
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not send data to Server.");
        }
    }

    /**
     * Checks if the device is connected to the internet.
     *
     * @return true if connected
     */
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Unregister network change listener
     */
    public void unregisterIntents() {
        mContext.unregisterReceiver(mNetworkStateReceiver);
    }
}
