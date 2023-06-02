package au.com.dmg.terminalposdemo.ble;

import android.content.Context;
import android.content.pm.PackageManager;
public class Helper {
    public static boolean checkBLE(Context context){
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static String getServiceName(String uuid){
        switch (uuid){
            case "00001800-0000-1000-8000-00805f9b34fb":{
                return "Generic Access";
            }
            case "00001801-0000-1000-8000-00805f9b34fb":{
                return "Generic Attribute";
            }
            case "00001111-0000-1000-8000-00805f9b34fb":{
                return "Notification Service";
            }
            case "49cef8b2-71f7-4e4f-8c5c-1f7c766addc8":{
                return "Custom Service";
            }

            default:
                return "Unknown";
        }
    }

}
