package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import au.com.dmg.terminalposdemo.R;

import au.com.dmg.terminalposdemo.ingenicoUtil.BytesUtil;
import au.com.dmg.terminalposdemo.ingenicoUtil.DeviceHelper;
import com.izettle.html2bitmap.Html2Bitmap;
import com.izettle.html2bitmap.content.WebViewContent;
import com.usdk.apiservice.aidl.printer.AlignMode;
import com.usdk.apiservice.aidl.printer.FactorMode;
import com.usdk.apiservice.aidl.printer.OnPrintListener;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.scanner.CameraId;
import com.usdk.apiservice.aidl.scanner.OnScanListener;
import com.usdk.apiservice.aidl.scanner.ScannerData;
import com.usdk.apiservice.aidl.scanner.UScanner;

import java.io.IOException;
import java.io.InputStream;

// TODO PAX/Ingenico Compatibility
// TODO check library - refund
public class MainActivity extends AppCompatActivity {

    private Button btnCart;
    private Button btnSatellite;
    private Button btnPrint;
    private Button btnScan;

    //printer
    private UPrinter printer;

    //scanner
    private UScanner scanner;
    String scanString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Device services - printer and scanner
        DeviceHelper.me().init(this);
        DeviceHelper.me().bindService();

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
        initRegister();
        printer = DeviceHelper.me().getPrinter();

    }

    public void testScan() throws RemoteException {
        initRegister();
        startFrontScan();

    }

    public void openActivityCart() {
        Intent intent = new Intent(this, ActivityCart.class);
        startActivity(intent);
    }

    public void openActivitySattelite() {
        Intent intent = new Intent(this, ActivitySatellite.class);
        startActivity(intent);
    }

    private void startPrinter() throws RemoteException, IOException {

        byte[] image = readAssetsFile(this, "DMGReceipt.png");
        printer.addBmpImage(0, FactorMode.BMP1X1, image);
        printer.feedLine(1);
        printer.addBarCode(AlignMode.CENTER, 2, 48,  "ProductCode123");
        printer.feedLine(5);
        printer.startPrint(new OnPrintListener.Default() {
            @Override
            public void onFinish() throws RemoteException {
                System.out.println("=> onFinish | printing");
            }

            @Override
            public void onError(int error) throws RemoteException {
                System.out.println("=> onError: " + error);
            }
        });
        printer.cutPaper();

    }

    public void initRegister() {
        DeviceHelper.me().register(true);
    }

    private byte[] readAssetsFile(Context ctx, String fileName) throws IOException {
        InputStream input = null;
        try {
            input = ctx.getAssets().open(fileName);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void startFrontScan() throws RemoteException {
        scanner = DeviceHelper.me().getScanner(CameraId.FRONT);
        Bundle bundle = new Bundle();
        bundle.putInt(ScannerData.TIMEOUT, 30);
        scanner.startScan(bundle, new OnScanListener.Stub() {

            @Override
            public void onSuccess(String barcode) throws RemoteException {
                Thread thread = new Thread(){
                    public void run(){
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast toast = Toast.makeText(MainActivity.this, "Check logcat for code", Toast.LENGTH_LONG);
                                View view = toast.getView();

                                //Gets the actual oval background of the Toast then sets the colour filter
                                view.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);

                                //Gets the TextView from the Toast so it can be editted
                                TextView text = view.findViewById(android.R.id.message);
                                text.setTextColor(Color.WHITE);

//                                toast.setGravity(Gravity.TOP, 0, 0);
                                toast.show();
                            }
                        });
                    }
                };
                thread.start();

                System.out.println("SCANNER => " + barcode);
                System.out.println("SCANNER => bytes data: " + BytesUtil.bytes2HexString(scanner.getRecentScanResult()));

            }

            @Override
            public void onCancel() throws RemoteException {
                System.out.println("SCANNER => onCancel");
            }

            @Override
            public void onTimeout() throws RemoteException {
                System.out.println("SCANNER => onTimeout");
            }

            @Override
            public void onError(int error) throws RemoteException {
                System.out.println("SCANNER => onError | " + DeviceHelper.me().getErrorDetail(error));
            }
        });

    }
}


