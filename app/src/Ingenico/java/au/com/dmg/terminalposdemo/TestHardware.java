package au.com.dmg.terminalposdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

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

import au.com.dmg.terminalposdemo.Ingenico.BytesUtil;
import au.com.dmg.terminalposdemo.Ingenico.DeviceHelper;

public class TestHardware {
    private UPrinter printer;
    private UScanner scanner;

    void initDevice(Context context) {
        DeviceHelper.me().init(context);
        DeviceHelper.me().bindService();
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
    public void startPrinter(Activity a) throws RemoteException, IOException {
        DeviceHelper.me().register(true);
        printer = DeviceHelper.me().getPrinter();
        byte[] image = readAssetsFile(a.getApplicationContext(), "DMGReceipt.png");
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

    public String startFrontScan() throws RemoteException {
        DeviceHelper.me().register(true);
        final String[] productCode = {""};
        scanner = DeviceHelper.me().getScanner(CameraId.FRONT);
        Bundle bundle = new Bundle();
        bundle.putInt(ScannerData.TIMEOUT, 30);
        scanner.startScan(bundle, new OnScanListener.Stub() {

            @Override
            public void onSuccess(String barcode) throws RemoteException {
                System.out.println("SCANNER => " + barcode);
                System.out.println("SCANNER => bytes data: " + BytesUtil.bytes2HexString(scanner.getRecentScanResult()));
                productCode[0] = barcode;
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
        return productCode[0];
    }
}
