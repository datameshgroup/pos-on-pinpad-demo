package au.com.dmg.terminalposdemo.ble;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.ParcelUuid;
import androidx.core.app.NotificationCompat;

import java.lang.ref.WeakReference;

import au.com.dmg.terminalposdemo.R;

public class GattService extends Service {

    private static WeakReference<Context> mContext;
    private static int NOTIFICATION_ID = 0;

    public static final ParcelUuid UUID = ParcelUuid.fromString("49cef8b2-71f7-4e4f-8c5c-1f7c766addc8"); //connection uuid
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("49cef8b2-71f7-4e4f-8c5c-1f7c766addc8");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("4473bd07-53db-42d4-bc41-61e3a703551b");
    public static final java.util.UUID DES_UUID = java.util.UUID.fromString("d7109bad-609d-486a-a502-0c431c893edc");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer server;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean start;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("VANNNNNNNNNNNNNNNNNN --- start command");
        setupBluetooth();
        return Service.START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void setupBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        server = bluetoothManager.openGattServer(this, serverCallback);
        initServer();
        bluetoothAdapter = bluetoothManager.getAdapter();
        advertise();
    }

    @SuppressLint("MissingPermission")
    private void initServer() {
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristic);
        server.addService(service);
    }

    @SuppressLint("MissingPermission")
    private void advertise() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseData advertisementData = getAdvertisementData();
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);
        start = true;
    }

    @SuppressLint("MissingPermission")
    private AdvertiseData getAdvertisementData() {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeTxPowerLevel(true);
        builder.addServiceUuid(UUID);
        bluetoothAdapter.setName("BLE client");
        builder.setIncludeDeviceName(true);
        return builder.build();
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setConnectable(true);
        return builder.build();
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @SuppressLint("Override")
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            final String message = "Advertisement successful";
            sendNotification(message);
        }

        @SuppressLint("Override")
        @Override
        public void onStartFailure(int i) {
            final String message = "Advertisement failed error code: " + i;
            sendNotification(message);

        }

    };

    private BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                sendNotification("Client connected");
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            System.out.println("CHECKING---onServiceAdded");
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            System.out.println("CHECKING---onCharacteristicReadRequest");
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            System.out.println("onCharacteristicWriteRequest");
            byte[] bytes = value;
            String message = new String(bytes);
            sendNotification(message);
            server.sendResponse(device, requestId, 0, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            System.out.println("CHECKING---onDescriptorReadRequest");
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            System.out.println("CHECKING---onDescriptorWriteRequest");
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            System.out.println("CHECKING---onExecuteWrite");
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            System.out.println("CHECKING---onNotificationSent");
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        if(start){
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        }
        super.onDestroy();
    }

    private void sendNotification(String message){
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setAutoCancel(true)
                        .setContentText(message);
        Notification note = mBuilder.build();
        note.defaults |= Notification.DEFAULT_VIBRATE;
        note.defaults |= Notification.DEFAULT_SOUND;
        mNotificationManager.notify(NOTIFICATION_ID++, note);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}