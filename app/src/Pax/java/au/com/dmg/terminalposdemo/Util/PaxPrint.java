package au.com.dmg.terminalposdemo.Util;

import android.content.Context;

import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.neptunelite.api.NeptuneLiteUser;

public class PaxPrint {
    private static IDAL dal;
    
    public static IDAL getDal(Context context) {
        if(dal == null){ //dal is a private static IDAL variable of the application
            try {
                dal = NeptuneLiteUser.getInstance().getDal(context);
            } catch (Exception e) {}
        }
        return dal;
    }
}
