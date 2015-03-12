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
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.edu.teco.bpart.R;

import edu.teco.bpart.gamification.PointTracker;

public class MainActivity extends ActionBarActivity {

	private Context mContext = null;

	// Constant for identifying intent request
	private static int ENABLE_BT_ACTION = 0;

	// The bluetooth adapter on the device.
	private BluetoothAdapter mBluetoothAdapter;

	private static TextView mPointTextView;
	private static Button mServiceButton;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;
		mServiceButton = (Button) findViewById(R.id.btn_service);

		if (checkIfServiceIsRunning(BleService.class)) {
		
			displayBleServiceIsAlreadyRunningUI();
			
		} else {
	
			displayBleServiceIsNotRunningUI();
		}

		// displays the current number of points
		mPointTextView = (TextView) findViewById(R.id.pointTxtView);
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
	}

	private void displayBleServiceIsAlreadyRunningUI() {
		mServiceButton.setText(R.string.stop_service);
	}

	private void displayBleServiceIsNotRunningUI() {
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
		if (bleScanServiceIsRunning) {
			stopService(new Intent(this, BleService.class));
			displayBleServiceIsNotRunningUI();

		} else {
			startService(new Intent(this, BleService.class));
			displayBleServiceIsAlreadyRunningUI();
		}

	}

	/**
	 * Checks if the given service is running.
	 * 
	 * @param serviceClass
	 *            The class of your service which you want to check.
	 * @return Returns <code>true</code> if the given service is running or
	 *         <code>false</code> otherwise.
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
