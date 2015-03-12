package edu.teco.bpart.gamification;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.TextView;

/**
 * This class takes care of adding and tracking points.
 * For each sensor data which is collected by the user
 * he can earn points. With this gamification element
 * the users are incentivized to collect the data as
 * often as possible which leads to more up-to-date data.
 * 
 * @author orlando
 *
 */
public class PointTracker {
	
	/** 
	 * Number of points which a player receives when
	 * user collects a lux value.
	 */
	private static final int LUX_VALUE_POINTS = 10;
	
	private static final String SHARED_PREFS_KEY_FOR_POINTS = "sharedPrefsKeyForPoints";
	
	/**
	 * This method should be called when
	 * a lux value is collected. The points
	 * of the player will increased and saved
	 * to the shared preferences.
	 */
	public static void luxValueCollected(Context context) {			
		
		int points = getCurrentPoints(context);
		points += LUX_VALUE_POINTS;
		saveCurrentPoints(points, context);		
	}
	
	public static int getCurrentPoints(Context context) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPrefs.getInt(SHARED_PREFS_KEY_FOR_POINTS, 0);
	}
	
	private static void saveCurrentPoints(int points, Context context) {
		SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(context);
		sf.edit().putInt(SHARED_PREFS_KEY_FOR_POINTS, points).commit();
	}		
}
