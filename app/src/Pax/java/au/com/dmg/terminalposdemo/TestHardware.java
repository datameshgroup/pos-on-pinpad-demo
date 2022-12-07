package au.com.dmg.terminalposdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;


import com.pax.dal.IPrinter;
import com.pax.dal.IScanner;
import com.pax.dal.exceptions.PrinterDevException;

import java.io.IOException;
import java.io.InputStream;

import au.com.dmg.terminalposdemo.Util.PaxPrint;

public class TestHardware {
    IPrinter printer;
    IScanner scanner;

    void initDevice(Context context) {
        System.out.println("No init required for Pax devices");
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
        byte[] image = readAssetsFile(a.getApplicationContext(), "DMGReceipt.png");
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0,image.length);
        String x = "Test print";
        try {
            printer = PaxPrint.getDal(a.getApplicationContext()).getPrinter();
            printer.init();
            printer.printBitmap(bitmap);
            printer.printStr(x,null);
            printer.start();
        } catch (PrinterDevException e) {
            e.printStackTrace();
        }
    }

    public void startFrontScan() throws RemoteException {
        System.out.println("Scanner not yet available for Pax");
    }
}
