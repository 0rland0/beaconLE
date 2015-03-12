package edu.teco.bpart;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public abstract class BleScanner {
	
    /** Constants for identifying an intent request. */
    private static int ENABLE_BT_ACTION = 0;
    private static int REQUEST_ENABLE_BT = 10;
	
    // Bluetooth data of detected beacon.
    // Will be set after a beacon was detected.
    private static String mUUID;
    private static int mMajor;
    private static int mMinor;
    
    private static int maxRSSI = Integer.MIN_VALUE;
    
    private static final String TAG = "BleScanner";
    
    /** The bluetooth adapter on the device. */
	private static BluetoothAdapter mBluetoothAdapter;
	
	public BleScanner(Context context) {
		startScan(context);
	}
	
	public void startScan(Context c) {
		
//		if(BleStatusChecker.isEnabled(c)) {
			// Get bluetooth manager.
	        BluetoothManager manager = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
	        mBluetoothAdapter = manager.getAdapter();
			
			 
			 // TODO use startScan (List<ScanFilter> filters, ScanSettings settings, ScanCallback callback) instead
             mBluetoothAdapter.startLeScan(mLeScanCallback);
			 
//		} else {
//			Toast.makeText(c, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
//		}		
	}
	
	
    
	/**
	 * Callback which is called after a bluetooth LE device was detected.
	 */
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
		
			if (rssi < maxRSSI) {
				return;
			}
			maxRSSI = rssi;
			
			int startByte = 2;
			boolean patternFound = false;
			while (startByte <= 5) {
				if ( ((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
						((int) scanRecord[startByte + 3] & 0xff) == 0x15) { //Identifies correct data length
					patternFound = true;
					break;
				}
				startByte++;
			}
			
			if (patternFound) {
				//Convert to hex String
				byte[] uuidBytes = new byte[16];
				System.arraycopy(scanRecord, startByte+4, uuidBytes, 0, 16);
				String hexString = byteArrayToHex(uuidBytes);
				
				// UUID
				mUUID = hexString.substring(0,8) + "-" +
				hexString.substring(8,12) + "-" +
				hexString.substring(12,16) + "-" +
				hexString.substring(16,20) + "-" +
				hexString.substring(20,32);
				
				// Major and minor
				mMajor = (scanRecord[startByte+20] & 0xff) * 0x100 + (scanRecord[startByte+21] & 0xff);
				mMinor = (scanRecord[startByte+22] & 0xff) * 0x100 + (scanRecord[startByte+23] & 0xff);
				
				Log.d(TAG, "UUID: " + mUUID);
				Log.d(TAG, "Major: " + mMajor);
				Log.d(TAG, "Minor: " + mMinor);
				
				onBeaconIdFound(mUUID, mMajor, mMinor);
			} 
			
		}
	};
	
	public abstract void onBeaconIdFound(String UUID, int major, int minor);
	
	private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
	
	

}
