package com.edu.teco.bpart;

import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import java.lang.Integer;
import java.util.List;
import java.util.UUID;

public class MainActivity extends ActionBarActivity {


    // Please note: This code barely has any error handling.
    // How you implement this is up to you.

		private Context mContext = null;

        // App name for logging.
        private static String APP = "Bluetooth Low Energy Demo";

        // Constant for identifying intent request
        private static int ENABLE_BT_ACTION = 0;

        // The bluetooth adapter on the device.
        private BluetoothAdapter mBluetoothAdapter;

        // The bPart itself.
        private BluetoothDevice mbPart;

        // GATT server we connect to.
        private BluetoothGatt mBluetoothGatt;

        // UUIDs for bPart Light characteristic, LED characteristic and notification enabling.
        protected static final UUID UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        protected static final UUID BPART_LIGHT_UUID = UUID.fromString("4b822f01-3941-4a4b-a3cc-b2602ffe0d00");
        protected static final UUID BPART_LED_UUID = UUID.fromString("4b822ff1-3941-4a4b-a3cc-b2602ffe0d00");


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            
            mContext = this;
            
            // Check for BLE. Quit app if BLE is not supported.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_SHORT).show();
                finish();
            }

            // Get bluetooth manager.
            BluetoothManager manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = manager.getAdapter();

            // Quit if bluetooth is not available on the device.
            if (mBluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.no_bt_on_device), Toast.LENGTH_SHORT).show();
                finish();
            }

            // Check if bluetooth is enabled.
            if (!mBluetoothAdapter.isEnabled()) {
                // If not, start intent to enable it.
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBtIntent, ENABLE_BT_ACTION); // 0 identifies the call.
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {

            // The user has returned from the Enable-BT-Intent. Check the result.
            if (requestCode == ENABLE_BT_ACTION && resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Thank you for activating Bluetooth.", Toast.LENGTH_SHORT).show();
            } else if (requestCode == ENABLE_BT_ACTION && resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "You did not activate Bluetooth. =(", Toast.LENGTH_SHORT).show();
            }
        }

        public void scanClicked(View v) {
            if (mBluetoothAdapter.isEnabled()) {
                // Bluetooth ready to go.
                // Before we start the scan, make sure we stop it after a certain period of time.
//                Handler handler = new Handler();
//                final Runnable r = new Runnable()
//                {
//                    public void run()
//                    {
//                        mBluetoothAdapter.stopLeScan(mBtLeScanCallback);
//                        Toast.makeText(getApplicationContext(), "Scan done.", Toast.LENGTH_SHORT).show();
//                    }
//                };
//                handler.postDelayed(r, 10000); // 10 seconds.

                // Start scan.
                // Two APIs: This one lets you specify UUIDs of GATT services your app supports. Only results with those services are shown.
                // mBluetoothAdapter.startLeScan(mBtLeScanCallback, UUIDs);
                // Or just call this instead:
                mBluetoothAdapter.startLeScan(mBtLeScanCallback);
                Toast.makeText(getApplicationContext(), "Scanning.", Toast.LENGTH_SHORT).show();

            }
        }


        private BluetoothAdapter.LeScanCallback mBtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
                // Device discovered. Print info.
                // Then print EIR (Extended Inquiry Response).
                Log.d(APP, bluetoothDevice.getName() + ", RSSI=" + rssi + ", Extra-Length=" + bytes.length);
                Log.d(APP, "Extra: " + byteArrayToHex(bytes));

                // bytes[9] to bytes[12] has the Light-Sensor value.
                int lux = java.nio.ByteBuffer.wrap(new byte[]{bytes[12],bytes[11],bytes[10],bytes[9]}).getInt();
                Log.d(APP, "Light: " + lux + " lux");

                // Pick the device we want.
                String bDeviceName = bluetoothDevice.getName();
                
                 if (bluetoothDevice.getName().equals("bPart 78:FA:5A")) {
                	 
                     mbPart = bluetoothDevice;
                     connectClicked(null);
                     Toast.makeText(mContext, "Connected with bPart", Toast.LENGTH_LONG).show();
                 }
            }
        };

        
        public void startServiceClicked(View v) {
        	startService(new Intent(this, BleService.class));
        }
        
        public void stopServiceClicked(View v) {
        	stopService(new Intent(this, BleService.class));
        }
        
        public void connectClicked(View v) {
        	
        	
        	
        	
        	if(null != mbPart) {
	            // Connect to GATT Server
	            mBluetoothGatt = mbPart.connectGatt(this, false, mGattCallback);
	            mBluetoothGatt.connect();
        	} else {
        		Toast.makeText(this, "Can not connect, no ble device found", Toast.LENGTH_SHORT).show();
        	}
        }
        
        public void stopScanClicked(View v) {
        	mBluetoothAdapter.stopLeScan(mBtLeScanCallback);
            Toast.makeText(getApplicationContext(), "Scan done.", Toast.LENGTH_SHORT).show();
        }

        public void disconnectClicked(View v) {
            // Disconnect from GATT Server
            mBluetoothGatt.disconnect();
        }

        // Callback for GATT server. Every time the server interacts with us, a callback is called.
        private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.d(APP, "onConnectionStateChange() called.");

                // Let's assume the state changed from disconnected to connected.
                // Now, we can discover the services.
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.d(APP, "onServicesDiscovered() called.");
                // If this was called, it means the service discovery has finished.
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                // This is called when a characteristic value changed that we requested notifications for.
                Log.d(APP, "onCharacteristicChanged() called.");

                // Just print value as raw byte data.
                byte[] bytes = characteristic.getValue();
                Log.d(APP, byteArrayToHex(bytes));

                // And print real value.
                int lux = java.nio.ByteBuffer.wrap(new byte[]{bytes[3],bytes[2],bytes[1],bytes[0]}).getInt();
                Log.d(APP, "Light: " + lux + " lux");
            }

        };


        public void lightClicked(View v) {
            // Get a list of all services on bPart. This can only be done after onServicesDiscovered-Callback was called.
            List<BluetoothGattService> mServices = mBluetoothGatt.getServices();

            // Iterate over all Characteristics and find Light-Sensor Characteristic.
            for (BluetoothGattService service : mServices) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic.getUuid().equals(BPART_LIGHT_UUID)) {
                        // To simply read the characteristic value once:
                        // gatt.readCharacteristic(characteristic);

                        // Enable Light-Notification.
                        mBluetoothGatt.setCharacteristicNotification(characteristic, true);

                        // Globally enable notifications on bPart.
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                    }
                }
            }
        }

        public void ledClicked(View v) {
            // Get a list of all services on bPart. This can only be done after onServicesDiscovered-Callback was called.
            List<BluetoothGattService> mServices = mBluetoothGatt.getServices();

            // Iterate over all Characteristics and find LED-Characteristic.
            for (BluetoothGattService service : mServices) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic.getUuid().equals(BPART_LED_UUID)) {

                        // There is only ON and OFF for each color, so put the sliders on 0 or 255 to see any difference.
                        int r = ((SeekBar) findViewById(R.id.r)).getProgress();
                        int g = ((SeekBar) findViewById(R.id.g)).getProgress();
                        int b = ((SeekBar) findViewById(R.id.b)).getProgress();

                        // Write characteristic to server.
                        characteristic.setValue(new byte[] {(byte)r, (byte)g, (byte)b});
                        mBluetoothGatt.writeCharacteristic(characteristic);
                    }
                }
            }
        }


        // Simple byte-to-hex-string thing from Stackoverflow.
        public static String byteArrayToHex(byte[] a) {
            StringBuilder sb = new StringBuilder(a.length * 2);
            for(byte b: a)
                sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
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

    public void sendLuxToServer(int lux, String deviceId) {

        TimeSeries<Double> timeSeries = new TimeSeries<>("LUX", deviceId);

        // Add some data points to the time series. Timestamp is current time + x.
        long currentTime = System.currentTimeMillis() / 1000;
        timeSeries.addDataPoint(currentTime, lux);

        // Add two tags to the timeseries.
        // Those key-value pairs can be any string.
        timeSeries.addTag("Debug", "1");
        //timeSeries.addTag("SomeOtherTag", "SomeOtherValue");

        // Write all the data to the TSDB.
        tsdb.write(timeSeries);

    }
}
