package edu.teco.bpart.tsdb.connection;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.teco.bpart.tsdb.DataPoint;
import edu.teco.bpart.tsdb.Query;
import edu.teco.bpart.tsdb.TimeSeries;

/**
 * This class represents the TSDB.
 * It is used to write data to and query data from the TSDB.
 *
 * @author Vincent Diener
 * @author Florian Dreschner
 */


public class TSDBConnector implements AsyncResponse {

    // The URL for writing data to the TSDB.
    private String mWriteURL;

    // The URL for querying data from the TSDB
    private String mQueryURL;

    // TAG for logger
    private final static String TAG = "TSDBCONNECTOR";

    // Queue of collected time series
    private final BlockingQueue<TimeSeries> mSendQueue = new LinkedBlockingQueue<TimeSeries>();

    /**
     * Creates the Connector.
     *
     * @param pWriteURL The base URL for writing data.
     * @param pQueryURL The base URL for querying data.
     */
    public TSDBConnector(String pWriteURL, String pQueryURL) {
        mWriteURL = pWriteURL;
        mQueryURL = pQueryURL;
    }

    /**
     * Queue new time series.
     *
     * @param pTimeSeries
     */
    public void queueTS(TimeSeries pTimeSeries) {
        try {
            mSendQueue.put(pTimeSeries);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void startAsyncTask(TimeSeries pTimeSeries) {
        // Start async task.
        PutIntoTSDBTask tsdbWrite = new PutIntoTSDBTask();
        tsdbWrite.delegate = this;
        tsdbWrite.execute(pTimeSeries);
    }

    /**
     * Get result of asyncron task. If it's not null, an error occured.
     *
     * @param pOutput
     */
    public void processFinish(TimeSeries pOutput) {
        if (pOutput != null) {
            try {
                mSendQueue.put(pOutput);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    /**
     * Process content of queue.
     */
    public void processQueue() {
        try {
            while (!mSendQueue.isEmpty()) {
                startAsyncTask(mSendQueue.take());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Query the TSDB with the given query.
     *
     * @param pQuery The query.
     */
    public void query(Query pQuery) {
        QueryTSDBTask tsdbRead = new QueryTSDBTask();
        tsdbRead.execute(pQuery);
    }

    /**
     * Async Task that writes all data points to the TSDB.
     */


    private class PutIntoTSDBTask extends AsyncTask<TimeSeries, AsyncTaskResult<HttpResponse>, TimeSeries> {

        public AsyncResponse delegate = null;

        @Override
        protected TimeSeries doInBackground(TimeSeries... timeSeries) {

            TimeSeries current = timeSeries[0];
            // Get all data points from time series.
            List<DataPoint> dataPoints = current.getDataPoints();

            List<DataPoint> retryList = new ArrayList<DataPoint>();
            // Write all the data points.
            for (DataPoint dp : dataPoints) {
                AsyncTaskResult<HttpResponse> result = putJsonAndGetResult(timeSeries[0], dp);

                // Call onProgressUpdate with result of last PUT.
                publishProgress(result);
                if (result.getError() != null || result.getResult().getStatusLine().getStatusCode() != 200) {
                    retryList.add(dp);
                }
            }
            if (retryList.isEmpty()) {
                return null;
            } else {
                current.setDataPoints(retryList);
                return current;
            }
        }

        @Override
        protected void onProgressUpdate(AsyncTaskResult<HttpResponse>... pResult) {
            super.onProgressUpdate(pResult);

            for (AsyncTaskResult<HttpResponse> elem : pResult) {
                if (elem.getError() == null && elem.getResult() != null) {
                    HttpResponse response = elem.getResult();
                    Log.d(TSDBConnector.TAG, "PUT is done. Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    try {
                        String msg = elem.getError().getLocalizedMessage();
                        Log.e(TSDBConnector.TAG, "Couldn't PUT timeserie. error: " + msg);
                    } catch (NullPointerException e) {
                        Log.e(TSDBConnector.TAG, "Couldn't PUT timeserie");
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(TimeSeries pResult) {
            delegate.processFinish(pResult);
            Log.d(TSDBConnector.TAG, "Finished PUT Task.");
        }

        private AsyncTaskResult<HttpResponse> putJsonAndGetResult(TimeSeries pTimeSeries, DataPoint pDatapoint) {

            HttpClient httpclient = new DefaultHttpClient();
            InputStream inputStream;

            // Put together URL.
            HttpPut httpPUT = new HttpPut(mWriteURL + pTimeSeries.getDeviceID() + "/");


            // Create JSON object.
            JSONObject main = new JSONObject();
            JSONObject metric = new JSONObject();
            JSONObject tags = new JSONObject();


            try {
                // Data. Contains the data and tags.
                main.put("data", metric);

                // Put metric.
                // The type of data. Can be any string, like "temperature" or "pressure".
                metric.put(pTimeSeries.getMetric(), tags);

                // Put value.
                // This is needed.
                tags.put("value", pDatapoint.getValue());

                // Put tags.
                // Those are optional and can be used to filter the data when placing a query.
                Map mp = pTimeSeries.getTags();
                for (Object fpairs : mp.entrySet()) {
                    Map.Entry pairs = (Map.Entry) fpairs;
                    tags.put(pairs.getKey().toString(), pairs.getValue().toString());
                }

                // Put timestamp.
                // You can use any timestamp. This means you may also upload data from
                // the past or the future.
                main.put("stime", pDatapoint.getTimestamp());

                // Put device ID.
                // This is written as value for the tag "resource_id". It is also optional.
                main.put("id", pTimeSeries.getDeviceID());

            } catch (JSONException e) {
                Log.e(TSDBConnector.TAG, "Malformed JSON.");
                return new AsyncTaskResult<HttpResponse>(e);
            }

            // Get JSON string representation.
            String json;
            json = main.toString();

            // Log URL and JSON.
            Log.d(TSDBConnector.TAG, "Writing: " + json);
            Log.d(TSDBConnector.TAG, "To: " + httpPUT.getURI());
            HttpResponse httpResponse;
            try {
                // PUT data to server and read response.
                StringEntity se = new StringEntity(json);
                httpPUT.setEntity(se);

                httpResponse = httpclient.execute(httpPUT);
                inputStream = httpResponse.getEntity().getContent();
            } catch (IOException e) {
                Log.d("InputStream", e.getLocalizedMessage());
                return new AsyncTaskResult<HttpResponse>(e);
            }
            // Return result. It is then printed to logcat.
            return new AsyncTaskResult<HttpResponse>(httpResponse);
        }
    }

    /**
     * Async Task that queries data from the TSDB.
     */
    private class QueryTSDBTask extends AsyncTask<Query, String, String> {


        @Override
        protected String doInBackground(Query... pQuery) {
            // Start query and return the result, a JSON string.
            String result = queryStringOverHttpGet(pQuery[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String pResult) {
            try {
                JSONObject response = new JSONObject(pResult);
            } catch (JSONException ex) {
                Log.d(TSDBConnector.TAG, ex.getMessage());
            }
            Log.d(TSDBConnector.TAG, "Query result: " + pResult);
        }

        private String queryStringOverHttpGet(Query pQuery) {

            HttpClient httpclient = new DefaultHttpClient();
            InputStream inputStream;
            String result = "";

            // Put together URL.
            String URL = mQueryURL + "?";
            URL += "start=" + pQuery.getStart() + "&";
            URL += "end=" + pQuery.getEnd() + "&";
            URL += "m=" + pQuery.getAggregator() + ":";

            // Only add downsampler if it is not null or empty.
            if ((!(pQuery.getDownsampler() != null)) && (!pQuery.getDownsampler().equals("")))
                URL += pQuery.getDownsampler() + ":";

            URL += pQuery.getTimeSeries().getMetric();
            try {
                // Put tags.
                // Those are optional and can be used to filter the data when placing a query.
                URL += URLEncoder.encode("{", "UTF8");

                // Put device tag ("resource_id") if it was set.
                String deviceID = pQuery.getTimeSeries().getDeviceID();
                if (!deviceID.equals(""))
                    URL += URLEncoder.encode("resource_id" + "=" + deviceID, "UTF8");

                Map mp = pQuery.getTimeSeries().getTags();
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
            Log.d(TSDBConnector.TAG, "URL String query: " + URL);

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

    /**
     * This class represents a result of one AsyncTask.
     *
     * @param <T>
     */
    public class AsyncTaskResult<T> {
        private T result;
        private Exception error;

        public T getResult() {
            return result;
        }

        public Exception getError() {
            return error;
        }


        public AsyncTaskResult(T pResult) {
            super();
            this.result = pResult;
        }


        public AsyncTaskResult(Exception pError) {
            super();
            this.error = pError;
        }
    }

    // Needed for logging.
    private String convertStreamToString(InputStream pInputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pInputStream));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                pInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
