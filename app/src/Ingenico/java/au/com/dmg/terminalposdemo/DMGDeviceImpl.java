package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.graphics.Bitmap;
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

import java.io.ByteArrayOutputStream;

import au.com.dmg.terminalposdemo.Util.BytesUtil;
import au.com.dmg.terminalposdemo.Util.DeviceHelper;

public class DMGDeviceImpl implements DeviceInterface {
    private UPrinter printer;
    private UScanner scanner;

    @Override
    public void init(Context context) {
        DeviceHelper.me().init(context);
        DeviceHelper.me().bindService();
    }

    @Override
    public boolean isisPrinterSupported() {
        return false;
    }

    @Override
    public boolean isPrinterPaperAvailable() {
        return false;
    }

    public void printBitmap(Bitmap bitmap) throws RemoteException {
        DeviceHelper.me().register(true);
        printer = DeviceHelper.me().getPrinter();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        printer.addBmpImage(0, FactorMode.BMP1X1, byteArray);
//        printer.startPrint(null);

        printer.feedLine(1);
//        printer.addBarCode(AlignMode.CENTER, 2, 48,  "ProductCode123");
//        printer.feedLine(5);
        printer.startPrint(new OnPrintListener.Default() {
            @Override
            public void onFinish() {
                System.out.println("=> onFinish | printing");
            }

            @Override
            public void onError(int error) {
                System.out.println("=> onError: " + error);
            }
        });
    }

    @Override
    public String frontScanBarcode() throws RemoteException{
        DeviceHelper.me().register(true);
        scanner = DeviceHelper.me().getScanner(CameraId.FRONT);

        final boolean[] isWaiting = {true};
        final String[] scanCode = {""};
        Bundle bundle = new Bundle();
        bundle.putInt(ScannerData.TIMEOUT, 30);
        scanner.startScan(bundle, new OnScanListener.Stub() {

            @Override
            public void onSuccess(String barcode) throws RemoteException {
                System.out.println("SCANNER => " + barcode);
                System.out.println("SCANNER => bytes data: " + BytesUtil.bytes2HexString(scanner.getRecentScanResult()));
                scanCode[0] = barcode;
                isWaiting[0] = false;
            }

            @Override
            public void onCancel() throws RemoteException {
                System.out.println("SCANNER => onCancel");
            }

            @Override
            public void onTimeout() throws RemoteException {
                System.out.println("SCANNER => onTimeout");
                isWaiting[0] = false;
            }

            @Override
            public void onError(int error) throws RemoteException {
                System.out.println("SCANNER => onError | " + DeviceHelper.me().getErrorDetail(error));
                isWaiting[0] = false;
            }
        });
        while(isWaiting[0]){
            System.out.println("SCANNER => waiting for code");
        }
        return scanCode[0];
    }



}
