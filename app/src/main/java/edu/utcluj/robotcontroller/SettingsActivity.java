package edu.utcluj.robotcontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SettingsActivity extends AppCompatActivity implements BLERecyclerAdapter.ItemClickListener {

    private Switch darkModeSwitch;

    private BLERecyclerAdapter recyclerAdapter;
    private BluetoothAdapter bleAdapter;
    private boolean scanning;

    private Handler handler;
    private static final int REQUEST_ENABLE_BT = 4;
    private static final long SCAN_PERIOD = 1000; // Stops scanning after 10 seconds.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        checkPermission();

        darkModeSwitch = findViewById(R.id.darkModeSwitch);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        darkModeSwitch.setChecked(preferences.getBoolean(GlobalData.DARK_MODE, false));
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GlobalData.setDarkMode(isChecked, SettingsActivity.this);
        });

        handler = new Handler();

        initializeLayout();

        // Initializes a Bluetooth recyclerAdapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanning = false;
        bleAdapter.stopLeScan(mBleScanCallback);
    }

    @Override
    public void onItemClick(View view, int position) {
        onPause();

        String name;

        if (recyclerAdapter.getDevice(position).getName() == null) {
            name = "Unknown Device";
        } else {
            name = recyclerAdapter.getDevice(position).getName();
        }

        final BluetoothDevice device = recyclerAdapter.getDevice(position);
        if (device == null) return;

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.ble_device_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor prefBleDeviceEditor = sharedPref.edit();
        prefBleDeviceEditor.putString(GlobalData.BT_NAME, device.getName());
        prefBleDeviceEditor.putString(GlobalData.BT_ADDRESS, device.getAddress());
        prefBleDeviceEditor.apply();

        final Intent intent = new Intent(this, MainActivity.class);

        if (scanning) {
            bleAdapter.stopLeScan(mBleScanCallback);
            scanning = false;
        }

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBleOn();
        // Clear list view recyclerAdapter.
        recyclerAdapter.clear();
        scanBleDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanBleDevice(false);
    }

    private BluetoothAdapter.LeScanCallback mBleScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(() -> {
                recyclerAdapter.addDevice(device);
                recyclerAdapter.notifyDataSetChanged();
            });
        }
    };

    private void scanBleDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(() -> {
                scanning = false;
                bleAdapter.stopLeScan(mBleScanCallback);
            }, SCAN_PERIOD);

            scanning = true;
            bleAdapter.startLeScan(mBleScanCallback);

        } else {
            scanning = false;
            bleAdapter.stopLeScan(mBleScanCallback);
        }
    }

    // Ensures Bluetooth is enabled on the device. If Bluetooth is not currently enabled, fire an intent to display a dialog asking the user to grant permission to enable it.
    private void isBleOn() {
        if (!bleAdapter.isEnabled()) {
            if (!bleAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void initializeLayout() {
        RecyclerView recyclerView = findViewById(R.id.devicesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerAdapter = new BLERecyclerAdapter(this);
        recyclerAdapter.setClickListener(this);
        recyclerView.setAdapter(recyclerAdapter);
    }

    public void checkPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Toast.makeText(SettingsActivity.this, "You can connect to a Bluetooth device now!", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(SettingsActivity.this, "Good!", Toast.LENGTH_SHORT).show();
        } else {
            checkPermission();
        }
    }
}