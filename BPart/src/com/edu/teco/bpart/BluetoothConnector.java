//package com.edu.teco.bpart;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.util.Log;
//import android.widget.Toast;
//
///**
// * This class takes care of the bluetooth scanning, connecting
// * and disconnecting stuff.
// * 
// * @author orlando
// *
// */
//public class BluetoothConnector {
//	
//	/** The bluetooth adapter on the device. */
//	private static BluetoothAdapter mBluetoothAdapter;
//	
//	private static final String TAG = "BluetoothConnector";
//	
//	private void scan() {
//		 if (mBluetoothAdapter.isEnabled()) {
//
//			 // TODO use startScan (List<ScanFilter> filters, ScanSettings settings, ScanCallback callback) instead
//             mBluetoothAdapter.startLeScan(mBtLeScanCallback);
//		 }
//	}
//	
//	private BluetoothAdapter.LeScanCallback mBtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
//            // Device discovered. Print info.
//            // Then print EIR (Extended Inquiry Response).
//            Log.d(TAG, bluetoothDevice.getName() + ", RSSI=" + rssi + ", Extra-Length=" + bytes.length);
//            Log.d(TAG, "Extra: " + MathHelper.byteArrayToHex(bytes));
//
//            // bytes[9] to bytes[12] has the Light-Sensor value.
//            int lux = java.nio.ByteBuffer.wrap(new byte[]{bytes[12],bytes[11],bytes[10],bytes[9]}).getInt();
//            Log.d(TAG, "Light: " + lux + " lux");
//
//            // Pick the device we want.
//            String bDeviceName = bluetoothDevice.getName();
//            
//             if (bluetoothDevice.getName().equals("bPart 78:FA:5A")) {
//            	 
//                 mbPart = bluetoothDevice;
//                 connectClicked(null);
//                 Toast.makeText(mContext, "Connected with bPart", Toast.LENGTH_LONG).show();
//             }
//        }
//    };
//
//}
