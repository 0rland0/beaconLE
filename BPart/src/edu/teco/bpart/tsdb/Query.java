package edu.teco.bpart.tsdb;

// Author: Vincent Diener  -  diener@teco.edu

/**
 * A query to the TSDB.
 */
public class Query {
    // The timeseries used to store the result of the query.
    // The timeseries may also already have tags set. If that is the case, they will be included in
    // the query to the time series database.
    private TimeSeries mTimeSeries;

    // Start and end can be timestamps or strings like "10m-ago", "3d-ago", "53s-ago", etc.
    private String mStart;
    private String mEnd;

    // Aggregator: What to do with two data points if they have the same timestamp?
    // Values include: "sum", "avg", "min", "max", ...
    private String mAggregator;

    // Downsampler: If you have alot of data, you might want the TSDB to only return one point
    // for every 5 seconds that is the average of all points in that timeslot.
    // The downsampler lets you do exactly that.
    // Values include: "5s-avg", "1d-sum", "23m-min", "9s-max", ...
    // Downsampler is optional.
    private String mDownsampler;

    /**
     * Creates new query.
     * @param timeSeries The time series in which the results are stored. If tags were added to the time
     *                   series before the query was started, the query will only return results with
     *                   the wanted tag values.
     * @param start The start time.
     * @param end The end time.
     * @param aggregator The aggregator function for data points with the same timestamp.
     * @param downsampler The downsampler.
     */
    public Query(TimeSeries timeSeries, String start, String end, String aggregator, String downsampler) {
        mTimeSeries = timeSeries;
        mStart = start;
        mEnd = end;
        mAggregator = aggregator;

        if (downsampler == null)
            mDownsampler = "";
        else
            mDownsampler = downsampler;
    }

    /**
     * Getter for timeseries. After the query is done, this will hold the results.
     * @return the timeseries.
     */
    public TimeSeries getTimeSeries() {
        return mTimeSeries;
    }

    /**
     * Getter for start time.
     * @return the start time.
     */
    public String getStart() {
        return mStart;
    }

    /**
     * Getter for end time.
     * @return the end time.
     */
    public String getEnd() {
        return mEnd;
    }

    /**
     * Getter for aggregator.
     * @return the aggregator.
     */
    public String getAggregator() {
        return mAggregator;
    }

    /**
     * Getter for downsampler.
     * @return the downsampler.
     */
    public String getDownsampler() {
        return mDownsampler;
    }
}
