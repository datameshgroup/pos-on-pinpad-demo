package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.fusion.Message;

public class MainActivity extends AppCompatActivity {

    private Button btnCart;
    private Button btnSatellite;
    private Button btnPrint;
    private Button btnScan;
    private Button btnUpdateSatellite;
    private TerminalDevice device = new TerminalDevice();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device.init(getApplicationContext());

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

        btnUpdateSatellite = (Button) findViewById(R.id.btnUpdateSatellite);
        btnUpdateSatellite.setOnClickListener(v -> {
            forceUpdateSatellite();
                }
        );
    }

    private void forceUpdateSatellite() {

        Intent intent = new Intent(Message.AXIS_PULL_UPDATE);

        Log.d("TerminalPOSDemo","Updating Satellite...");

        // AXIS_RESULT_ACTIVITY = Activity to go back to after the update
        intent.putExtra(Message.AXIS_RESULT_ACTIVITY, "au.com.dmg.terminalposdemo.MainActivity");
        intent.setAction("au.com.dmg.axispay.action.UPDATE");
        startActivityForResult(intent,100);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d("TerminalPOSDemo","Main Activity onResume...");
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey("result")) {
            String updateResult = getIntent().getStringExtra("result");
            Toast.makeText(MainActivity.this, updateResult, Toast.LENGTH_LONG).show();
        }
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

}


