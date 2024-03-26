package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.os.RemoteException;
import android.util.Log;
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
import java.util.Random;

import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.CustomFieldType;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.UnitOfMeasure;
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
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.util.BigDecimalAdapter;
import au.com.dmg.fusion.util.InstantAdapter;

public class ActivityPayment extends AppCompatActivity {

    BigDecimal totalAmount = BigDecimal.valueOf(0);
    BigDecimal bTotal = BigDecimal.valueOf(0);
    BigDecimal bDiscount = BigDecimal.valueOf(0);
    BigDecimal bTip = BigDecimal.valueOf(0);
    SaleToPOIResponse response = null;

    private ImageView ivScan;
    private Button btnPay;
    private Button btnAbort;
    private Button btnCustomField;
    private TextView inputTotal;
    private TextView inputDiscount;
    private TextView inputTip;
    private TextView tvResults;
    private POITransactionID resPOI = null;
    private TextView txtProductCode = null;

    ErrorCondition errorCondition = null;
    String additionalResponse = "";

    //scanner
    private TerminalDevice device = new TerminalDevice();

    private long pressedTime;

    String testServiceID;
    CustomField customField = null;
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

        btnCustomField = (Button) findViewById(R.id.btnCustomField);
        btnCustomField.setOnClickListener(this::viewCustomField);

        inputTotal = (TextView) findViewById(R.id.inputTotal);

        inputDiscount = (TextView) findViewById(R.id.inputDiscount);

        inputTip = (TextView) findViewById(R.id.inputTip);

        tvResults = (TextView) findViewById(R.id.tvResults);
        txtProductCode =  (TextView) findViewById(R.id.txtProductCode);

    }

    public void viewCustomField(View view)  {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CustomField");
        CustomField customField;
                if(this.customField ==null){
                    customField = createCustomField();
                }else{
                    customField = this.customField;
                }

        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_customfield, null);
        builder.setView(customLayout);
        EditText editText = customLayout.findViewById(R.id.etExtenstionData);

        JSONObject json;
        try {
            json = new JSONObject(printCustomFieldtoJson(customField));
            editText.setText(json.toString(2));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "CustomField not update", Toast.LENGTH_SHORT).show();
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
            customField = buildCustomFieldfromJson(data);
            //Validate CustomField using builder
            CustomField cf = new CustomField.Builder()
                    .key(customField.getKey())
                    .type(customField.getType())
                    .value(customField.getValue())
                    .build();
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            customField = null;
            Toast.makeText(this, "Invalid CustomField. Ignoring.", Toast.LENGTH_SHORT).show();
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

        this.testServiceID = generateServiceID();
        //create payment request first
        SaleToPOIRequest paymentRequest = buildPaymentRequest(testServiceID);

        Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);

        // wrapper of request.
        Message message = new Message(paymentRequest);
        Log.d("Request", message.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, GlobalClass.APPLICATION_NAME);
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, GlobalClass.APPLICATION_VERSION);

        //create abort request
        SaleToPOIRequest abortRequest = buildAbortRequest(testServiceID);
        Intent intentCancel = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message messageCancel = new Message(abortRequest);
        Log.d("AbortRequest", messageCancel.toJson());
        intentCancel.putExtra(Message.INTENT_EXTRA_MESSAGE, messageCancel.toJson());
        intentCancel.putExtra("RETURN_TO_PACKAGE", this.getPackageName());

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
        CustomField customField1;
        if(customField ==null){
            customField1 = createCustomField();
        }else{
            customField1 = customField;
        }

        //Computation
        bTotal = new BigDecimal(inputTotal.getText().toString());
        if (inputDiscount != null && !inputDiscount.getText().toString().isEmpty()) {
            bDiscount = new BigDecimal(inputDiscount.getText().toString());
        }
        if (inputTip != null && !inputTip.getText().toString().isEmpty()) {
            bTip = new BigDecimal(inputTip.getText().toString());
        }
        totalAmount = bTotal.subtract(bDiscount).add(bTip);

        //Request creation
        paymentRequest = new SaleToPOIRequest.Builder()
                .messageHeader(new MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Payment)
                        .messageType(MessageType.Request)
                        .serviceID(generateServiceID())
                        .build())
                .request(new PaymentRequest.Builder()
                        .addCustomField(customField1)
                        .saleData(new SaleData.Builder()
                                .operatorLanguage("en")
                                .saleTransactionID(new SaleTransactionID.Builder()
                                        .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                        .transactionID(generateTransactionId())
                                        .build())
                                .build())
                        .paymentTransaction(
                                new PaymentTransaction.Builder()
                                        .amountsReq(new AmountsReq.Builder()
                                                .currency("AUD")
                                                .requestedAmount(totalAmount)
                                                .tipAmount(bTip)
                                                .cashBackAmount(bDiscount)
                                                .build())
                                        .addSaleItem(new SaleItem.Builder()
                                                .itemID(1)
                                                .productCode(txtProductCode.getText().toString())
                                                .unitOfMeasure(UnitOfMeasure.Kilometre)
                                                .itemAmount(bTotal)
                                                .unitPrice(bTotal)
                                                .quantity(new BigDecimal(1))
                                                .productLabel(getString(R.string.idProductLabel))
                                                .build())
                                        .build()
                        )
                        .paymentData(new PaymentData.Builder()
                                .paymentType(PaymentType.Normal)
                                .build())
                        .build()
                )
                .build();
        return paymentRequest;
    }

    private void sendPaymentRequest(View view) {
        this.testServiceID = generateServiceID();
        //Computation
        SaleToPOIRequest request = buildPaymentRequest(testServiceID);

        sendRequest(request);

        bDiscount = BigDecimal.valueOf(0);
        bTip = BigDecimal.valueOf(0);
        totalAmount = BigDecimal.valueOf(0);
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
                        .serviceID(generateServiceID())
                        .build())
                .request(abortTransactionRequest)
                .build();

        return abortRequest;
    }
    private String generateTransactionId() {
        String s = "";
        Random r = new Random();
        for (int i = 0; i < 10; ++i) {
            int x = r.nextInt(10);
            s += x;
        }
        return s;
    }

    private String generateServiceID() {
        return java.util.UUID.randomUUID().toString();
    }

    private void sendRequest(SaleToPOIRequest request) {
        // V2

//        Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);
//
//        // wrapper of request.
//        Message message = new Message(request);
//        Log.d("Request", message.toJson());
//
//        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
//        // name of this app, that gets treated as the POS label by the terminal.
//        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, GlobalClass.APPLICATION_NAME);
//        // version of of this POS app.
//        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, GlobalClass.APPLICATION_VERSION);


        //v1
        Intent intent = new Intent("au.com.dmg.axispay");
        intent.putExtra("TransType", "Purchase Transaction");
        intent.putExtra("Amount", 2345);
        intent.putExtra("CashOut", 0);
        intent.putExtra("POS", "Android POS App!");
        intent.putExtra("Source", "POS App V0.00.00");

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        // V1 Only for Logs
        if (requestCode == 100) {
            if (responseCode == RESULT_OK) {
                if (data != null) {
                    String state = data.getStringExtra("TransState");
                    if (state != null)
                        System.out.println("=====V1 Result Transaction " + state);
                    String id = data.getStringExtra("TransID");
                    if (id != null)
                        System.out.println("=====V1 Result TXN ID = " + id);
                }
            } else if (responseCode == RESULT_CANCELED) {
                System.out.println("=====V1 Result Transaction aborted!");
                System.out.println("=====V1 Result ");
            }
        }

        //V2
        if (data != null && data.hasExtra(Message.INTENT_EXTRA_MESSAGE)) {
            this.handleResponseIntent(data);
        }
    }

    private void handleResponseIntent(Intent intent) {
        Log.d("Response", intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
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
                Log.d("Response", response.toJson());
                textViewJson.setText(response.toJson()); //prints to payment page

                GlobalClass globalClass = (GlobalClass)getApplicationContext();
                globalClass.setResponse(response);

                //parse response
                MessageHeader mh = response.getMessageHeader();
                MessageCategory mc = mh.getMessageCategory();

                openActivityResult(mc, response, message);

                }
                catch (Exception e){
                    Log.d("Error", "Invalid Response ==>" + e.getMessage() );
                }

        }
    }
    CustomField buildCustomFieldfromJson(String jsonString) throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();

        JsonAdapter<CustomField> jsonAdapter = moshi.adapter(CustomField.class);
        return jsonAdapter.nonNull().fromJson(jsonString);
    }

    public String printCustomFieldtoJson(CustomField customField) {
        Moshi moshi = new Moshi.Builder()
                .add(new BigDecimalAdapter())
                .add(new InstantAdapter())
                .build();
        JsonAdapter<CustomField> jsonAdapter = moshi.adapter(CustomField.class);
        return jsonAdapter.toJson(customField);
    }

    public CustomField createCustomField(){
        //CustomField. This example id for CustomFieldType==Object. You can also do:
        //    Integer,
        //    Number,
        //    String,
        //    Array,
        //    Object,
        //    Boolean,
        //    Unknown;
        String[] strArray = {"\"sample1\"", "\"sample2\"", "\"sample3\""};

        CustomData customData = new CustomData.Builder()
                .GroupName("SampleCustomData")
                .Quantity(100)
                .Items(Arrays.asList(strArray))
                .build();

        return new CustomField.Builder()
                .key("samplePaymentRequestCustomFieldKey")
                .type(CustomFieldType.Object)
                .value(customData.toString())
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
}