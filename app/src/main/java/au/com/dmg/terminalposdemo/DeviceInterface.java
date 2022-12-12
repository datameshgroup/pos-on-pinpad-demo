package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.RemoteException;

import java.io.IOException;

public interface DeviceInterface {
    void init(Context context);
    boolean isisPrinterSupported();
    boolean isPrinterPaperAvailable();
    void printBitmap(Bitmap bitmap) throws RemoteException;

    String frontScanBarcode() throws RemoteException;
}
