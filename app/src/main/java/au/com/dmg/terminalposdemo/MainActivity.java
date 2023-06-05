package au.com.dmg.terminalposdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;

import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.terminalposdemo.ble.ActivityBLEClient;
import au.com.dmg.terminalposdemo.ble.ActivityBLEServer;

public class MainActivity extends AppCompatActivity {
//    private static final int ENABLE_BLUETOOTH_REQUEST = 17;
    private Button btnCart;
    private Button btnSatellite;
    private Button btnPrint;
    private Button btnScan;
    private TerminalDevice device = new TerminalDevice();

    private Button btnBLEClient; // terminal app - can accept multiple servers
    private Button btnBLEServer; // SDK - only one client at a time
    private static int LOCATION_PERMISSION_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device.init(getApplicationContext());
        System.out.println("ANDROID BUILD VERSION: " + Build.VERSION.SDK_INT);
        setContentView(R.layout.activity_main);

        btnCart = (Button) findViewById(R.id.btnPayment);
        btnCart.setOnClickListener(v -> openActivityCart());

        btnSatellite = (Button) findViewById(R.id.btnSatellite);
        btnSatellite.setOnClickListener(v -> openActivitySatellite());

        btnPrint = (Button) findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(v -> {
            try {
                testPrint();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        btnScan = (Button) findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> {
            try {
                testScan();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        btnBLEServer = (Button) findViewById(R.id.btnBLEServer);
        btnBLEServer.setOnClickListener(v->
        {
            Intent intent = new Intent(this, ActivityBLEServer.class);
            startActivity(intent);
        });

        btnBLEClient = (Button) findViewById(R.id.btnClient);
        btnBLEClient.setOnClickListener(v->
        {
            Intent intent = new Intent(this, ActivityBLEClient.class);
            startActivity(intent);
        });
    }



    public void testPrint() throws RemoteException {
        new Thread(new Runnable(){
            public void run() {
                    try {
                        Bitmap img = getBitmapFromAsset(getApplicationContext(),"DMGReceipt.png");
                        device.printBitmap(img);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

            }
        }).start();

    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();
        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }
    @SuppressLint("HandlerLeak")
    private final Handler barcodeHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                Toast toast = Toast.makeText(MainActivity.this, "Barcode: " + msg.obj.toString(), Toast.LENGTH_LONG);
                View view = toast.getView();

                view.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);

                TextView text = view.findViewById(android.R.id.message);
                text.setTextColor(Color.WHITE);

                toast.show();
            }
        };
    };
    public void testScan() throws RemoteException {

        try {
            device.scanBarcode(barcodeHandler, 30, TerminalDevice.camera_back);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void openActivityCart() {
        Intent intent = new Intent(this, ActivityPayment.class);
        startActivity(intent);
    }

    public void openActivitySatellite() {
        Intent intent = new Intent(this, ActivitySatellite.class);
        startActivity(intent);
    }


//    private void askForLocationPermissions() {
//
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)) {
//            //
//            new AlertDialog.Builder(this)
//                    .setTitle("Location permissions needed")
//                    .setMessage("you need to allow this permission!")
//                    .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            ActivityCompat.requestPermissions(MainActivity.this,
//                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                                    LOCATION_PERMISSION_REQUEST_CODE);
//                        }
//                    })
//                    .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                        }
//                    })
//                    .create()
//                    .show();
//
//        } else {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//
//        }
//    }

}


