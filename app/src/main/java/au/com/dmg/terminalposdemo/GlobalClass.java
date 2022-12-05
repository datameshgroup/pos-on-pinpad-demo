package au.com.dmg.terminalposdemo;

import android.app.Application;

public class GlobalClass extends Application {
    private String gLasttxnID;
    private String gLastServiceID;

    public String getgLasttxnID(){
        return gLasttxnID;
    }
    public String getgLastServiceID(){
        return gLastServiceID;
    }

    public void setgLasttxnID(String gLasttxnID){
        this.gLasttxnID = gLasttxnID;
    }
    public void setgLastServiceID(String gLastServiceID){
        this.gLastServiceID = gLastServiceID;
    }


}
