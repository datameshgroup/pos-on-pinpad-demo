package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;

import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.IScanner;
import com.pax.dal.entity.EScannerType;
import com.pax.dal.entity.ScanResult;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.neptunelite.api.NeptuneLiteUser;


public class DMGDeviceImpl implements DeviceInterface {
    IPrinter printer;
    IScanner scanner;

    private static IDAL dal;

    public static IDAL getDal(Context context) {
        if(dal == null){ //dal is a private static IDAL variable of the application
            try {
                dal = NeptuneLiteUser.getInstance().getDal(context);
            } catch (Exception e) {}
        }
        return dal;
    }

    @Override
    public void init(Context context) {
        printer = getDal(context).getPrinter();
        scanner = getDal(context).getScanner(EScannerType.FRONT);
    }

    @Override
    public boolean isisPrinterSupported() {
        return false;
    }

    @Override
    public boolean isPrinterPaperAvailable() {
        return false;
    }

    @Override
    public void printBitmap(Bitmap bitmap) throws RemoteException {
        try {
            printer.init();
            printer.printBitmap(bitmap);
            printer.start();
        } catch (PrinterDevException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void frontScanBarcode(final Handler handler, int timeout) throws RemoteException {
        final boolean[] isWaiting = {true};
        final String[] scanCode = {""};
        scanner.open();
        scanner.setTimeOut(timeout);
//        scanner.setContinuousTimes(5);
//        scanner.setContinuousInterval(1000);

        scanner.start(new IScanner.IScanListener() {
            @Override
            public void onRead(ScanResult scanResult) {
                System.out.println("SCANNER => onRead");
                System.out.println("SCANNER => result: " +scanResult.getContent());
                scanCode[0] = scanCode[0] + scanResult.getContent();

                Message message = Message.obtain();
                message.what = 0;
                message.obj = scanResult.getContent();
                handler.sendMessage(message);
            }

            @Override
            public void onFinish() {
                System.out.println("SCANNER => onFinish");
                System.out.println("SCANNER => result: " + scanCode[0]);
                isWaiting[0] = false;
                scanner.close();
            }

            @Override
            public void onCancel() {
                System.out.println("SCANNER => onCancel");
                scanner.close();
            }
        });

    }
}
