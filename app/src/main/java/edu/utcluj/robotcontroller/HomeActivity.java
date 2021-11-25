package edu.utcluj.robotcontroller;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class HomeActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private TextView speedTextView, angleTextView, userTitleTextView, bluetoothDeviceTextView;

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BLEService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    private String deviceName;
    private String deviceAddress;
    private BluetoothAdapter bleAdapter;
    private BLEService bleService;

    private SharedPreferences sharedPreferences;

    private static final int REQUEST_BLUETOOTH_ADMIN_ID = 1;
    private static final int REQUEST_LOCATION_ID = 2;
    private static final int REQUEST_BLUETOOTH_ID = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);

        setContentView(R.layout.activity_home);

        speedTextView = findViewById(R.id.speedLabel);
        angleTextView = findViewById(R.id.angleLabel);
        userTitleTextView = findViewById(R.id.userTitle);
        bluetoothDeviceTextView = findViewById(R.id.btTitle);

        bleCheck();
        locationCheck();

        sharedPreferences = getSharedPreferences(getString(R.string.ble_device_key), Context.MODE_PRIVATE);
        deviceName = sharedPreferences.getString(GlobalData.BT_NAME, null);
        deviceAddress = sharedPreferences.getString(GlobalData.BT_ADDRESS, null);

        userTitleTextView.setText(GlobalData.getLoggedInUser().getEmail());

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bluetooth_unsupported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (bleAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_unsupported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (deviceName != null && !deviceName.isEmpty()) {
            bluetoothDeviceTextView.setText(deviceName + " (" + deviceAddress + ")");
        } else {
            bluetoothDeviceTextView.setText(R.string.bluetooth_connected_not);
        }

        JoystickView joystick = findViewById(R.id.joystick);
        joystick.setOnMoveListener((angle, strength) -> {
            speedTextView.setText(String.format("Speed: %s", strength));
            angleTextView.setText(String.format("Angle: %s", angle));

            bleService.serialCharacteristic.setValue(angle + "," + strength + "*");
            bleService.bleGatt.writeCharacteristic(bleService.serialCharacteristic);
        });

        establishServiceConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleService != null) {
            unbindService(serviceConnection);
            bleService = null;
        }
    }

    // Handles various events fired by the Service.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                Toast.makeText(context, R.string.conn_success, Toast.LENGTH_SHORT).show();

            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(context, R.string.conn_fail, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
            final boolean result = bleService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_READ_COMPLETED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void establishServiceConnection() {
        if (deviceName != null && deviceAddress != null) {
            Intent gattServiceIntent = new Intent(this, BLEService.class);
            bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    private void bleCheck() {
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{BLUETOOTH}, REQUEST_BLUETOOTH_ID);
        }
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth admin permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_ADMIN_ID);
        }
    }

    private void locationCheck() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Location permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION_ID);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intentSettings = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intentSettings);
                break;
            case R.id.logout:
                GlobalData.setLoggedInUser(null, HomeActivity.this);

                Intent intentLogout = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intentLogout);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}