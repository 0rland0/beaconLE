package edu.teco.bpart;

import edu.teco.bpart.tsdb.TimeSeriesSender;
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


/**
 * A service which runs in the background and scans in a certain interval
 * for bluetooth devices. If a bPart device is found the current lux value is send
 * to the server.
 * 
 * @author philip
 *
 */
public class BleService extends Service {

    // Tag for logging.
    private static final String TAG = "BleService";

    // Handler needed for starting delayed threads.
    private Handler mHandler;

    // The bluetooth adapter of the device.
    private BluetoothAdapter mBluetoothAdapter;

    // Indicates whether bluetooth is ready to be used on this device.
    private boolean mBluetoothReady;


    // Gets called by bindService(), which would not start the service.
    // We only use startService, so we can do without binding it first.
    @Override
    public IBinder onBind(Intent intent) { return null; }



    // Gets called when we start the service.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Make sure the service is only started once.
        if (mHandler == null) {
            // Check if BT is ready.
            prepareBluetooth();
            if (mBluetoothReady) {
                Toast.makeText(this, "BLE scanner service started.", Toast.LENGTH_SHORT).show();

                // Wait a bit, then start scan.
                mHandler = new Handler();
                mHandler.postDelayed(startScan, 3000);
            }
        }

        // START_STICKY: Auto-restart service if it dies.
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
    // Then make sure it gets stopped in 10 seconds.
    // Then make sure it gets started again in 30 seconds.
    private Runnable startScan = new Runnable()
    {
        public void run()
        {
        	Log.d(TAG, "Scanning in progress");
            mBluetoothAdapter.startLeScan(mBtLeScanCallback);

            mHandler.postDelayed(stopScan, 10 * 1000);
            mHandler.postDelayed(startScan, 30 * 1000);
        }
    };
    private Runnable stopScan = new Runnable()
    {
        public void run()
        {
            mBluetoothAdapter.stopLeScan(mBtLeScanCallback);
        }
    };


    // Device discovered.
    private BluetoothAdapter.LeScanCallback mBtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
//            customToast("Bluetooth Low Energy device \"" + bluetoothDevice.getName() + "\" found.");
        	
        	
            // Device discovered. Print info.
            // Then print EIR (Extended Inquiry Response).
            Log.d(TAG, bluetoothDevice.getName() + ", RSSI=" + rssi + ", Extra-Length=" + bytes.length);
            Log.d(TAG, "Extra: " + MathHelper.byteArrayToHex(bytes));

            // bytes[9] to bytes[12] has the Light-Sensor value.
            int lux = java.nio.ByteBuffer.wrap(new byte[]{bytes[12],bytes[11],bytes[10],bytes[9]}).getInt();
            Log.d(TAG, "Light: " + lux + " lux");
            
            String deviceName = bluetoothDevice.getName();
            
            // TODO test
            if (deviceName != null && deviceName.startsWith("bPart")); {
            	TimeSeriesSender.sendLuxToServer(lux, deviceName);
            }
//        	Log.d(TAG, "Bluetooth callback was triggered");
//        	if (null != bluetoothDevice.getName()) {
//        		Log.d("BleService", "ble device found. name = " + bluetoothDevice.getName());
//        	}
        }
    };

//    // Starts custom toast as defined in toast_layout.xml
//    private void customToast(final String textString) {
//        Runnable r = new Runnable() {
//            @Override
//            public void run() {
//                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//                View layout = inflater.inflate(R.layout.toast_layout, null);
//
//                TextView text = (TextView) layout.findViewById(R.id.text);
//                text.setText(textString);
//
//                Toast toast = new Toast(getApplicationContext());
//                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
//                toast.setDuration(Toast.LENGTH_LONG);
//                toast.setView(layout);
//                toast.show();
//            }
//        };
//        mHandler.post(r);
//    }

    // When service dies, stop all threads and scans.
    @Override
    public void onDestroy() {
        Toast.makeText(this, "BLE scanner service stopped.", Toast.LENGTH_SHORT).show();
        mHandler.removeCallbacks(stopScan);
        mHandler.removeCallbacks(startScan);

        mBluetoothAdapter.stopLeScan(mBtLeScanCallback);
    }

}
