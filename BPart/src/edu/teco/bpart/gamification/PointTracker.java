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
 *
 */
public class PointTracker {

	/** 
	 * Number of points which a player receives when
	 * he collects a lux value.
	 */
	private static final int LUX_VALUE_POINTS = 10;
	
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
		return sharedPrefs.getInt("sharedPrefsKeyForPoints", 0);
	}
	
	public static void saveCurrentPoints(int points, Context context) {
		SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(context);
		sf.edit().putInt("sharedPrefsKeyForPoints", points).commit();
	}
		
}
