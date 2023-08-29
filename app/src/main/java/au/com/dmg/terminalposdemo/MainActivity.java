package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

import au.com.dmg.devices.TerminalDevice;

public class MainActivity extends AppCompatActivity {

    private Button btnPayment;
    private Button btnSatellite;
    private Button btnOtherRequests;
    private Button btnPrint;
    private Button btnScan;
    private TerminalDevice device = new TerminalDevice();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device.init(getApplicationContext());

        setContentView(R.layout.activity_main);

        btnPayment = (Button) findViewById(R.id.btnUpgrade);
        btnPayment.setOnClickListener(v -> openActivityCart());

        btnSatellite = (Button) findViewById(R.id.btnSatellite);
        btnSatellite.setOnClickListener(v -> openActivitySatellite());

        btnOtherRequests = (Button) findViewById(R.id.btnTerminalInfo);
        btnOtherRequests.setOnClickListener(v -> openActivityOtherRequests());

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
    }

    private void openActivitySatellite() {
        Intent intent = new Intent(this, ActivitySatellite.class);
        startActivity(intent);
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

    public void openActivityOtherRequests() {
        Intent intent = new Intent(this, ActivityOtherRequests.class);
        startActivity(intent);
    }

    private int tapCount = 0;
    private long tapCounterStartMillis = 0;

//detect any touch event in the screen (instead of an specific view)

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventAction = event.getAction();
        if (eventAction == MotionEvent.ACTION_UP) {
            long time= System.currentTimeMillis();
            if (tapCounterStartMillis == 0 || (time-tapCounterStartMillis > 3000) ) {
                tapCounterStartMillis = time;
                tapCount = 1;
            }
            else{
                tapCount ++;
            }
            if (tapCount == 5) {
                forceExitApplication();
            }
            return true;
        }
        return false;
    }

    private void forceExitApplication() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Closing application...");
        builder.setMessage("Do you want to close the application?");
        builder.setPositiveButton("YES", (dialog, which) -> {
            this.finish();
            System.exit(0);
        });
        builder.setNegativeButton("NO", (dialog, which) -> {
            Toast.makeText(this, "Close cancelled", Toast.LENGTH_SHORT).show();
        });
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}


