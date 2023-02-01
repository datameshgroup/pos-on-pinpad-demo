package au.com.dmg.devices.Util;

import android.content.Context;

import com.pax.dal.IDAL;
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
