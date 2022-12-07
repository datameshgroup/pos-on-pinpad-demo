package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

//import au.com.dmg.terminalposdemo.Ingenico.DeviceHelper;
//import au.com.dmg.terminalposdemo.PaxPrint;

import com.izettle.html2bitmap.Html2Bitmap;
import com.izettle.html2bitmap.content.WebViewContent;
//import com.pax.dal.IPrinter;
//import com.pax.dal.exceptions.PrinterDevException;

import java.io.IOException;
import java.io.InputStream;

// TODO PAX/Ingenico Compatibility
// TODO check library - refund
public class MainActivity extends AppCompatActivity {

    private Button btnCart;
    private Button btnSatellite;
    private Button btnPrint;
    private Button btnScan;

//    ///printer
//    private UPrinter printer;
//
//    ///scanner
//    private UScanner scanner;

//    ///pax
//    IPrinter printer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        /// Device services - printer and scanner
//        DeviceHelper.me().init(this);
//        DeviceHelper.me().bindService();

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
//        initRegister();
//        printer = DeviceHelper.me().getPrinter();

        new Thread(new Runnable(){
            public void run() {
                String x = "PHAgaWQ9InJlY2VpcHQtaW5mbyI+MjIvMTEvMjAyMiAxMjoyNzo1NDxici8+TWVyY2hhbnQgSUQ6IE0wMDAwMDAyNTxici8+VGVybWluYWwgSUQ6IERNR1ZBMDAxPC9wPjxwIGlkPSJyZWNlaXB0LWRldGFpbHMiPjxiPlB1cmNoYXNlIFRyYW5zYWN0aW9uPC9iPjxici8+QW1vdW50OiAkOTAuMDA8YnIvPlN1cmNoYXJnZTogJDAuNTg8YnIvPlRpcDogJDkuOTk8YnIvPlRvdGFsOiAkMTAwLjU3PGJyLz5NYXN0ZXJDYXJkOiA1MzYzMzRYWFhYWFgyMDU0IChUKTxici8+Q3JlZGl0IEFjY291bnQ8L3A+PHAgaWQ9InJlY2VpcHQtcmVzdWx0Ij48Yj5BcHByb3ZlZDwvYj48YnIvPlJlZmVyZW5jZTogMDAwMCAwMDA3IDA3NjI8YnIvPkF1dGggQ29kZTogNzE0NjE1PGJyLz5BSUQ6IEEwMDAwMDAwMDQxMDEwPGJyLz5BVEM6IDBBQTI8YnIvPlRWUjogODAwMDAwODAwMTxici8+QVJRQzogOUNBMzI4NUZGQ0U3QkZDRTwvcD4=";
                String y = "<html><head>HEAD </head><body>BODYemememememememememememememe</body></html>";

                Html2Bitmap build = new Html2Bitmap.Builder()
                        .setContext(getApplicationContext())
                        .setContent(WebViewContent.html(y))
//                .setBitmapWidth(width)
//                .setMeasureDelay(10)
//                .setScreenshotDelay(10)
                        .setStrictMode(false)
                        .setTimeout(20)
//                .setTextZoom(150)
//                .setConfigurator(html2BitmapConfigurator)
                        .build();


                    try {
                        startPrinter();
                    } catch (RemoteException | IOException e) {
                        e.printStackTrace();
                    }

            }
        }).start();

    }

    public void testScan() throws RemoteException {
//        initRegister();
//        startFrontScan();

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

//        byte[] image = readAssetsFile(this, "DMGReceipt.png");
//        printer.addBmpImage(0, FactorMode.BMP1X1, image);
//        printer.feedLine(1);
//        printer.addBarCode(AlignMode.CENTER, 2, 48,  "ProductCode123");
//        printer.feedLine(5);
//        printer.startPrint(new OnPrintListener.Default() {
//            @Override
//            public void onFinish() throws RemoteException {
//                System.out.println("=> onFinish | printing");
//            }
//
//            @Override
//            public void onError(int error) throws RemoteException {
//                System.out.println("=> onError: " + error);
//            }
//        });
//        printer.cutPaper();


//        try {
//            printer = PaxPrint.getDal(getApplicationContext()).getPrinter();
//            printer.init();
//
//            byte[] image = readAssetsFile(this, "DMGReceipt.png");
//            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0,image.length);
//
//            printer.printBitmap(bitmap);
//            printer.printStr("\n", null);
//            printer.printStr("\n", null);
//            printer.printStr("Print Something",null);
//            printer.printStr("\n", null);
//            printer.start();
//        } catch (PrinterDevException e) {
//            e.printStackTrace();
//        }
    }

//    public void initRegister() {
//        DeviceHelper.me().register(true);
//    }

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
//        scanner = DeviceHelper.me().getScanner(CameraId.FRONT);
//        Bundle bundle = new Bundle();
//        bundle.putInt(ScannerData.TIMEOUT, 30);
//        scanner.startScan(bundle, new OnScanListener.Stub() {
//
//            @Override
//            public void onSuccess(String barcode) throws RemoteException {
//                Thread thread = new Thread(){
//                    public void run(){
//                        runOnUiThread(new Runnable() {
//                            public void run() {
//                                Toast toast = Toast.makeText(MainActivity.this, "Check logcat for code", Toast.LENGTH_LONG);
//                                View view = toast.getView();
//
//                                view.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);
//
//                                TextView text = view.findViewById(android.R.id.message);
//                                text.setTextColor(Color.WHITE);
//
////
//                                toast.show();
//                            }
//                        });
//                    }
//                };
//                thread.start();
//
//                System.out.println("SCANNER => " + barcode);
//                System.out.println("SCANNER => bytes data: " + BytesUtil.bytes2HexString(scanner.getRecentScanResult()));
//
//            }
//
//            @Override
//            public void onCancel() throws RemoteException {
//                System.out.println("SCANNER => onCancel");
//            }
//
//            @Override
//            public void onTimeout() throws RemoteException {
//                System.out.println("SCANNER => onTimeout");
//            }
//
//            @Override
//            public void onError(int error) throws RemoteException {
//                System.out.println("SCANNER => onError | " + DeviceHelper.me().getErrorDetail(error));
//            }
//        });

    }
}


