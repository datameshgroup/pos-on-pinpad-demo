package au.com.dmg.terminalposdemo.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import au.com.dmg.terminalposdemo.R;

public class ActivityBLEServer extends Activity {

  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = "SSERVER";
  private static Context mContext;

  private TextView mAdvStatus;
  private TextView mConnectionStatus;
//  private ServiceFragment mCurrentServiceFragment;
  private BluetoothGattService mBluetoothGattService;
  private BluetoothGatt currentGatt;
  private HashSet<BluetoothDevice> mBluetoothDevices;
  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private AdvertiseData mAdvData;
  private AdvertiseData mAdvScanResponse;
  private AdvertiseSettings mAdvSettings;
  private BluetoothLeAdvertiser mAdvertiser;

  private static final UUID MESSAGE_SERVICE_UUID = UUID.fromString("49cef8b2-71f7-4e4f-8c5c-1f7c766addc8");
//  private static final UUID MESSAGE_DESC_UUID = UUID.fromString("6b21765b-6917-4651-8516-4b11fc6d84a8");
//  private static final String MESSAGE_DES_TEXT = "This is the MESSAGE_DESCRIPTION";

  private static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID.fromString("4473bd07-53db-42d4-bc41-61e3a703551b");
  private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("4473bd07-53db-42d4-bc41-61e3a703551b");

  // GATT
  private BluetoothGattCharacteristic mMessageCharacteristic;

  // UI
  private EditText mMessage1EditText;
  private EditText mMessage2EditText;
  private final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
    @Override
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);
      Log.e(TAG, "Not broadcasting: " + errorCode);
      int statusText;
      switch (errorCode) {
        case ADVERTISE_FAILED_ALREADY_STARTED:
          statusText = R.string.status_advertising;
          Log.w(TAG, "App was already advertising");
          break;
        case ADVERTISE_FAILED_DATA_TOO_LARGE:
          statusText = R.string.status_advDataTooLarge;
          break;
        case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
          statusText = R.string.status_advFeatureUnsupported;
          break;
        case ADVERTISE_FAILED_INTERNAL_ERROR:
          statusText = R.string.status_advInternalError;
          break;
        case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
          statusText = R.string.status_advTooManyAdvertisers;
          break;
        default:
          statusText = R.string.status_notAdvertising;
          Log.wtf(TAG, "Unhandled error: " + errorCode);
      }
      mAdvStatus.setText(statusText);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      Log.v(TAG, "Broadcasting");
      mAdvStatus.setText(R.string.status_advertising);
    }
  };

  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
    throw new UnsupportedOperationException("Method writeCharacteristic not overridden");
  };
  private BluetoothGattServer mGattServer;
  private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
      super.onConnectionStateChange(device, status, newState);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
          mBluetoothDevices.add(device);
          updateConnectedDevicesStatus();
          Log.v(TAG, "Connected to device: " + device.getAddress());

          ///copied
          currentGatt = device.connectGatt(mContext, false, gattCallback);

        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          mBluetoothDevices.remove(device);
          updateConnectedDevicesStatus();
          Log.v(TAG, "Disconnected from device");
        }
      } else {
        mBluetoothDevices.remove(device);
        updateConnectedDevicesStatus();
        // There are too many gatt errors (some of them not even in the documentation) so we just
        // show the error to the user.
        final String errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(ActivityBLEServer.this, errorMessage, Toast.LENGTH_LONG).show();
          }
        });
        Log.e(TAG, "Error when connecting: " + status);
      }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
      @SuppressLint("MissingPermission")
      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if(newState == BluetoothProfile.STATE_CONNECTED) {
//                currentGatt.discoverServices();
          System.out.println("ServerTAG------Device connected");
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              boolean ans = currentGatt.discoverServices();
              System.out.println("ServerTAG------Discover Services started: " + ans);
            }
          });


        }
        else{
          System.out.println("ServerTAG------else");
        }
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Log.d(TAG, "onCharacteristicRead " + GattService.CHAR_UUID);
        Log.d(TAG, "onCharacteristicRead " + characteristic.getValue());
      }

      @SuppressLint("MissingPermission")
      @Override
      public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        gatt.executeReliableWrite();
        Log.d(TAG, "onCharacteristicWrite " + GattService.CHAR_UUID);

      }

      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.d(TAG, "onCharacteristicChanged " + GattService.CHAR_UUID);
      }

      @Override
      public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        Log.d(TAG, "onDescriptorRead " + GattService.CHAR_UUID);
      }

      @Override
      public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.d(TAG, "onDescriptorWrite " + GattService.CHAR_UUID);
      }

      @Override
      public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        Log.d(TAG, "onReliableWriteCompleted " + GattService.CHAR_UUID);
      }

      @Override
      public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        Log.d(TAG, "onReadRemoteRssi " + GattService.CHAR_UUID);
      }

      @Override
      public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        Log.d(TAG, "onMtuChanged " + GattService.CHAR_UUID);
      }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
        BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
      Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
      if (offset != 0) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
        return;
      }
      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
          offset, characteristic.getValue());
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
      super.onNotificationSent(device, status);
      Log.v(TAG, "Notification sent. Status: " + status);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
        BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
        int offset, byte[] value) {
      super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
          responseNeeded, offset, value);
      Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
      int status = writeCharacteristic(characteristic, offset, value);
      if (responseNeeded) {
        mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
      }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
        int offset, BluetoothGattDescriptor descriptor) {
      super.onDescriptorReadRequest(device, requestId, offset, descriptor);
      Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
      Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
      if (offset != 0) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
        return;
      }
      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
          descriptor.getValue());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
        BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
        int offset,
        byte[] value) {
      super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
          offset, value);
      Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
      int status = BluetoothGatt.GATT_SUCCESS;
      if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        boolean supportsNotifications = (characteristic.getProperties() &
            BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        boolean supportsIndications = (characteristic.getProperties() &
            BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

        if (!(supportsNotifications || supportsIndications)) {
          status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
        } else if (value.length != 2) {
          status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
        } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS;
          notificationsDisabled(characteristic);
          descriptor.setValue(value);
        } else if (supportsNotifications &&
            Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS;
          notificationsEnabled(characteristic, false /* indicate */);
          descriptor.setValue(value);
        } else if (supportsIndications &&
            Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS;
          notificationsEnabled(characteristic, true /* indicate */);
          descriptor.setValue(value);
        } else {
          status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
        }
      } else {
        status = BluetoothGatt.GATT_SUCCESS;
        descriptor.setValue(value);
      }
      if (responseNeeded) {
        mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
      }
    }
  };


  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ble_server);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    mAdvStatus = (TextView) findViewById(R.id.textView_advertisingStatus);
    mConnectionStatus = (TextView) findViewById(R.id.textView_connectionStatus);
    mBluetoothDevices = new HashSet<>();
    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();

    mMessage1EditText = (EditText) findViewById(R.id.editText_message1);
    mMessage2EditText = (EditText) findViewById(R.id.editText_message2);
    Button notifyButton = (Button) findViewById(R.id.button_messageNotify);

    notifyButton.setOnClickListener(v -> {
      mMessageCharacteristic = new BluetoothGattCharacteristic(
              GattService.DES_UUID,
              BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ);
//                    characteristic.addDescriptor();
//                    characteristic.addDescriptor();
//      characteristic.setValue(message.getText().toString().getBytes());
      String x = "TESTVAN3";
      mMessageCharacteristic.setValue(x.getBytes());
      currentGatt.writeCharacteristic(mMessageCharacteristic);
//      writeCharacteristic(mMessageCharacteristic,0, x.getBytes());
      sendNotificationToDevices(mMessageCharacteristic);
    });

    mBluetoothGattService = new BluetoothGattService(MESSAGE_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY);

    mAdvSettings = new AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build();
    mAdvData = new AdvertiseData.Builder()
        .setIncludeTxPowerLevel(true)
        .addServiceUuid(new ParcelUuid(MESSAGE_SERVICE_UUID))
        .build();
    mAdvScanResponse = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .build();
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_ble, menu);
    return true /* show menu */;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == RESULT_OK) {
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
          Toast.makeText(this, R.string.bluetoothAdvertisingNotSupported, Toast.LENGTH_LONG).show();
          Log.e(TAG, "Advertising not supported");
        }
        onStart();
      } else {
        //TODO(g-ortuno): UX for asking the user to activate bt
        Toast.makeText(this, R.string.bluetoothNotEnabled, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Bluetooth not enabled");
        finish();
      }
    }
  }


  @SuppressLint("MissingPermission")
  @Override
  protected void onStart() {
    super.onStart();
    resetStatusViews();
    // If the user disabled Bluetooth when the app was in the background,
    // openGattServer() will return null.
    mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
    if (mGattServer == null) {
      ensureBleFeaturesAvailable();
      return;
    }
    // Add a service for a total of three services (Generic Attribute and Generic Access
    // are present by default).
    mGattServer.addService(mBluetoothGattService);

    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
      mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
      mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
    } else {
      mAdvStatus.setText(R.string.status_noLeAdv);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_disconnect_devices) {
      disconnectFromDevices();
      return true /* event_consumed */;
    }
    return false /* event_consumed */;
  }

  @SuppressLint("MissingPermission")
  @Override
  protected void onStop() {
    super.onStop();
    if (mGattServer != null) {
      mGattServer.close();
    }
    if (mBluetoothAdapter.isEnabled() && mAdvertiser != null) {
      // If stopAdvertising() gets called before close() a null
      // pointer exception is raised.
      mAdvertiser.stopAdvertising(mAdvCallback);
    }
    resetStatusViews();
  }

  @SuppressLint("MissingPermission")
  public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
    Log.i(TAG, "CHECK---sendNotificationToDevices");
    for (BluetoothDevice device : mBluetoothDevices) {
      characteristic.getDescriptor(CHARACTERISTIC_USER_DESCRIPTION_UUID);
      mGattServer.notifyCharacteristicChanged(device, characteristic, false);
      Log.i(TAG, "CHECK---sendNotificationToDevices + " + device + " = " + characteristic.getUuid() +  " = " + characteristic.getValue() + " " + device.getAddress() );
    }
  }

  private void resetStatusViews() {
    mAdvStatus.setText(R.string.status_notAdvertising);
    updateConnectedDevicesStatus();
  }

  private void updateConnectedDevicesStatus() {
    @SuppressLint("MissingPermission")
    final String message = getString(R.string.status_devicesConnected) + " "
        + mBluetoothManager.getConnectedDevices(BluetoothGattServer.GATT).size();
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mConnectionStatus.setText(message);
      }
    });


  }

  public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
        CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
        (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
    descriptor.setValue(new byte[]{0, 0});
    return descriptor;
  }

  public static BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor(String defaultValue) {
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
        CHARACTERISTIC_USER_DESCRIPTION_UUID,
        (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
    try {
      descriptor.setValue(defaultValue.getBytes("UTF-8"));
    } finally {
      return descriptor;
    }
  }

  @SuppressLint("MissingPermission")
  private void ensureBleFeaturesAvailable() {
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show();
      Log.e(TAG, "Bluetooth not supported");
      finish();
    } else if (!mBluetoothAdapter.isEnabled()) {
      // Make sure bluetooth is enabled.
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }
  @SuppressLint("MissingPermission")
  private void disconnectFromDevices() {
    Log.d(TAG, "Disconnecting devices...");
    for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(
        BluetoothGattServer.GATT)) {
      Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
      mGattServer.cancelConnection(device);
    }
  }


  public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
    if (indicate) {
      return;
    }
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(mContext, R.string.notificationsEnabled, Toast.LENGTH_SHORT)
                .show();
      }
    });
  }


  public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid() != MESSAGE_SERVICE_UUID) {
      return;
    }
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(mContext, R.string.notificationsNotEnabled, Toast.LENGTH_SHORT)
                .show();
      }
    });
  }
}
