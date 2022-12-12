package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.RemoteException;

import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.IScanner;
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
    public String frontScanBarcode() throws RemoteException {
        String barcode = "";
        boolean isWaiting = true;

        barcode = "unavailable";
//        while(isWaiting){
//            System.out.println("SCANNER => waiting for code");
//        }
        return barcode;
    }
}
