package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.UnitOfMeasure;
import au.com.dmg.fusion.request.SaleTerminalData;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.aborttransactionrequest.AbortTransactionRequest;
import au.com.dmg.fusion.request.paymentrequest.AmountsReq;
import au.com.dmg.fusion.request.paymentrequest.POITransactionID;
import au.com.dmg.fusion.request.paymentrequest.PaymentData;
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest;
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction;
import au.com.dmg.fusion.request.paymentrequest.SaleData;
import au.com.dmg.fusion.request.paymentrequest.SaleItem;
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID;
import au.com.dmg.fusion.request.paymentrequest.SponsoredMerchant;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.ExtensionData;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.Stop;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.TransitData;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.Trip;
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.util.BigDecimalAdapter;
import au.com.dmg.fusion.util.InstantAdapter;

public class ActivityPayment extends AppCompatActivity {

    BigDecimal bAmount = BigDecimal.valueOf(0);
    SaleToPOIResponse response = null;

    private ImageView ivScan;
    private Button btnPay;
    private Button btnAbort;
    private Button btnExtension;
    private Button btnAddSalteItems;
    private Button btnOtherFields;
    private TextView inputAmount;
    private TextView tvResults;
    private POITransactionID resPOI = null;
    private TextView txtProductCode = null;
    private TextView txtTransactionID = null;

    //scanner
    private TerminalDevice device = new TerminalDevice();

    private long pressedTime;

    String testServiceID;
    ExtensionData customExtensionData = null;

    //A2B-Specific Amounts
    BigDecimal lateNightFee = BigDecimal.valueOf(1.1);
    BigDecimal sampleLevy = BigDecimal.valueOf(10);

    //Additional A2B Required fields:
    String registeredIdentifier = "Taxi123"; //AKA SubMerchantID
    String operatorID = "TestOpID123";
    String businessID = "TestBusID123";
    String deviceID = generateRandomUUID();
    String shiftNumber = "123Shift";
    String siteID = "TestSitID123";
    String appName = "";
    String appVersion = "";

    Boolean pendingPartialPayment = false;
    BigDecimal remainingAmount;
    String pendingTransactionID;

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device.init(getApplicationContext());

        setContentView(R.layout.activity_payment);

        appName = getResources().getString(R.string.application_name);
        appVersion = getResources().getString(R.string.application_version);

        ivScan = (ImageView) findViewById(R.id.ivScan);
        ivScan.setOnClickListener(v -> {
            try {
                startScan();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        btnPay = (Button) findViewById(R.id.btnPay);
        btnPay.setOnClickListener(this::sendPaymentRequest);

        btnAbort = (Button) findViewById(R.id.btnAbort);
        btnAbort.setOnClickListener(this::testAbort);

        btnExtension = (Button) findViewById(R.id.btnExtensionData);
        btnExtension.setOnClickListener(this::viewExtensionData);

        btnAddSalteItems = (Button) findViewById(R.id.btnUpdateSaleItems);
        btnAddSalteItems.setOnClickListener(this::updateSalteItems);

        btnOtherFields = (Button) findViewById(R.id.btnOtherFields);
        btnOtherFields.setOnClickListener(this::viewOtherFields);

        inputAmount = (TextView) findViewById(R.id.inputTotal);

        tvResults = (TextView) findViewById(R.id.tvResults);
        txtProductCode =  (TextView) findViewById(R.id.txtProductCode);
        txtTransactionID = (TextView) findViewById(R.id.txtTransactionID);

        // TODO Add logic for saleitems
        // Check Partial Payment
        pendingPartialPayment = GlobalClass.PartialPayment.hasPendingPartial;
        if (pendingPartialPayment){
            pendingTransactionID = GlobalClass.PartialPayment.transactionID;
            remainingAmount = GlobalClass.PartialPayment.remainingAmount;

            btnPay.setText("Pay Remaining");
            inputAmount.setText(remainingAmount.toString());
            inputAmount.setFocusable(false);
            txtTransactionID.setText(pendingTransactionID);

        }else{
            txtTransactionID.setText(generateRandomUUID());
            inputAmount.setFocusable(true);
            btnPay.setText("PAY");
        }
    }

    private void updateSalteItems(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SaleItems");
        SaleItem saleItem;


        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_saleitems, null);
        builder.setView(customLayout);
//        EditText editText = customLayout.findViewById(R.id.etExtenstionData);

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "SaleItem/s not saved", Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
//            sendDialogDataToActivity(editText.getText().toString()); //TODO update this to updateSaleItems create new
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void viewOtherFields(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Other Fields");

        final View fieldsLayout = getLayoutInflater().inflate(R.layout.dialog_otherfields, null);

        EditText etRegisteredIdentifier = fieldsLayout.findViewById(R.id.inputRegisteredIdentifier);
        EditText etOperatorID = fieldsLayout.findViewById(R.id.inputOperatorID);
        EditText etDeviceID = fieldsLayout.findViewById(R.id.inputDeviceID);
        TextView tvDeviceID = fieldsLayout.findViewById(R.id.tvDeviceID);
        tvDeviceID.setOnClickListener(v -> etDeviceID.setText(generateRandomUUID()));
        EditText etShiftNumber = fieldsLayout.findViewById(R.id.inputShiftNumber);
        EditText etSiteID = fieldsLayout.findViewById(R.id.inputSiteID);

        etRegisteredIdentifier.setText(registeredIdentifier);
        etOperatorID.setText(operatorID);
        etDeviceID.setText(deviceID);
        etShiftNumber.setText(shiftNumber);
        etSiteID.setText(siteID);

        builder.setView(fieldsLayout);

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "Other fields not update", Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("Update", (dialog, which) -> {
            registeredIdentifier = String.valueOf(etRegisteredIdentifier.getText());
            operatorID = String.valueOf(etOperatorID.getText());
            deviceID = String.valueOf(etDeviceID.getText());
            shiftNumber = String.valueOf(etShiftNumber.getText());
            siteID = String.valueOf(etSiteID.getText());
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void viewExtensionData(View view)  {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("TransitData");
        ExtensionData extensionData;
                if(customExtensionData==null){
                    extensionData = createSampleExtensionData();
                }else{
                    extensionData = customExtensionData;
                }

        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_extensiondata, null);
        builder.setView(customLayout);
        EditText editText = customLayout.findViewById(R.id.etExtenstionData);

        JSONObject json;
        try {
            json = new JSONObject(printExtensionDatatoJson(extensionData));
            editText.setText(json.toString(2));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "TransitData not updated", Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            sendDialogDataToActivity(editText.getText().toString());
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendDialogDataToActivity(String data) {
        try {
            customExtensionData = buildExtensionDatafromJson(data);
            //Validate TransitData using builder
            TransitData td = new TransitData.Builder()
                    .isWheelchairEnabled(customExtensionData.getTransitData().getIsWheelchairEnabled())
                    .trip(customExtensionData.getTransitData().getTrip())
                    .tags(customExtensionData.getTransitData().getTags())
                    .build();
            //Validate Trip using builder
            Trip trip = new Trip.Builder()
                    .addStops(td.getTrip().getStops())
                    .totalDistanceTravelled(td.getTrip().getTotalDistanceTravelled())
                    .build();
            //Validate stops; This just checks for the first stop entry as a sample
            int stopsCount = trip.getStops().size();
            for(int x = 0; x < stopsCount; x++){
                Stop stop = new Stop.Builder()
                        .stopIndex(trip.getStops().get(x).getStopIndex())
                        .timestamp(trip.getStops().get(x).getTimestamp())
                        .build();
            }
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            customExtensionData = null;
            Toast.makeText(this, "Invalid TransitData. Ignoring.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler barcodeHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                txtProductCode.setText(msg.obj.toString());
            }
        };
    };

    public void startScan() throws RemoteException {
        try {
            device.scanBarcode(barcodeHandler, 30, TerminalDevice.camera_front);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void testAbort(View view) {
        //This simulates an abort request during a payment. For this code, we delay the intentCancel

        this.testServiceID = generateRandomUUID();
        //create payment request first
        SaleToPOIRequest paymentRequest = buildPaymentRequest(testServiceID);

        Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);

        // wrapper of request.
        Message message = new Message(paymentRequest);
        Utils.showLog("Request", message.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, appName);
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, appVersion);

        //create abort request
        SaleToPOIRequest abortRequest = buildAbortRequest(testServiceID);
        Intent intentCancel = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message messageCancel = new Message(abortRequest);
        Utils.showLog("AbortRequest", messageCancel.toJson());
        intentCancel.putExtra(Message.INTENT_EXTRA_MESSAGE, messageCancel.toJson());
        intentCancel.putExtra(Message.RETURN_TO_PACKAGE, this.getPackageName());

        startActivityForResult(intent, 1);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                sendBroadcast(intentCancel);
            }
        }, 10000);
    }

    private SaleToPOIRequest buildPaymentRequest(String serviceID) {

        SaleToPOIRequest paymentRequest;
        ExtensionData extensionData;

        if(customExtensionData==null){
            extensionData = createSampleExtensionData();
        }else{
            extensionData = customExtensionData;
        }

        bAmount = new BigDecimal(inputAmount.getText().toString());

        //Request creation
        paymentRequest = new SaleToPOIRequest.Builder()
                .messageHeader(new MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Payment)
                        .messageType(MessageType.Request)
                        .serviceID(serviceID)
                        .saleID("test")
                        .build())
                .request(new PaymentRequest.Builder()
                        .saleData(new SaleData.Builder()
                                .operatorLanguage("en")
                                .operatorID(operatorID)
                                .saleTransactionID(new SaleTransactionID.Builder()
                                        .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                        .transactionID(generateRandomUUID())
                                        .build())
                                .sponsoredMerchant(new SponsoredMerchant.Builder()
                                        .siteID(siteID)
                                        .businessID(businessID)
                                        .registeredIdentifier(registeredIdentifier)
                                        .build())
                                .shiftNumber(shiftNumber)
                                .saleTerminalData(new SaleTerminalData.Builder()
                                        .deviceID(deviceID)
                                        .build())
                                .build())
                        .paymentTransaction(
                                new PaymentTransaction.Builder()
                                        .amountsReq(new AmountsReq.Builder()
                                                .currency("AUD")
                                                .requestedAmount(bAmount) //Total of all sale items
                                                .tipAmount(BigDecimal.valueOf(0))
                                                .cashBackAmount(BigDecimal.valueOf(0))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(1)
                                                .productCode("MeteredFare")
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(BigDecimal.valueOf(3.9))
                                                .unitPrice(BigDecimal.valueOf(3.9))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("TARIFF 3")
                                                .tags(Arrays.asList(new String[]{""}))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(100)
                                                .productCode("Levy")
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(BigDecimal.valueOf(1.1))
                                                .unitPrice(BigDecimal.valueOf(1.1))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("Levy")
                                                .tags(Arrays.asList(new String[]{"extra"}))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(205)
                                                .productCode("Lifting Fee")
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(BigDecimal.valueOf(20.0))
                                                .unitPrice(BigDecimal.valueOf(20.0))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("Lifting Fee")
                                                .tags(Arrays.asList(new String[]{"extra"}))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(204)
                                                .productCode("Cleaning Fee")
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(BigDecimal.valueOf(120.0))
                                                .unitPrice(BigDecimal.valueOf(120.0))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("Cleaning Fee")
                                                .tags(Arrays.asList(new String[]{"extra"}))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(203)
                                                .productCode("HOV")
                                                .unitOfMeasure(UnitOfMeasure.Other)
                                                .itemAmount(BigDecimal.valueOf(5.0))
                                                .unitPrice(BigDecimal.valueOf(5.0))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("HOV")
                                                .tags(Arrays.asList(new String[]{"extra"}))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(201)
                                                .productCode("Peak")
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(BigDecimal.valueOf(2.5))
                                                .unitPrice(BigDecimal.valueOf(2.5))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("Peak")
                                                .tags(Arrays.asList(new String[]{"extra"}))
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(202)
                                                .productCode("Airport")
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(BigDecimal.valueOf(20.0))
                                                .unitPrice(BigDecimal.valueOf(20.0))
                                                .quantity(new BigDecimal(1))
                                                .productLabel("Airport")
                                                .tags(Arrays.asList(new String[]{"extra"}))
                                                .build())
                                        .build()
                        )
                        .paymentData(new PaymentData.Builder()
                                .paymentType(PaymentType.Normal)
                                .build())
                        .extensionData(extensionData)
                        .build()
                )
                .build();
        return paymentRequest;
    }

    private void sendPaymentRequest(View view) {
        this.testServiceID = generateRandomUUID();
        SaleToPOIRequest request = buildPaymentRequest(testServiceID);
        sendRequest(request);
    }

    private SaleToPOIRequest buildAbortRequest(String refServiceID) {

        // Abort Request
        MessageReference messageReference = new MessageReference.Builder()//
                .messageCategory(MessageCategory.Abort)
                .saleID("SaleIDHere")
                .POIID("POIIDHere")
                .serviceID(refServiceID)
                .build();
        AbortTransactionRequest abortTransactionRequest = new AbortTransactionRequest(messageReference, "User Cancel");

        SaleToPOIRequest abortRequest = new SaleToPOIRequest.Builder()
                .messageHeader(new MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Abort)
                        .messageType(MessageType.Request)
                        .serviceID(generateRandomUUID())
                        .build())
                .request(abortTransactionRequest)
                .build();

        return abortRequest;
    }

    private String generateRandomUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    private void sendRequest(SaleToPOIRequest request) {
        Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);

        // wrapper of request.
        Message message = new Message(request);
        Utils.showLog("Request", message.toJson());

        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
        // name of this app, that gets treated as the POS label by the terminal.
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, appName);
        // version of of this POS app.
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, appVersion);

        startActivityForResult(intent, 100);
    }


    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        if (data != null && data.hasExtra(Message.INTENT_EXTRA_MESSAGE)) {
            this.handleResponseIntent(data);
        }
    }

    private void handleResponseIntent(Intent intent) {
        Utils.showLog("Response", intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
        Message message = null;
        try {
            message = Message.fromJson(intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
        } catch (Exception e) {
            Toast.makeText(this, "Error reading intent.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        handleResponse(message);
    }


    private void handleResponse(Message message) {
        this.response = message.getResponse();

        if(response != null) {
            try {
                TextView textViewJson = findViewById(R.id.tvResults);
                Utils.showLog("Response", response.toJson());
                textViewJson.setText(response.toJson()); //prints to payment page

                GlobalClass globalClass = (GlobalClass)getApplicationContext();
                globalClass.setResponse(response);

                //parse response
                MessageHeader mh = response.getMessageHeader();
                MessageCategory mc = mh.getMessageCategory();

                openActivityResult(mc, response, message);

                }
                catch (Exception e){
                    Utils.showLog("Error", "Invalid Response ==>" + e.getMessage() );
                }

        }
    }
    ExtensionData buildExtensionDatafromJson(String jsonString) throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();

        JsonAdapter<ExtensionData> jsonAdapter = moshi.adapter(ExtensionData.class);
        return jsonAdapter.nonNull().fromJson(jsonString);
    }

    public String printExtensionDatatoJson(ExtensionData extensionData) {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();
        JsonAdapter<ExtensionData> jsonAdapter = moshi.adapter(ExtensionData.class);
        return jsonAdapter.toJson(extensionData);
    }

    public ExtensionData createSampleExtensionData(){
        String tagsString = "NSWAllowTSSSubsidy, NSWAllowTSSLift";
        List<String> tags = Arrays.asList(tagsString.split("\\s*,\\s*"));

        return new ExtensionData.Builder().transitData(
                        new TransitData.Builder()
                                .isWheelchairEnabled(false)
                                .trip(new Trip.Builder()
                                        .totalDistanceTravelled(new BigDecimal("222.22"))
                                        .addStop(new Stop.Builder()
                                                .stopIndex(0)
                                                .stopID("0")
                                                .stopName("test0")
                                                .latitude(new BigDecimal(3432423))
                                                .longitude(new BigDecimal(-3432423))
                                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                                .build())
                                        .addStop(new Stop.Builder()
                                                .stopIndex(1)
                                                .stopID("1")
                                                .stopName("test1")
                                                .latitude(new BigDecimal(3432423))
                                                .longitude(new BigDecimal(-3432423))
                                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                                .build())
                                        .build())
                                .tags(tags)
                                .build())
                .build();
    }
    public void openActivityResult(MessageCategory mc, SaleToPOIResponse r, Message message) {
        Intent intent = new Intent(this, ActivityResult.class);

        Bundle bundle = new Bundle();
        bundle.putSerializable("messageCategory", mc);
        bundle.putString("message", message.toString());
        intent.putExtras(bundle);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("prevClass", this.getClass());
        startActivity(intent);
    }
    public void TransactionID_OnClick(View view) {
        if(!pendingPartialPayment){
            txtTransactionID.setText(generateRandomUUID());
        }else{
            Toast.makeText(getBaseContext(), "Complete pending partial payment first before generating a new TransactionID", Toast.LENGTH_LONG).show();
        }

    }
}