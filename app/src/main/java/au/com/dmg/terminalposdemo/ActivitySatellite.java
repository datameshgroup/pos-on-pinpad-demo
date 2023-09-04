package au.com.dmg.terminalposdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.adminrequest.AdminRequest;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.POIInformation;
import au.com.dmg.fusion.response.diagnosisresponse.DiagnosisResponse;

public class ActivitySatellite extends AppCompatActivity {
    private Button btnPrintLastCustomerReceipt;
    private Button btnPrintLastMerchantReceipt;
    private Button btnPrintShiftReport;
    private Button btnLaunchSatellite;
    private Button btnUpgrade;
    private Button btnTerminalInfo;
//    private final TerminalDevice device = new TerminalDevice();
    private TextView tvTerminalInfo;
    private int tapCount = 0;
    private long tapCounterStartMillis = 0;

    TerminalInfoReceiver terminalInfoReceiver = new TerminalInfoReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.showLog("TerminalPOSDemo", intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
            Message message = null;
            DiagnosisResponse diagnosisResponse = null;
            POIInformation terminalInformationResponse = null;
            try {
                message = Message.fromJson(intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
                terminalInformationResponse =  message.getResponse().getDiagnosisResponse().getExtensionData().getPoiInformation();

                String stringTerminalInfo = "TID: " + terminalInformationResponse.getTid()  + " \n" +
                        "MID: " + terminalInformationResponse.getMid() + " \n" +
                        "Address1: " + terminalInformationResponse.getAddressLocation().getAddress1() + " \n" +
                        "Address2: " + terminalInformationResponse.getAddressLocation().getAddress2() + " \n" +
                        "AddressState: " + terminalInformationResponse.getAddressLocation().getAddressState() + " \n" +
                        "Location: " + terminalInformationResponse.getAddressLocation().getLocation() + " \n" +
                        "SoftwareVersion: " + terminalInformationResponse.getSoftwareVersion(); // This will include app version + "-" + hash code
                tvTerminalInfo.setText(stringTerminalInfo);
            } catch (Exception e) {
                Utils.showLog("TerminalPOSDemo", "Error reading intent.");
                e.printStackTrace();
                return;
            }

            Log.d("TerminalPOSDemo", terminalInformationResponse.toString());
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

        tvTerminalInfo = findViewById(R.id.tvTerminalInfo);

        registerReceiver(terminalInfoReceiver,  new IntentFilter("fusion_broadcast_receiver"));
    }

    private void printLastCustomerReceipt() {
        Log.d("TerminalPOSDemo","Printing last customer receipt...");

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID("")
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
        Log.d("TerminalPOSDemo","Printing last merchant receipt...");

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID("")
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
        Log.d("TerminalPOSDemo","Printing shift report...");

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID("")
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
        Log.d("TerminalPOSDemo","Getting Terminal information...");


        SaleToPOIRequest terminalInfoRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Diagnosis)
                                .messageType(MessageType.Request)
                                .serviceID("")
                                .build()
                )
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message terminalInfoRequestMessage = new Message(terminalInfoRequest);
        Utils.showLog("terminalInfoRequestMessage", terminalInfoRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, terminalInfoRequestMessage.toJson());
        sendBroadcast(intent);
    }

    public void pullUpgrade() {
        Log.d("TerminalPOSDemo","Updating Satellite...");

        Intent intent = new Intent(Message.AXIS_PULL_UPDATE);

        // AXIS_RESULT_ACTIVITY = Activity to go back to after the update
//        System.out.println(getComponentName());
        intent.putExtra(Message.AXIS_RESULT_ACTIVITY, "au.com.dmg.terminalposdemo.ActivitySatellite");
//        startActivityForResult(intent,100);
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


