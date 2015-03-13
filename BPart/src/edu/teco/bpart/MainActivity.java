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

    private int scanBreakMultiplier = 5;

    // The bluetooth adapter on the device.
    private BluetoothAdapter mBluetoothAdapter;

    private static TextView mPointTextView;
    private static Button mServiceButton;
    private static SeekBar mTimesliceSeekBar;

    private final static int POSITIV_COLOR = Color.rgb(101, 155, 94);
    private final static int NEGATIV_COLOR = Color.rgb(168, 93, 93);


    // Receiver which receives data from the BleService
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePointDisplay();

            if (mContext != null) {
                Toast.makeText(mContext,
                        "Congrats, you collected a lux value!",
                        Toast.LENGTH_LONG).show();
            }
        }
    };
    private int scanAccuracy;

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
        updatePointDisplay();

        // Check for BLE. Quit app if BLE is not supported.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getString(R.string.ble_not_supported),
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
                    getString(R.string.no_bt_on_device), Toast.LENGTH_SHORT)
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
                    scanBreakMultiplier = (100 - progress) / 10;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mTimesliceSeekBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mTimesliceSeekBar.isEnabled()) {
                    Toast.makeText(getApplicationContext(),
                            "Please stop scanning, before you change this.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });


    }

    private void displayBleServiceIsAlreadyRunningUI() {
        mTimesliceSeekBar.setEnabled(false);
        mServiceButton.setBackgroundColor(NEGATIV_COLOR);
        mServiceButton.setText(R.string.stop_service);
    }

    private void displayBleServiceIsNotRunningUI() {
        mTimesliceSeekBar.setEnabled(true);
        mServiceButton.setBackgroundColor(POSITIV_COLOR);
        mServiceButton.setText(R.string.start_service);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.BROADCAST_ACTION);
        registerReceiver(receiver, filter);

        // updates points if necessary
        updatePointDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    // Update Point View to actually value
    private void updatePointDisplay() {
        int points = PointTracker.getCurrentPoints(this);
        mPointTextView.setText(String.valueOf(points));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // The user has returned from the Enable-BT-Intent. Check the result.
        if (requestCode == ENABLE_BT_ACTION && resultCode == RESULT_OK) {
            Toast.makeText(getApplicationContext(),
                    "Thank you for activating Bluetooth.", Toast.LENGTH_SHORT)
                    .show();
        } else if (requestCode == ENABLE_BT_ACTION
                && resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(),
                    "You did not activate Bluetooth. =(", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void serviceBtnClicked(View v) {
        boolean bleScanServiceIsRunning = checkIfServiceIsRunning(BleService.class);
        Intent newIntent = new Intent(this, BleService.class);
        if (bleScanServiceIsRunning) {
            stopService(new Intent(this, BleService.class));
            displayBleServiceIsNotRunningUI();
        } else {
            newIntent.putExtra("SCAN_BREAK_MULTIPLIER", scanBreakMultiplier);
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
