package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;


// TODO PAX/Ingenico Compatibility
// TODO check library - refund
public class MainActivity extends AppCompatActivity {

    private Button btnCart;
    private Button btnSatellite;
    private Button btnPrint;
    private Button btnScan;
    private TestHardware testPrint = new TestHardware();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        testPrint.initDevice(getApplicationContext());

        setContentView(R.layout.activity_main);

        btnCart = (Button) findViewById(R.id.btnCart);
        btnCart.setOnClickListener(v -> openActivityCart());

        btnSatellite = (Button) findViewById(R.id.btnSatellite);
        btnSatellite.setOnClickListener(v -> openActivitySattelite());

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

    public void testPrint() throws RemoteException {
        new Thread(new Runnable(){
            public void run() {
                    try {
                        testPrint.startPrinter(MainActivity.this);
                    } catch (RemoteException | IOException e) {
                        e.printStackTrace();
                    }

            }
        }).start();

    }

    public void testScan() throws RemoteException {
        testPrint.startFrontScan();
        Thread thread = new Thread(){
            public void run(){
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast toast = Toast.makeText(MainActivity.this, "Check logcat for code", Toast.LENGTH_LONG);
                        View view = toast.getView();

                        view.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);

                        TextView text = view.findViewById(android.R.id.message);
                        text.setTextColor(Color.WHITE);

                        toast.show();
                    }
                });
            }};
        thread.start();
    }

    public void openActivityCart() {
        Intent intent = new Intent(this, ActivityCart.class);
        startActivity(intent);
    }

    public void openActivitySattelite() {
        Intent intent = new Intent(this, ActivitySatellite.class);
        startActivity(intent);
    }

}


