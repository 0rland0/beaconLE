package edu.teco.bpart;


import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.edu.teco.bpart.R;

import edu.teco.bpart.gamification.PointTracker;

public class MainActivity extends ActionBarActivity {

    private Context mContext = null;

    // Constant for identifying intent request
    private static int ENABLE_BT_ACTION = 0;

    // Default scan break multiplier
    private int mScanBreakMultiplier = 5;

    // The bluetooth adapter on the device.
    private BluetoothAdapter mBluetoothAdapter;

    private static TextView mPointTextView;
    private static Button mServiceButton;
    private static SeekBar mTimesliceSeekBar;

    private final static String SHARED_PREFS_KEY_FOR_SCAN = "sharedPrefsKeyForScanBreakMultiplier";
    // Positiv color of the start button
    private final static int POSITIV_COLOR = Color.rgb(101, 155, 94);
    // Negativ color of the stop button
    private final static int NEGATIV_COLOR = Color.rgb(168, 93, 93);


    // Receiver which receives data from the BleService
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateView();

            if (mContext != null) {
                Toast.makeText(mContext,
                        R.string.congrats_lux,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        mServiceButton = (Button) findViewById(R.id.btn_service);
        mTimesliceSeekBar = (SeekBar) findViewById(R.id.time_slices);
        mPointTextView = (TextView) findViewById(R.id.pointTxtView);


        if (checkIfServiceIsRunning(BleService.class)) {
            displayBleServiceIsAlreadyRunningUI();
        } else {
            displayBleServiceIsNotRunningUI();
        }

        // displays the current number of points
        updateView();

        // Check for BLE. Quit app if BLE is not supported.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // Get bluetooth manager.
        BluetoothManager manager = (BluetoothManager) this
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        // Quit if bluetooth is not available on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    R.string.no_bt_on_device, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        // Check if bluetooth is enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            // If not, start intent to enable it.
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, ENABLE_BT_ACTION); // 0
            // identifies
            // the
            // call.
        }

        mTimesliceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mScanBreakMultiplier = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void displayBleServiceIsAlreadyRunningUI() {
        mTimesliceSeekBar.setEnabled(false);
        mServiceButton.setBackgroundColor(NEGATIV_COLOR);
        mServiceButton.setText(R.string.stop_helping);
    }

    private void displayBleServiceIsNotRunningUI() {
        mTimesliceSeekBar.setEnabled(true);
        mServiceButton.setBackgroundColor(POSITIV_COLOR);
        mServiceButton.setText(R.string.start_helping);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.BROADCAST_ACTION);
        registerReceiver(receiver, filter);
        mScanBreakMultiplier = PreferencesHelper.getIntPrefForKey(SHARED_PREFS_KEY_FOR_SCAN, mContext);
        // updates points if necessary
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferencesHelper.setIntPrefForKey(SHARED_PREFS_KEY_FOR_SCAN, mScanBreakMultiplier, mContext);
        unregisterReceiver(receiver);
    }

    // Update Point View to actually value
    private void updateView() {
        int points = PointTracker.getCurrentPoints(this);
        mPointTextView.setText(String.valueOf(points));
        mTimesliceSeekBar.setProgress(mScanBreakMultiplier);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user has returned from the Enable-BT-Intent. Check the result.
        if (requestCode == ENABLE_BT_ACTION && resultCode == RESULT_OK) {
            Toast.makeText(getApplicationContext(),
                    R.string.activated_bluetooth, Toast.LENGTH_SHORT)
                    .show();
        } else if (requestCode == ENABLE_BT_ACTION
                && resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(),
                    R.string.not_activated, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Callback for button.
     *
     * @param v
     */
    public void serviceBtnClicked(View v) {
        boolean bleScanServiceIsRunning = checkIfServiceIsRunning(BleService.class);
        Intent newIntent = new Intent(this, BleService.class);
        if (bleScanServiceIsRunning) {
            stopService(new Intent(this, BleService.class));
            displayBleServiceIsNotRunningUI();
        } else {
            newIntent.putExtra("SCAN_BREAK_MULTIPLIER", mScanBreakMultiplier);
            startService(newIntent);
            displayBleServiceIsAlreadyRunningUI();
        }

    }

    /**
     * Checks if the given service is running.
     *
     * @param serviceClass The class of your service which you want to check.
     * @return Returns <code>true</code> if the given service is running or
     * <code>false</code> otherwise.
     */
    private boolean checkIfServiceIsRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
