package edu.teco.bpart;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class helps to handle shared preferences.
 *
 * @author Florian
 */
public class PreferencesHelper {

    /**
     * Returns integer of shared preferences value for key.
     *
     * @param pKey
     * @param pContext
     * @return
     */
    public static int getIntPrefForKey(final String pKey, Context pContext) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(pContext);
        return sharedPrefs.getInt(pKey, 0);
    }

    /**
     * Sets int value in shared preferences
     *
     * @param pKey
     * @param pValue
     * @param pContext
     */
    public static void setIntPrefForKey(final String pKey, final int pValue, Context pContext) {
        SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(pContext);
        sf.edit().putInt(pKey, pValue).commit();
    }
}
