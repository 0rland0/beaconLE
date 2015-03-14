package edu.teco.bpart.gamification;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class takes care of adding and tracking points.
 * For each sensor data which is collected by the user
 * he can earn points. With this gamification element
 * the users are incentivized to collect the data as
 * often as possible which leads to more up-to-date data.
 *
 * @author orlando
 */
public class PointTracker {

    /**
     * Number of points which a player receives when
     * user collects a lux value.
     */
    private static final int LUX_VALUE_POINTS = 10;

    /**
     * Shared Key for Points
     */
    private static final String SHARED_PREFS_KEY_FOR_POINTS = "sharedPrefsKeyForPoints";

    /**
     * This method should be called when
     * a lux value is collected. The points
     * of the player will increased and saved
     * to the shared preferences.
     */
    public static void luxValueCollected(Context pContext) {

        int points = getCurrentPoints(pContext);
        points += LUX_VALUE_POINTS;
        saveCurrentPoints(points, pContext);
    }

    /**
     * Returns current points.
     *
     * @param pContext
     * @return
     */

    public static int getCurrentPoints(Context pContext) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(pContext);
        return sharedPrefs.getInt(SHARED_PREFS_KEY_FOR_POINTS, 0);
    }

    /**
     * Sets amount of points.
     *
     * @param pPoints
     * @param pContext
     */
    private static void saveCurrentPoints(int pPoints, Context pContext) {
        SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(pContext);
        sf.edit().putInt(SHARED_PREFS_KEY_FOR_POINTS, pPoints).commit();
    }
}
