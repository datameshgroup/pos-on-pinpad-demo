package au.com.dmg.terminalposdemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.CustomFieldType;
import au.com.dmg.fusion.data.DocumentQualifier;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.UnitOfMeasure;
import au.com.dmg.fusion.request.SaleTerminalData;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.aborttransactionrequest.AbortTransactionRequest;
import au.com.dmg.fusion.request.paymentrequest.AmountsReq;
import au.com.dmg.fusion.request.paymentrequest.CustomField;
import au.com.dmg.fusion.request.paymentrequest.POITransactionID;
import au.com.dmg.fusion.request.paymentrequest.PaymentData;
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest;
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction;
import au.com.dmg.fusion.request.paymentrequest.SaleData;
import au.com.dmg.fusion.request.paymentrequest.SaleItem;
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID;
import au.com.dmg.fusion.request.paymentrequest.SponsoredMerchant;
import au.com.dmg.fusion.request.paymentrequest.TransactionConditions;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.ExtensionData;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.Stop;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.TransitData;
import au.com.dmg.fusion.request.paymentrequest.extenstiondata.Trip;
import au.com.dmg.fusion.request.printrequest.OutputContent;
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentReceipt;
import au.com.dmg.fusion.util.BigDecimalAdapter;
import au.com.dmg.fusion.util.InstantAdapter;

public class ActivityPayment extends AppCompatActivity {

    BigDecimal bAmount = BigDecimal.valueOf(0);
    SaleToPOIResponse response = null;
    private Button btnPay;
    private Button btnAbort;
    private Button btnTripData;
    private Button btnAddSalteItems;
    private Button btnOtherFields;
    private TextView inputAmount;
    private TextView tvResults;
    private POITransactionID resPOI = null;
    private TextView txtTransactionID = null;

    //scanner
    private TerminalDevice device = new TerminalDevice();

    private long pressedTime;

    String testServiceID;
    Trip customTripData = null;

    SaleItem customSaleItem = null;

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

    EditText inputLift;
    List<String> selectedTags;
    private TextView inputODBS;
    private EditText inputPaymentBrand;
    private EditText inputCustomFooter;

    private SwitchMaterial switchWheelchair;
    private SwitchMaterial switchSubsidy;
    private SwitchMaterial switchLift;
    private Spinner spinnerState;
    private TextView tvSubsidiesHeader;
    private LinearLayout layoutSubsidies;

    // State management
    private String selectedState = "";
    private boolean isSubsidiesExpanded = false;

    // State constants
    private static final String[] STATES = {
            "Select State...",
            "NSW - New South Wales",
            "VIC - Victoria",
            "QLD - Queensland",
            "SA - South Australia",
            "WA - Western Australia",
            "TAS - Tasmania",
            "NT - Northern Territory",
            "ACT - Australian Capital Territory"
    };

    private static final String[] STATE_CODES = {
            "", "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT"
    };

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


    // Add these class variables after the existing declarations
    private int requestCounter = 0;
    private int successCounter = 0;
    private boolean isProcessingMultipleRequests = false;
    private static final int MULTIPLE_REQUEST_BASE = 200;
    private static final int MULTIPLE_REQUEST_COUNT = 100;
    private Button btnSend100Payments;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device.init(getApplicationContext());

        setContentView(R.layout.activity_payment);

        appName = getResources().getString(R.string.application_name);
        appVersion = getResources().getString(R.string.application_version);

        btnPay = (Button) findViewById(R.id.btnPay);
        btnPay.setOnClickListener(this::sendPaymentRequest);

        btnAbort = (Button) findViewById(R.id.btnAbort);
        btnAbort.setOnClickListener(this::testAbort);

        btnTripData = (Button) findViewById(R.id.btnTripData);
        btnTripData.setOnClickListener(this::viewTripData);

        btnAddSalteItems = (Button) findViewById(R.id.btnUpdateSaleItems);
        btnAddSalteItems.setOnClickListener(this::viewSaleItem);

        btnOtherFields = (Button) findViewById(R.id.btnSaleDataFields);
        btnOtherFields.setOnClickListener(this::viewOtherFields);
        
        btnSend100Payments = (Button) findViewById(R.id.btnSend100Payments);
        if (btnSend100Payments != null) {
            btnSend100Payments.setOnClickListener(this::send100PaymentRequests);
        }

        inputAmount = (TextView) findViewById(R.id.inputTotal);

        tvResults = (TextView) findViewById(R.id.tvResults);
        txtTransactionID = (TextView) findViewById(R.id.txtTransactionID);

        inputODBS = findViewById(R.id.inputODBS);
        inputPaymentBrand = findViewById(R.id.inputPaymentBrand);
        inputCustomFooter = findViewById(R.id.inputCustomFooter);
        inputCustomFooter.setText("line1<br/>line2<br/>line3<br/>");

        inputLift = findViewById(R.id.inputLift);

        switchWheelchair = findViewById(R.id.switchWheelchair);
        switchSubsidy = findViewById(R.id.switchSubsidy);
        switchLift = findViewById(R.id.switchLift);
        spinnerState = findViewById(R.id.spinnerState);
        tvSubsidiesHeader = findViewById(R.id.tvSubsidiesHeader);
        layoutSubsidies = findViewById(R.id.layoutSubsidies);

        // Subsidies expandable section
        tvSubsidiesHeader.setOnClickListener(this::toggleSubsidiesSection);
        switchLift.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLiftInputVisibility();
        });
        setupStateSpinner();

        selectedTags = new ArrayList<>();

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
            inputAmount.setText(getRandomAmount());
            inputAmount.setFocusable(true);
            btnPay.setText("PAY");
        }
    }

    private void setupStateSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, STATES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerState.setAdapter(adapter);

        // State spinner listener
        spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedState = position > 0 ? STATE_CODES[position] : "";
                updateSubsidyAvailability();
                updateLiftInputVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedState = "";
                updateLiftInputVisibility();
            }
        });
    }

    private void toggleSubsidiesSection(View view) {
        isSubsidiesExpanded = !isSubsidiesExpanded;

        if (isSubsidiesExpanded) {
            layoutSubsidies.setVisibility(View.VISIBLE);
            tvSubsidiesHeader.setText("Transport Subsidies ▲");
        } else {
            layoutSubsidies.setVisibility(View.GONE);
            tvSubsidiesHeader.setText("Transport Subsidies ▼");
        }
    }

    private void updateSubsidyAvailability() {
        boolean hasStateSelected = !selectedState.isEmpty();

        // Enable/disable subsidy controls based on state selection
        switchSubsidy.setEnabled(hasStateSelected);
        switchLift.setEnabled(hasStateSelected);

        if (!hasStateSelected) {
            switchSubsidy.setChecked(false);
            switchLift.setChecked(false);
        }
    }

    private void updateLiftInputVisibility() {
        boolean showACTLift = "ACT".equals(selectedState) && switchLift.isChecked();
        boolean showTASLift = "TAS".equals(selectedState) && switchLift.isChecked();

        inputLift.setVisibility((showACTLift || showTASLift )? View.VISIBLE : View.GONE);

        // Clear the input when hiding
        if (!showACTLift && !showTASLift) {
            inputLift.setText("");
        }

        // Set default values when showing
        if (showACTLift && inputLift.getText().toString().isEmpty()) {
            inputLift.setText("2500");
        }
        if (showTASLift && inputLift.getText().toString().isEmpty()) {
            inputLift.setText("2500");
        }
    }


    private void collectSelectedTags() {
        selectedTags.clear();

        if (!selectedState.isEmpty()) {
            if (switchSubsidy.isChecked()) {
                selectedTags.add(selectedState + "AllowTSSSubsidy");
            }

            if (switchLift.isChecked()) {
                // For ACT and TAS, you might want to add lift amount
                if ("ACT".equals(selectedState)) {
                    String actLift = inputLift.getText().toString();
                    if (!TextUtils.isEmpty(actLift)) {
                        selectedTags.add("ACTAllowTSSLift." + actLift);
                    }
                } else if ("TAS".equals(selectedState)) {
                    String tasLift = inputLift.getText().toString();
                    if (!TextUtils.isEmpty(tasLift)) {
                        selectedTags.add("TASAllowTSSLift." + tasLift);
                    }
                }else {
                    selectedTags.add(selectedState + "AllowTSSLift");
                }
            }
        }
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

    public void viewSaleItem(View view)  {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SaleItem");
        SaleItem saleItem;

        if(customSaleItem ==null){
            saleItem = createSampleSaleItem();
        }else{
            saleItem = customSaleItem;
        }

        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_saleitems, null);
        builder.setView(customLayout);
        EditText editText = customLayout.findViewById(R.id.etSaleItems);

        JSONObject json;
        try {
            json = new JSONObject(printSaleItemtoJson(saleItem));
            editText.setText(json.toString(2));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "TransitData not updated", Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            sendDialogSaleItemToActivity(editText.getText().toString());
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public SaleItem createSampleSaleItem(){
        return new SaleItem.Builder()
                .itemID(1)
                .productCode("MeteredFare")
                .customFields(Collections.singletonList(buildCustomField()))
                .unitOfMeasure(UnitOfMeasure.Kilometre)
                .itemAmount(BigDecimal.valueOf(3.9))
                .unitPrice(BigDecimal.valueOf(3.9))
                .quantity(new BigDecimal(1))
                .productLabel("TARIFF 3")
                .tags(Arrays.asList(new String[]{""}))
                .build();
    }

    public void viewTripData(View view)  {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("TransitData");
        Trip tripData;
                if(customTripData ==null){
                    tripData = createSampleTripData();
                }else{
                    tripData = customTripData;
                }

        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_extensiondata, null);
        builder.setView(customLayout);
        EditText editText = customLayout.findViewById(R.id.etTripData);

        JSONObject json;
        try {
            json = new JSONObject(printTripDatatoJson(tripData));
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
            customTripData = buildTripDatafromJson(data);
//            //Validate TransitData using builder
//            TransitData td = new TransitData.Builder()
//                    .isWheelchairEnabled(customTripData.getTransitData().getIsWheelchairEnabled())
//                    .trip(customTripData.getTransitData().getTrip())
//                    .tags(customTripData.getTransitData().getTags())
//                    .build();
            //Validate Trip using builder
            Trip trip = new Trip.Builder()
                    .addStops(customTripData.getStops())
                    .totalDistanceTravelled(customTripData.getTotalDistanceTravelled())
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
            customTripData = null;
            Toast.makeText(this, "Invalid TransitData. Ignoring.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendDialogSaleItemToActivity(String data) {
        try {
            customSaleItem = buildSaleItemfromJson(data);
            //Validate TransitData using builder
            SaleItem si = new SaleItem.Builder()
                    .itemID(1)
                    .productCode(customSaleItem.getProductCode())
                    .unitOfMeasure(customSaleItem.getUnitOfMeasure())
                    .itemAmount(customSaleItem.getItemAmount())
                    .unitPrice(customSaleItem.getUnitPrice())
                    .quantity(customSaleItem.getQuantity())
                    .productLabel(customSaleItem.getProductLabel())
                    .tags(Arrays.asList(new String[]{""}))
                    .build();
        } catch (Exception e) {
            customSaleItem = null;
            Toast.makeText(this, "Invalid SaleItem. Ignoring.", Toast.LENGTH_SHORT).show();
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
        Trip trip;
        SaleItem saleItem;
        collectSelectedTags();
        String odbs = inputODBS.getText().toString();
        String paymentBrand = inputPaymentBrand.getText().toString();
        String customFooter = inputCustomFooter.getText().toString();

        if(customTripData ==null){
            trip = createSampleTripData();
        }else{
            trip = customTripData;
        }
        if(customSaleItem==null){
            saleItem = createSampleSaleItem();
        }else{
            saleItem = customSaleItem;
        }


        extensionData =  new ExtensionData.Builder().transitData(
                            new TransitData.Builder()
                                    .isWheelchairEnabled(switchWheelchair.isChecked())
                                    .trip(trip)
                                    .tags(selectedTags)
                                    .odbs(TextUtils.isEmpty(odbs) ? null : odbs)
                                    .build())
                        .build();


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
                        .paymentReceipt(Collections.singletonList(new PaymentReceipt.Builder()
                                .documentQualifier(DocumentQualifier.CustomFooter)
                                .outputContent(new OutputContent("XHTML", customFooter))
                                .requiredSignatureFlag(false)
                                .build()))
                        .customFields(saleItem.getCustomFields())
                        .saleData(new SaleData.Builder()
                                .operatorLanguage("en")
                                .operatorID(operatorID)
                                .saleTransactionID(new SaleTransactionID.Builder()
                                        .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                        .transactionID(txtTransactionID.getText().toString())
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
                                        .transactionConditions(new TransactionConditions.Builder()
                                                .allowedPaymentBrand(Arrays.asList(paymentBrand.trim().split(",")))
                                                .build())
                                        .amountsReq(new AmountsReq.Builder()
                                                .currency("AUD")
                                                .requestedAmount(bAmount) //Total of all sale items
                                                .tipAmount(BigDecimal.valueOf(0))
                                                .cashBackAmount(BigDecimal.valueOf(0))
                                                .build())
                                        .addSaleItem(saleItem)
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

    private String getRandomAmount() {
        double amount = ThreadLocalRandom.current().nextDouble(0.1, 100.0);
        return new DecimalFormat("0.00").format(amount);
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

    /**
     * Initiates sending 100 payment requests
     */
    private void send100PaymentRequests(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send 100 Payments");
        builder.setMessage("Are you sure you want to send 100 payment requests?");
        
        builder.setPositiveButton("Yes", (dialog, which) -> {
            sendMultiplePaymentRequests(MULTIPLE_REQUEST_COUNT);
        });
        
        builder.setNegativeButton("No", null);
        builder.show();
    }
    
    /**
     * Sends multiple payment requests in sequence
     * @param count Number of requests to send
     */
    private void sendMultiplePaymentRequests(int count) {
        isProcessingMultipleRequests = true;
        requestCounter = 0;
        successCounter = 0;
        
        tvResults.setText("Preparing to send " + count + " payment requests...");
        Utils.showLog("Multiple Requests", "Starting to send " + count + " requests");
        
        // Start the first request
        sendNextPaymentRequest();
    }
    
    /**
     * Sends the next payment request in the sequence
     */
    private void sendNextPaymentRequest() {
        if (requestCounter < MULTIPLE_REQUEST_COUNT) {
            requestCounter++;
            String serviceID = generateRandomUUID();
            String transactionID = generateRandomUUID();
            txtTransactionID.setText(transactionID);
            
            // Generate a different random amount for each request
            String randomAmount = getRandomAmount();
            inputAmount.setText(randomAmount);
            
            SaleToPOIRequest request = buildPaymentRequest(serviceID);
            
            tvResults.setText("Sending request " + requestCounter + " of " + MULTIPLE_REQUEST_COUNT + 
                             "\nSuccessful: " + successCounter + 
                             "\nAmount: $" + randomAmount);
            Utils.showLog("Multiple Requests", "Sending request " + requestCounter + " of " + MULTIPLE_REQUEST_COUNT + " with amount $" + randomAmount);
            
            // Send the request
            Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);
            Message message = new Message(request);
            intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
            intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, appName);
            intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, appVersion);
            
            startActivityForResult(intent, MULTIPLE_REQUEST_BASE + requestCounter);
        } else {
            // All requests have been sent
            isProcessingMultipleRequests = false;
            tvResults.setText("Completed sending " + MULTIPLE_REQUEST_COUNT + " payment requests\nSuccessful: " + successCounter);
            Utils.showLog("Multiple Requests", "Completed sending all requests");
            
            // Show a completion dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityPayment.this);
            builder.setTitle("Multiple Requests Completed");
            builder.setMessage("Sent " + MULTIPLE_REQUEST_COUNT + " payment requests\nSuccessful: " + successCounter);
            builder.setPositiveButton("OK", null);
            builder.show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        if (data != null && data.hasExtra(Message.INTENT_EXTRA_MESSAGE)) {
            // Check if this is a response from our multiple payment requests
            if (requestCode >= MULTIPLE_REQUEST_BASE && isProcessingMultipleRequests) {
                handleMultiplePaymentResponse(data);
            } else {
                this.handleResponseIntent(data);
            }
        }
    }
    
    /**
     * Handles responses from multiple payment requests
     */
    private void handleMultiplePaymentResponse(Intent intent) {
        Utils.showLog("Multiple Response", "Received response for request " + requestCounter);
        try {
            Message message = Message.fromJson(intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
            SaleToPOIResponse response = message.getResponse();
            
            if (response != null && response.getPaymentResponse() != null) {
                // Check if payment was successful
                if (response.getPaymentResponse().getResponse() != null && 
                    response.getPaymentResponse().getResponse().getResult() != null && 
                    response.getPaymentResponse().getResponse().getResult().toString().equals("Success")) {
                    successCounter++;
                }
                
                // Schedule the next request after a delay
                final Handler handler = new Handler();
                handler.postDelayed(() -> sendNextPaymentRequest(), 1000); // 1 second delay
            } else {
                // If there was an error, still continue with the next request
                final Handler handler = new Handler();
                handler.postDelayed(() -> sendNextPaymentRequest(), 1000);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error reading intent: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            
            // If there was an exception, still continue with the next request
            final Handler handler = new Handler();
            handler.postDelayed(() -> sendNextPaymentRequest(), 1000);
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

    CustomField buildCustomField(){
        Map<String, Object> pickUpLocation = new HashMap<>();
        pickUpLocation.put("Latitude", -33.8688);
        pickUpLocation.put("Longitude", 151.2093);

        Map<String, Object> dropOffLocation = new HashMap<>();
        dropOffLocation.put("Latitude", -33.8688);
        dropOffLocation.put("Longitude", 151.2093);

        Map<String, Object> location = new HashMap<>();
        location.put("PickUp", pickUpLocation);
        location.put("DropOff", dropOffLocation);

        // Convert to JSON using Moshi
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Map<String, Object>> jsonAdapter = moshi.adapter(
                Types.newParameterizedType(Map.class, String.class, Object.class));

        String locationJson = jsonAdapter.toJson(location);

        // Create CustomField
        CustomField locationField = new CustomField.Builder()
                .key("Location")
                .type(CustomFieldType.Object)
                .value(locationJson)
                .build();
        return  locationField;
    }

    Trip buildTripDatafromJson(String jsonString) throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();

        JsonAdapter<Trip> jsonAdapter = moshi.adapter(Trip.class);
        return jsonAdapter.nonNull().fromJson(jsonString);
    }

    SaleItem buildSaleItemfromJson(String jsonString) throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();

        JsonAdapter<SaleItem> jsonAdapter = moshi.adapter(SaleItem.class);
        return jsonAdapter.nonNull().fromJson(jsonString);
    }

    public String printTripDatatoJson(Trip tripData) {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();
        JsonAdapter<Trip> jsonAdapter = moshi.adapter(Trip.class);
        return jsonAdapter.toJson(tripData);
    }

    public String printSaleItemtoJson(SaleItem saleItem) {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();
        JsonAdapter<SaleItem> jsonAdapter = moshi.adapter(SaleItem.class);
        return jsonAdapter.toJson(saleItem);
    }

    public Trip createSampleTripData(){
        return new Trip.Builder()
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