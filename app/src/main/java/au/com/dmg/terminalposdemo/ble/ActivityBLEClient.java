package au.com.dmg.terminalposdemo.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.com.dmg.terminalposdemo.R;

public class ActivityBLEClient extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView devicesListView;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private final int REQUEST_ENABLE_BT = 1;
    private Handler handler;
    private AdapterDev adapterDev;
    private Set<BluetoothDevice> deviceSet;
    private ProgressDialog dialog;

    private static int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private void askForLocationPermissions() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Location permissions needed")
                    .setMessage("you need to allow this permission!")
                    .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(ActivityBLEClient.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
//                                        //Do nothing
                        }
                    })
                    .create()
                    .show();
        } else {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //Do you work
                } else {
                    Toast.makeText(this, "Can not proceed! i need permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    public static boolean isPermissionGranted(String[] grantPermissions, int[] grantResults,
                                              String permission) {
        for (int i = 0; i < grantPermissions.length; i++) {
            if (permission.equals(grantPermissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_find);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermissions();
        }

        handler = new Handler();
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage("Loading");
        dialog.show();
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresher);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                deviceSet.clear();
                adapterDev.notifyDataSetChanged();
                checkBleAdapter();
            }
        });
        devicesListView = (ListView) findViewById(R.id.device_list);
        devicesListView.setOnItemClickListener(this);
        deviceSet = new HashSet<>();
        adapterDev = new AdapterDev();
        devicesListView.setAdapter(adapterDev);
        if(Helper.checkBLE(this)) {
            BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            checkBleAdapter();
        }
    }

    @SuppressLint("MissingPermission")
    private void checkBleAdapter() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            scanLeDevice(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(boolean b) {
        ScanFilter.Builder filter = new ScanFilter.Builder();
        filter.setServiceUuid(GattService.UUID);
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter.build());
        if (b) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(scanCallback);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                        if(dialog.isShowing()){
                            dialog.hide();
                        }
                    }
                }
            }, 10000);
            //scanner.startScan(scanCallback);
            scanner.startScan(filters, new ScanSettings.Builder().build(), scanCallback);
        } else {
            scanner.stopScan(scanCallback);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(dialog.isShowing()){
                        dialog.hide();
                    }
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        BluetoothDevice device = deviceSet.toArray(new BluetoothDevice[0])[i];
        Intent intent = new Intent(this, ServicesList.class);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BluetoothDevice device = result.getDevice();
                    deviceSet.add(device);
                    adapterDev.notifyDataSetChanged();
                    if(dialog.isShowing()){
                        dialog.hide();
                    }
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private class AdapterDev extends ArrayAdapter<BluetoothDevice> {

        public AdapterDev() {
            super(ActivityBLEClient.this, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
            BluetoothDevice device = deviceSet.toArray(new BluetoothDevice[0])[position];
            @SuppressLint("MissingPermission") String name = device.getName();
            if (name == null || name.isEmpty()){
                name = device.getAddress();
            }
            v.setText(name);
            return v;
        }

        @Override
        public int getCount() {
            return deviceSet.size();
        }
    }
}