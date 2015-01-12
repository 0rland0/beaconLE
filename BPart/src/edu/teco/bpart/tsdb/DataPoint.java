package edu.teco.bpart.tsdb;

// Author: Vincent Diener  -  diener@teco.edu

/**
 * A single data point in a time series.
 */
public class DataPoint<T> {

    // The value of the data point.
    private String mValue;

    // The timestamp of the data point.
    private long mTimestamp;

    /**
     * Create a data point.
     * @param timestamp The timestamp.
     * @param value The value.
     */
    public DataPoint(long timestamp, T value) {
        mValue = value.toString();
        mTimestamp = timestamp;
    }

    /**
     * Getter for timestamp.
     * @return timestamp of this data point.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Getter for value.
     * @return value of this timestamp.
     */
    public String getValue() {
        return mValue;
    }
}
