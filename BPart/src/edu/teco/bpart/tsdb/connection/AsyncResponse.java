package edu.teco.bpart.tsdb.connection;

import edu.teco.bpart.tsdb.TimeSeries;

/**
 * This interface is used to get the result of an asyncron task.
 *
 * @author Florian Dreschner
 */

interface AsyncResponse {
    /**
     * Returns result of task.
     *
     * @param pOutput result of task
     */
    void processFinish(TimeSeries pOutput);
}