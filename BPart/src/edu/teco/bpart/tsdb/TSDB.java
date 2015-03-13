package edu.teco.bpart.tsdb;

// Author: Vincent Diener  -  diener@teco.edu

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class represents the TSDB.
 * It is used to write data to and query data from the TSDB.
 */
public class TSDB {

    // The URL for writing data to the TSDB.
    private String mWriteURL;

    // The URL for querying data from the TSDB
    private String mQueryURL;

    // TAG for logger
    private final static String TAG = "TSDB";


    /**
     * Creates the TSDB.
     *
     * @param writeURL The base URL for writing data.
     * @param queryURL The base URL for querying data.
     */
    public TSDB(String writeURL, String queryURL) {
        mWriteURL = writeURL;
        mQueryURL = queryURL;
    }

    /**
     * Write all data points from time series to TSDB.
     *
     * @param timeSeries The time series to write.
     */
    public void write(TimeSeries timeSeries) {
        // Start async task.
        PutIntoTSDBTask tsdbWrite = new PutIntoTSDBTask();
        tsdbWrite.execute(timeSeries);
    }

    /**
     * Query the TSDB with the given query.
     *
     * @param query The query.
     */
    public void query(Query query) {
        QueryTSDBTask tsdbRead = new QueryTSDBTask();
        tsdbRead.execute(query);
    }

    /**
     * Async Task that writes all data points to the TSDB.
     */
    private class PutIntoTSDBTask extends AsyncTask<TimeSeries, String, String> {

        @Override
        protected String doInBackground(TimeSeries... timeSeries) {

            // Get all data points from time series.
            List<DataPoint> dataPoints = timeSeries[0].getDataPoints();

            // Write all the data points.
            for (DataPoint dp : dataPoints) {
                String result = putJsonAndGetResult(timeSeries[0], dp);

                // Call onProgressUpdate with result of last PUT.
                publishProgress(result);
            }

            return "Writing all done.";
        }

        @Override
        protected void onProgressUpdate(String... result) {
            super.onProgressUpdate(result);
            Log.d(TSDB.TAG, "PUT done. Result: " + result[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TSDB.TAG, result);
        }

        private String putJsonAndGetResult(TimeSeries ts, DataPoint dp) {

            String result = "";
            HttpClient httpclient = new DefaultHttpClient();
            InputStream inputStream = null;

            // Put together URL.
            HttpPut httpPUT = new HttpPut(mWriteURL + ts.getDeviceID() + "/");


            // Create JSON object.
            JSONObject main = new JSONObject();
            JSONObject metric = new JSONObject();
            JSONObject tags = new JSONObject();


            try {
                // Data. Contains the data and tags.
                main.put("data", metric);

                // Put metric.
                // The type of data. Can be any string, like "temperature" or "pressure".
                metric.put(ts.getMetric(), tags);

                // Put value.
                // This is needed.
                tags.put("value", dp.getValue());

                // Put tags.
                // Those are optional and can be used to filter the data when placing a query.
                Map mp = ts.getTags();
                Iterator it = mp.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    tags.put(pairs.getKey().toString(), pairs.getValue().toString());
                }

                // Put timestamp.
                // You can use any timestamp. This means you may also upload data from
                // the past or the future.
                main.put("stime", dp.getTimestamp());

                // Put device ID.
                // This is written as value for the tag "resource_id". It is also optional.
                main.put("id", ts.getDeviceID());

            } catch (JSONException e) {
                Log.e(TSDB.TAG, "Malformed JSON.");
            }

            // Get JSON string representation.
            String json;
            json = main.toString();

            // Log URL and JSON.
            Log.d(TSDB.TAG, "Writing: " + json);
            Log.d(TSDB.TAG, "To: " + httpPUT.getURI());

            try {
                // PUT data to server and read response.
                StringEntity se = new StringEntity(json);
                httpPUT.setEntity(se);

                HttpResponse httpResponse = httpclient.execute(httpPUT);
                inputStream = httpResponse.getEntity().getContent();

                if (inputStream != null) {
                    result = convertStreamToString(inputStream);
                } else {
                    result = "Exception while writing.";
                }

            } catch (IOException e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

            // Return result. It is then printed to logcat.
            return result;
        }

    }

    /**
     * Async Task that queries data from the TSDB.
     */
    private class QueryTSDBTask extends AsyncTask<Query, String, String> {


        @Override
        protected String doInBackground(Query... query) {
            // Start query and return the result, a JSON string.
            String result = queryStringOverHttpGet(query[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject response = new JSONObject(result);
            } catch (JSONException ex) {
                Log.d(TSDB.TAG, ex.getMessage());
            }
            Log.d(TSDB.TAG, "Query result: " + result);
        }

        private String queryStringOverHttpGet(Query query) {

            HttpClient httpclient = new DefaultHttpClient();
            InputStream inputStream = null;
            String result = "";

            // Put together URL.
            String URL = mQueryURL + "?";
            URL += "start=" + query.getStart() + "&";
            URL += "end=" + query.getEnd() + "&";
            URL += "m=" + query.getAggregator() + ":";

            // Only add downsampler if it is not null or empty.
            if ((!(query.getDownsampler() != null)) && (!query.getDownsampler().equals("")))
                URL += query.getDownsampler() + ":";

            URL += query.getTimeSeries().getMetric();
            try {
                // Put tags.
                // Those are optional and can be used to filter the data when placing a query.
                URL += URLEncoder.encode("{", "UTF8");

                // Put device tag ("resource_id") if it was set.
                String deviceID = query.getTimeSeries().getDeviceID();
                if (!deviceID.equals(""))
                    URL += URLEncoder.encode("resource_id" + "=" + deviceID, "UTF8");

                Map mp = query.getTimeSeries().getTags();
                Iterator it = mp.entrySet().iterator();

                if (it.hasNext() && !deviceID.equals(""))
                    URL += URLEncoder.encode(",", "UTF8");

                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    URL += URLEncoder.encode(pairs.getKey().toString() + "=" + pairs.getValue().toString(), "UTF8");
                    if (it.hasNext())
                        URL += URLEncoder.encode(",", "UTF8");
                }

                URL += URLEncoder.encode("}", "UTF8");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // The string is done. Print it.
            Log.d(TSDB.TAG, "URL String query: " + URL);

            try {
                // Start HTTP GET request and return data.
                HttpGet httpGET = new HttpGet(URL);
                HttpResponse httpResponse = httpclient.execute(httpGET);
                inputStream = httpResponse.getEntity().getContent();

                if (inputStream != null) {
                    result = convertStreamToString(inputStream);
                } else {
                    result = "Exception while writing.";
                }

            } catch (IOException e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

            // Return the data the query has returned. It should be a JSON string that can now be
            // parsed. TODO: Implement this. Maybe in onPostExecute.
            return result;
        }

    }

    // Needed for logging.
    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
