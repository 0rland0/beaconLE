package edu.teco.bpart.tsdb;

// Author: Vincent Diener  -  diener@teco.edu

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a time series.
 * @param <T> The type of the data in the series. Data will be saved as key-value pairs, where the
 *            key is a string and the value is a T.
 */
public class TimeSeries<T> {

    // The metric. Name of the data that is being stored.
    private String mMetric;

    // The ID of the device that recorded the data. May be null or empty if the data comes from
    // multiple devices.
    private String mDeviceID;

    // The tag list. Tags are added to all data points in the time series. When querying the TSDB,
    // you can filter the data for those tags.
    private Map<String, String> mTags;

    // The list of all data points in the time series.
    private List<DataPoint> mDataPoints;

    /**
     * Creates a new time series.
     * @param metric The metric. Name of the data that is being stored.
     * @param deviceID The ID of the device that recorded the data. May be null or empty if the
     *                 data comes from multiple devices.
     */
    public TimeSeries(String metric, String deviceID) {
        mMetric = metric;

        if (deviceID != null && !(deviceID.equals("")))
            mDeviceID = deviceID;
        else
            mDeviceID = "";

        mTags = new HashMap<String, String>();
        mDataPoints = new ArrayList<DataPoint>();
    }

    /**
     * Add data point to time series.
     * @param timestamp The timestamp of the new data point.
     * @param value The value of the new data point.
     */
    public void addDataPoint(long timestamp, T value) {
        DataPoint<T> newDataPoint = new DataPoint<T>(timestamp, value);
        mDataPoints.add(newDataPoint);
    }

    /**
     * Add data point to time series. Timestamp is "now".
     * @param value The value of the new data point.
     */
    public void addDataPoint(T value) {
        long timestamp = System.currentTimeMillis() / 1000;
        DataPoint<T> newDataPoint = new DataPoint<T>(timestamp, value);
        mDataPoints.add(newDataPoint);
    }


    /**
     * Put key-value-pair into the tag list for the time series.
     * Uses toString of value to get the string value for storage.
     * @param key The key.
     * @param value The value.
     */
    public void addTag(String key, Object value) {
        String strValue = value.toString();
        mTags.put(key, strValue);
    }

    // Helpers for handling basic types (non-object).
    /**
     * Put key-value-pair into the tag list for the time series.
     * @param key The key.
     * @param value The integer value.
     */
    public void addTag(String key, int value) {
        addTag(key, Integer.valueOf(value));
    }

    /**
     * Put key-value-pair into the tag list for the time series.
     * @param key The key.
     * @param value The long value.
     */
    public void addTag(String key, long value) {
        addTag(key, Long.valueOf(value));
    }

    /**
     * Put key-value-pair into the tag list for the time series.
     * @param key The key.
     * @param value The float value.
     */
    public void addTag(String key, float value) {
        addTag(key, Float.valueOf(value));
    }

    /**
     * Put key-value-pair into the tag list for the time series.
     * @param key The key.
     * @param value The double value.
     */
    public void addTag(String key, double value) {
        addTag(key, Double.valueOf(value));
    }

    /**
     * Put key-value-pair into the tag list for the time series.
     * @param key The key.
     * @param value The boolean value.
     */
    public void addTag(String key, boolean value) {
        addTag(key, Boolean.valueOf(value));
    }

    /**
     * Getter for the metric of the time series.
     * @return the metric of the time series.
     */
    public String getMetric() {
        return mMetric;
    }

    /**
     * Getter for the device ID of the time series.
     * @return the device ID of the time series.
     */
    public String getDeviceID() {
        return mDeviceID;
    }

    /**
     * Getter for the data points of the time series.
     * @return the data point list of the time series.
     */
    public List<DataPoint> getDataPoints() {
        return mDataPoints;
    }

    /**
     * Getter for the tags of the time series.
     * @return the tag hash map of the time series.
     */
    public Map<String, String> getTags() {
        return mTags;
    }
}
