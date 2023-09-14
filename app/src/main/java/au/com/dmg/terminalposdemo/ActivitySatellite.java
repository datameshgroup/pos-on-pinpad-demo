package au.com.dmg.terminalposdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.ServiceIdentification;
import au.com.dmg.fusion.data.TerminalEnvironment;
import au.com.dmg.fusion.request.SaleTerminalData;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.adminrequest.AdminRequest;
import au.com.dmg.fusion.request.loginrequest.LoginRequest;
import au.com.dmg.fusion.request.loginrequest.SaleSoftware;
import au.com.dmg.fusion.response.LoginResponse;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.adminresponse.AdminResponse;
import au.com.dmg.fusion.response.diagnosisresponse.DiagnosisResponse;
import au.com.dmg.fusion.response.responseextensiondata.POIInformation;

public class ActivitySatellite extends AppCompatActivity {
    private Button btnPrintLastCustomerReceipt;
    private Button btnPrintLastMerchantReceipt;
    private Button btnPrintShiftReport;
    private Button btnLaunchSatellite;
    private Button btnUpgrade;
    private Button btnTerminalInfo;
    private Button btnLogon;
//    private final TerminalDevice device = new TerminalDevice();
    private TextView tvInfo;
    private int tapCount = 0;
    private long tapCounterStartMillis = 0;

    private String sentServiceID = "";

    TerminalInfoReceiver terminalInfoReceiver = new TerminalInfoReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.showLog("TerminalPOSDemo", "broadcast" + intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
            //TODO switch case here admin, login

            String responseInfo = "";
            Message message = null;
            SaleToPOIResponse response = null;

            DiagnosisResponse diagnosisResponse = null;
            try {
                message = Message.fromJson(intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));

                response = message.getResponse();
                MessageCategory mc = response.getMessageHeader().getMessageCategory();
                String receivedServiceID = message.getResponse().getMessageHeader().getServiceID();
                Utils.showLog("TerminalPOSDemo", "received serviceID:" + receivedServiceID);

                switch (mc){
                    case Diagnosis:
                        POIInformation poiInformation =  response.getDiagnosisResponse().getExtensionData().getPoiInformation();
                        responseInfo = "ServiceID: " + receivedServiceID  + " \n" +
                                "TID: " + poiInformation.getTid()  + " \n" +
                                "MID: " + poiInformation.getMid() + " \n" +
                                "Address1: " + poiInformation.getAddressLocation().getAddress1() + " \n" +
                                "Address2: " + poiInformation.getAddressLocation().getAddress2() + " \n" +
                                "AddressState: " + poiInformation.getAddressLocation().getAddressState() + " \n" +
                                "Location: " + poiInformation.getAddressLocation().getLocation() + " \n" +
                                "SoftwareVersion: " + poiInformation.getSoftwareVersion(); // This will include app version + "-" + hash code
                        break;
                    case Admin:
                        AdminResponse adminResponse = response.getAdminResponse();
                        responseInfo = "ServiceID: " + receivedServiceID + " \n" +
                                        "Result: " + adminResponse.getResponse().getResult() + " \n" +
                                        "ErrorCondition: " + adminResponse.getResponse().getErrorCondition() + " \n" +
                                        "AdditionalResponse: " + adminResponse.getResponse().getAdditionalResponse();
                        break;
                    case Login:
                        LoginResponse loginResponse = response.getLoginResponse();
                        responseInfo = "ServiceID: " + receivedServiceID + " \n" +
                                "Result: " + loginResponse.getResponse().getResult() + " \n" +
                                "ErrorCondition: " + loginResponse.getResponse().getErrorCondition() + " \n" +
                                "AdditionalResponse: " + loginResponse.getResponse().getAdditionalResponse();
                        break;
                }
                tvInfo.setText(responseInfo);
            } catch (Exception e) {
                Utils.showLog("TerminalPOSDemo", "Error reading intent broadcast. : " + intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
                e.printStackTrace();
                return;
            }
            Log.d("TerminalPOSDemo", responseInfo);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        device.init(getApplicationContext());

        setContentView(R.layout.activity_satellite);

        btnPrintLastCustomerReceipt = (Button) findViewById(R.id.btnPrintLastCustomerReceipt);
        btnPrintLastCustomerReceipt.setOnClickListener(v -> printLastCustomerReceipt());

        btnPrintLastMerchantReceipt = (Button) findViewById(R.id.btnPrintLastMerchantReceipt);
        btnPrintLastMerchantReceipt.setOnClickListener(v -> printLastMerchantReceipt());

        btnPrintShiftReport = (Button) findViewById(R.id.btnPrintShiftReport);
        btnPrintShiftReport.setOnClickListener(v -> printShiftReport());

        btnLaunchSatellite = (Button) findViewById(R.id.btnLaunchSatellite);
        btnLaunchSatellite.setOnClickListener(v -> launchSatelliteApp());

        btnUpgrade = (Button) findViewById(R.id.btnUpgrade);
        btnUpgrade.setOnClickListener(v -> pullUpgrade());

        btnTerminalInfo = (Button) findViewById(R.id.btnTerminalInfo);
        btnTerminalInfo.setOnClickListener(v -> getTerminalInformation());

        btnLogon = (Button) findViewById(R.id.btnLogon);
        btnLogon.setOnClickListener(v -> dologon());

        tvInfo = findViewById(R.id.tvInfo);

        registerReceiver(terminalInfoReceiver,  new IntentFilter("fusion_broadcast_receiver"));
    }

    private void dologon() {
        this.sentServiceID = generateServiceID();

        Log.d("TerminalPOSDemo","Sending Login Request...ServiceID: " + this.sentServiceID);
        SaleToPOIRequest loginRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Login)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .saleID("")
                                .POIID("")
                                .build()

                ).request(new LoginRequest.Builder()
                        .dateTime("")
                        .saleSoftware(new SaleSoftware.Builder()
                                .providerIdentification("")
                                .applicationName("TerminalPOSDemo")
                                .softwareVersion("")
                                .certificationCode("")
                                .build()
                        )
                        .saleTerminalData(new SaleTerminalData.Builder()
                                .terminalEnvironment(TerminalEnvironment.Attended)
                                .build())
                        .operatorLanguage("en")
                        .build())
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message loginRequestMessage = new Message(loginRequest);
        Utils.showLog("loginRequestMessage", loginRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, loginRequestMessage.toJson());
        sendBroadcast(intent);
    }

    private void printLastCustomerReceipt() {
        this.sentServiceID = generateServiceID();
        Log.d("TerminalPOSDemo","Printing last customer receipt...ServiceID: " + this.sentServiceID);
        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .build()
                )
                .request(new AdminRequest.Builder()
                        .serviceIdentification(ServiceIdentification.PrintLastCustomerReceipt)
                        .build())
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message adminRequestMessage = new Message(adminRequest);
        Utils.showLog("adminRequestMessage", adminRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, adminRequestMessage.toJson());
        sendBroadcast(intent);
    }

    private void printLastMerchantReceipt() {
        this.sentServiceID = generateServiceID();
        Log.d("TerminalPOSDemo","Printing last merchant receipt...ServiceID: " + this.sentServiceID);

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .build()
                )
                .request(new AdminRequest.Builder()
                        .serviceIdentification(ServiceIdentification.PrintLastMerchantReceipt)
                        .build())
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message adminRequestMessage = new Message(adminRequest);
        Utils.showLog("adminRequestMessage", adminRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, adminRequestMessage.toJson());
        sendBroadcast(intent);
    }

    private void printShiftReport() {
        this.sentServiceID = generateServiceID();
        Log.d("TerminalPOSDemo","Printing shift report...ServiceID: " + this.sentServiceID);

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .build()
                )
                .request(new AdminRequest.Builder()
                        .serviceIdentification(ServiceIdentification.PrintShiftTotals)
                        .build())
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message adminRequestMessage = new Message(adminRequest);
        Utils.showLog("adminRequestMessage", adminRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, adminRequestMessage.toJson());
        sendBroadcast(intent);
    }


    private void launchSatelliteApp() {
        Log.d("TerminalPOSDemo","Launching Satellite app...");

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(Message.AXISPAY_PACKAGE_NAME, "au.com.dmg.axispay.MainActivity"));
        startActivity(intent);
    }

    private void getTerminalInformation() {
        this.sentServiceID = generateServiceID();
        Log.d("TerminalPOSDemo","Getting Terminal information...ServiceID: " + this.sentServiceID);


        SaleToPOIRequest terminalInfoRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Diagnosis)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .build()
                )
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message terminalInfoRequestMessage = new Message(terminalInfoRequest);
        Utils.showLog("terminalInfoRequestMessage", terminalInfoRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, terminalInfoRequestMessage.toJson());
        sendBroadcast(intent);
    }
    private String generateServiceID() {
        return java.util.UUID.randomUUID().toString();
    }

    public void pullUpgrade() {
        Log.d("TerminalPOSDemo","Updating Satellite...");

        Intent intent = new Intent(Message.AXIS_PULL_UPDATE);

        // AXIS_RESULT_ACTIVITY = Activity to go back to after the update
        intent.putExtra(Message.AXIS_RESULT_ACTIVITY, "au.com.dmg.terminalposdemo.ActivitySatellite");
        startActivity(intent);
    }

//detect any touch event in the screen (instead of an specific view)

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventAction = event.getAction();
        if (eventAction == MotionEvent.ACTION_UP) {
            long time = System.currentTimeMillis();
            if (tapCounterStartMillis == 0 || (time - tapCounterStartMillis > 3000)) {
                tapCounterStartMillis = time;
                tapCount = 1;
            } else {
                tapCount++;
            }
            if (tapCount == 5) {
                forceExitApplication();
            }
            return true;
        }
        return false;
    }

    private void forceExitApplication() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Closing application...");
        builder.setMessage("Do you want to close the application?");
        builder.setPositiveButton("YES", (dialog, which) -> {
            this.finish();
            System.exit(0);
        });
        builder.setNegativeButton("NO", (dialog, which) -> {
            Toast.makeText(this, "Close cancelled", Toast.LENGTH_SHORT).show();
        });
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(terminalInfoReceiver);
    }
}


