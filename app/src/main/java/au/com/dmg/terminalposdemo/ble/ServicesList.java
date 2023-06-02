package au.com.dmg.terminalposdemo.ble;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import au.com.dmg.terminalposdemo.R;
import au.com.dmg.terminalposdemo.ble.GattService;
import au.com.dmg.terminalposdemo.ble.Helper;


public class ServicesList extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private ListView servicesList;
    private LinearLayout messageContainer;
    private BluetoothDevice device;
    private List<String> servicesListNames;
    private ArrayAdapter<String> servicesAdapter;
    private Handler handler;
    private List<BluetoothGattService> services;
    private BluetoothGatt currentGatt;
    private EditText message;
    private Button send;
    private BluetoothGattCharacteristic characteristic;
    private ProgressDialog dialog;

    private static final String TAG = "SSERVICELIST";

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.services_list);

        handler = new Handler();
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("Loading Services");
        device = getIntent().getExtras().getParcelable("device");
        servicesList = (ListView) findViewById(R.id.services_list);
        messageContainer = (LinearLayout) findViewById(R.id.message_container);
        message = (EditText) findViewById(R.id.message);
        send = (Button) findViewById(R.id.send);
        currentGatt = device.connectGatt(this, false, gattCallback);
        dialog.show();
        servicesListNames = new ArrayList<>();
        servicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, servicesListNames);
        servicesList.setAdapter(servicesAdapter);
        servicesList.setOnItemClickListener(this);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!message.getText().toString().trim().isEmpty()) {
                    String x = message.getText().toString();
                    characteristic = new BluetoothGattCharacteristic(
                            GattService.DES_UUID,
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ);
//                    characteristic.addDescriptor();
//                    characteristic.addDescriptor();


                    characteristic.setValue(message.getText().toString().getBytes());
                    currentGatt.writeCharacteristic(characteristic);
                    currentGatt.setCharacteristicNotification(characteristic, true);
                    message.setText("TESTVAN2");
                }
            }
        });
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
//                currentGatt.discoverServices();
                System.out.println("TAG------Device connected");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        boolean ans = currentGatt.discoverServices();
                        System.out.println("TAG------Discover Services started: " + ans);
                    }
                });


            }else{
                if(dialog.isShowing()){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.hide();
                        }
                    });
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
//            services = currentGatt.getServices();
//            for (BluetoothGattService service : services) {
//                servicesListNames.add(Helper.getServiceName(service.getUuid().toString()));
//            }
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    servicesAdapter.notifyDataSetChanged();
//                }
//            });
            System.out.println("TAG------onServicesDiscovered");
            services = currentGatt.getServices();
            for(BluetoothGattService service : services){
                System.out.println("TAG------Uuid = " + service.getUuid().toString());
                servicesListNames.add(Helper.getServiceName(service.getUuid().toString()));

//                BluetoothGattCharacteristic ble_my_characterstic = currentGatt.getService(service.getUuid()).getCharacteristic(GattService.CHAR_UUID);
                Log.d(TAG, "BLE SELECTED CHARACTERSTIC " + GattService.CHAR_UUID);

//                currentGatt.setCharacteristicNotification(service.getCharacteristic(GattService.CHAR_UUID), true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        servicesAdapter.notifyDataSetChanged();
                    }
                });
            }
            if (dialog.isShowing()){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.hide();
                    }
                });
            }
            

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead " + GattService.CHAR_UUID); //not firing why???
            Log.d(TAG, "onCharacteristicRead " + characteristic.getValue());
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite " + characteristic.getValue());
            gatt.executeReliableWrite();

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged " + characteristic.getValue());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead " + characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite " + characteristic.getValue());
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted " + characteristic.getValue());
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d(TAG, "onReadRemoteRssi " + characteristic.getValue());
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged " + characteristic.getValue());
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(services != null){
            BluetoothGattService notificationService = services.get(position);
            if(notificationService.getUuid().equals(GattService.SERVICE_UUID)){
                characteristic = notificationService.getCharacteristic(GattService.CHAR_UUID);
//                if(characteristic != null) {
                    messageContainer.setVisibility(View.VISIBLE);
//                }

            }else{
                Toast.makeText(this, "Can't connect to this service", Toast.LENGTH_SHORT).show();
            }
        }
    }
}