package edu.teco.bpart.tsdb;

import android.util.Log;

/**
 * This class sends time series to the server.
 *
 * @author orlando
 */
public class TimeSeriesSender {

    private static TSDB tsdb = new TSDB("http://cumulus.teco.edu:52001/data/", "");

    private final static String TAG = "TimeSeriesSender";

    public static void sendLuxToServer(int lux, String deviceName, double latitude, double longitude) {

        TimeSeries<Integer> timeSeries = new TimeSeries<Integer>("mociot.light", deviceName);

        // Add some data points to the time series. Timestamp is current time + x.
        long currentTime = System.currentTimeMillis() / 1000;
        timeSeries.addDataPoint(currentTime, lux);

        // Add two tags to the timeseries.
        // Those key-value pairs can be any string.
        if (latitude > 0 && longitude > 0) {
            timeSeries.addTag("lat", String.valueOf(latitude));
            timeSeries.addTag("long", String.valueOf(longitude));
        }

        // Write all the data to the TSDB.
        try {
            tsdb.write(timeSeries);
        } catch (Exception e) {
            Log.e(TAG, "Could not send data to Server.");
        }
    }
}
