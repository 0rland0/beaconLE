package edu.teco.bpart.tsdb;

/**
 * 
 * This class sends time series to the server.
 * 
 * @author orlando
 *
 */
public class TimeSeriesSender {
	
	private static TSDB tsdb;

	// TODO @flo please comment the parameters and test this method
	public static void sendLuxToServer(int lux, String deviceName) {

        TimeSeries<Integer> timeSeries = new TimeSeries<Integer>("LUX", deviceName);

        // Add some data points to the time series. Timestamp is current time + x.
        long currentTime = System.currentTimeMillis() / 1000;
        timeSeries.addDataPoint(currentTime, lux);

        // Add two tags to the timeseries.
        // Those key-value pairs can be any string.
        timeSeries.addTag("Debug", "1");
        //timeSeries.addTag("SomeOtherTag", "SomeOtherValue");

        // Write all the data to the TSDB.
        tsdb.write(timeSeries);

    }
	
}
