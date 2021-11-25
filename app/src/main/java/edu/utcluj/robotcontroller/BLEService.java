package edu.utcluj.robotcontroller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * *****************************************************
 * * This is a service to handle the BLE interactions. *
 * *****************************************************
 **/

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEService extends Service {
    private final static String TAG = BLEService.class.getSimpleName();

    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private String bleDeviceAddress;
    public BluetoothGatt bleGatt;
    private int connectionState = STATE_DISCONNECTED;
    private List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    public BluetoothGattCharacteristic serialCharacteristic;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ_COMPLETED = "ACTION_DATA_READ_COMPLETED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";

    // Implements callback methods for GATT events that the app cares about. For example, connection change and services discovered.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;

                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "Attempting to start service discovery.");
                gatt.discoverServices();
                Log.i(TAG, "Connected to GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            } else {
                Log.i(TAG, "Unknown Bluetooth state.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                List<BluetoothGattService> services = gatt.getServices();

                // Loops through available GATT Services.
                for (BluetoothGattService gattService : services) {
                    List<BluetoothGattCharacteristic> gattCharacteristicsList = gattService.getCharacteristics();

                    // Loops through available Characteristics.
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristicsList) {
                        chars.add(gattCharacteristic);
                        if (gattCharacteristic.getWriteType() == 1) {
                            serialCharacteristic = gattCharacteristic;
                            //gattCharacteristic.setValue("F");
                            //gatt.writeCharacteristic(gattCharacteristic);
                        }
                    }
                }

                requestCharacteristics(gatt);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE);

                if (chars.size() > 0) {
                    gatt.readCharacteristic(chars.get(chars.size() - 1));
                } else {
                    Log.i(TAG, "Gatt server data read completed.");
                    broadcastUpdate(ACTION_DATA_READ_COMPLETED);
                    disconnect();
                }
            }
        }

        public void requestCharacteristics(BluetoothGatt gatt) {
            gatt.readCharacteristic(chars.get(chars.size() - 1));
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    public boolean initialize() {
        if (bleManager == null) {
            bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (bleManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bleAdapter = bleManager.getAdapter();

        if (bleAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (bleAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(bleDeviceAddress) && bleGatt != null) {
            Log.d(TAG, "Trying to use an existing bleGatt for connection.");

            if (bleGatt.connect()) {
                connectionState = STATE_CONNECTING;
                Log.d(TAG, "Connecting...");
                return true;

            } else {
                Log.d(TAG, "Can't connect...");
                return false;
            }
        }

        final BluetoothDevice device = bleAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bleGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bleDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bleGatt.disconnect();
    }

    public void close() {
        if (bleGatt == null) {
            return;
        }
        bleGatt.close();
        bleGatt = null;
    }
}