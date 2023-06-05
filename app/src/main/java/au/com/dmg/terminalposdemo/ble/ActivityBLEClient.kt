package au.com.dmg.terminalposdemo.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import au.com.dmg.terminalposdemo.R

class ActivityBLEClient : Activity() {

    private var mAdvStatus: TextView? = null
    private var mConnectionStatus: TextView? = null
    private var mBluetoothDevices: java.util.HashSet<BluetoothDevice>? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_client)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mAdvStatus = findViewById<View>(R.id.textView_advertisingStatus) as TextView
        mConnectionStatus = findViewById<View>(R.id.textView_connectionStatus) as TextView
        mBluetoothDevices = HashSet<BluetoothDevice>()
        mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
    }
}