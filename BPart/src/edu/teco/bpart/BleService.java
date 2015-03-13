package edu.teco.bpart;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import edu.teco.bpart.gamification.PointTracker;
import edu.teco.bpart.location.LocationProvider;
import edu.teco.bpart.tsdb.TimeSeriesSender;


/**
 * A service which runs in the background and scans in a certain interval
 * for bluetooth devices. If a bPart device is found the current lux value is send
 * to the server.
 *
 * @author philip
 */
public class BleService extends Service {

    public static final String BROADCAST_ACTION = "edu.teco.bpart";

    // Tag for logging.
    private static final String TAG = "BleService";

    // Handler needed for starting delayed threads.
    private Handler mHandler;

    // The bluetooth adapter of the device.
    private BluetoothAdapter mBluetoothAdapter;

    // LocationProvider return location of person
    private LocationProvider mLocationProvider;

    // Indicates whether bluetooth is ready to be used on this device.
    private boolean mBluetoothReady;

    // Reference to the context of the service.
    private static Context mContext;

    // Duration of one scan
    private static final int SLICE_SIZE = 5;

    // Multiplier between scan and break
    private int SCAN_BREAK_MULTIPLIER;


    // Gets called by bindService(), which would not start the service.
    // We only use startService, so we can do without binding it first.
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // Gets called when we start the service.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "scanner service started!");
        mContext = this;

        if (mLocationProvider == null) {
            mLocationProvider = new LocationProvider(mContext);
        }
        mLocationProvider.startListening();
        if (intent != null) {
            SCAN_BREAK_MULTIPLIER = intent.getIntExtra("SCAN_BREAK_MULTIPLIER", 5);
        } else {
            SCAN_BREAK_MULTIPLIER = 5;
        }

        // Make sure the service is only started once.
        if (mHandler == null) {
            // Check if BT is ready.
            prepareBluetooth();
            if (mBluetoothReady) {
                Toast.makeText(this, "BLE scanner service started.", Toast.LENGTH_SHORT).show();

                // Wait a bit, then start scan.
                mHandler = new Handler();
                mHandler.postDelayed(startScan, 10);
            }
        }

        // START_STICKY: Auto-restart service if it dies (e.g. if it is killed by
        // the OS due to lack of enough memory.
        // This mode makes sense for things that will be explicitly started and stopped to run
        // for arbitrary periods of time, such as a service performing background music playback.
        // If you want the service to stay dead, use START_NOT_STICKY.
        return START_STICKY;
    }

    // Check if bluetooth is ready to be used.
    private void prepareBluetooth() {
        mBluetoothReady = true;

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getApplicationContext(), "The system has no support for bluetooth LE.", Toast.LENGTH_SHORT).show();
            mBluetoothReady = false;
        }

        BluetoothManager manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device has no bluetooth hardware.", Toast.LENGTH_SHORT).show();
            mBluetoothReady = false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
            mBluetoothReady = false;
        }
    }

    // Start BLE scan
    // Scan for 10 seconds and pause then for 30 seconds
    private Runnable startScan = new Runnable() {
        public void run() {
            Log.d(TAG, "Scanning in progress");
            mBluetoothAdapter.startLeScan(mBtLeScanCallback);
            if (SCAN_BREAK_MULTIPLIER == 0) {
                SCAN_BREAK_MULTIPLIER = 1;
            }
            mHandler.postDelayed(stopScan, SLICE_SIZE * 1000);
            mHandler.postDelayed(startScan, SLICE_SIZE * SCAN_BREAK_MULTIPLIER * 1000 + 10);

        }
    };

    private Runnable stopScan = new Runnable() {
        public void run() {
            mBluetoothAdapter.stopLeScan(mBtLeScanCallback);
        }
    };


    // Callback which is triggered when a LE device was found
    private BluetoothAdapter.LeScanCallback mBtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            // Device discovered. Print info.
            // Then print EIR (Extended Inquiry Response).
            Log.d(TAG, bluetoothDevice.getName() + ", RSSI=" + rssi + ", Extra-Length=" + bytes.length);
            Log.d(TAG, "Extra: " + MathHelper.byteArrayToHex(bytes));

            // bytes[9] to bytes[12] has the Light-Sensor value.
            int lux = java.nio.ByteBuffer.wrap(new byte[]{bytes[12], bytes[11], bytes[10], bytes[9]}).getInt();
            Log.d(TAG, "Light: " + lux + " lux");

            String deviceName = bluetoothDevice.getName();

            if (deviceName != null && deviceName.startsWith("bPart")) {
                TimeSeriesSender.sendLuxToServer(lux, normalizeDeviceName(deviceName), mLocationProvider.getLatitude(), mLocationProvider.getLongitude());
                PointTracker.luxValueCollected(mContext);
                Intent broadcast = new Intent();
                broadcast.setAction(BROADCAST_ACTION);
                broadcast.putExtra("lux", lux);
                sendBroadcast(broadcast);
            }

        }
    };

    private String normalizeDeviceName(String oldName) {
        return oldName.toLowerCase().replaceAll("\\s", "").replace(":", "");
    }

    ;

    // When service dies, stop all threads and scans.
    @Override
    public void onDestroy() {
        Toast.makeText(this, "BLE scanner service stopped.", Toast.LENGTH_SHORT).show();
        mHandler.removeCallbacks(stopScan);
        mHandler.removeCallbacks(startScan);
        mLocationProvider.stopListening();
        mBluetoothAdapter.stopLeScan(mBtLeScanCallback);
    }

}
