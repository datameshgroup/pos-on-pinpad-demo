package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.UnitOfMeasure;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.paymentrequest.AmountsReq;
import au.com.dmg.fusion.request.paymentrequest.POITransactionID;
import au.com.dmg.fusion.request.paymentrequest.PaymentData;
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest;
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction;
import au.com.dmg.fusion.request.paymentrequest.SaleData;
import au.com.dmg.fusion.request.paymentrequest.SaleItem;
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.TransactionStatusResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentReceipt;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponseCardData;
import au.com.dmg.fusion.response.paymentresponse.PaymentResult;

public class ActivityPayment extends AppCompatActivity {

    BigDecimal totalAmount = BigDecimal.valueOf(0);
    BigDecimal bTotal = BigDecimal.valueOf(0);
    BigDecimal bDiscount = BigDecimal.valueOf(0);
    BigDecimal bTip = BigDecimal.valueOf(0);
    SaleToPOIResponse response = null;
//    String lastTxid = null;

    private ImageView ivScan;
    private Button btnPay;
    private TextView inputTotal;
    private TextView inputDiscount;
    private TextView inputTip;
    private TextView tvResults;
//    private String lastServiceID = null;
    private POITransactionID resPOI = null;
    private TextView txtProductCode = null;

    ErrorCondition errorCondition = null;
    String additionalResponse = "";

    //scanner
    private DMGDeviceImpl device = new DMGDeviceImpl();

    private long pressedTime;

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

        setContentView(R.layout.activity_cart);

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

        inputTotal = (TextView) findViewById(R.id.inputTotal);

        inputDiscount = (TextView) findViewById(R.id.inputDiscount);

        inputTip = (TextView) findViewById(R.id.inputTip);

        tvResults = (TextView) findViewById(R.id.tvResults);
        txtProductCode =  (TextView) findViewById(R.id.txtProductCode);

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
            device.scanBarcode(barcodeHandler, 30, DMGDeviceImpl.camera_front);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
    private void sendPaymentRequest(View view) {

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
        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(new MessageHeader.Builder()
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.Payment)
                        .messageType(MessageType.Request)
                        .serviceID(generateRandomServiceID())
                        .build())
                .request(new PaymentRequest.Builder()
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
                                                .unitOfMeasure(UnitOfMeasure.Litre)
                                                .itemAmount(bTotal)
                                                .unitPrice(bTotal)
                                                .quantity(new BigDecimal(1.0))
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

        sendRequest(request);
        bDiscount = BigDecimal.valueOf(0);
        bTip = BigDecimal.valueOf(0);
        totalAmount = BigDecimal.valueOf(0);
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

    private String generateRandomServiceID() {
        StringBuilder serviceId = new StringBuilder();

        Random rand = new Random();
        for (int i = 0; i < 10; ++i) {
            serviceId.append(rand.nextInt(10));
        }
        return serviceId.toString();
    }

    private void sendRequest(SaleToPOIRequest request) {
        Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);

        // wrapper of request.
        Message message = new Message(request);
        Log.d("Request", message.toJson());

        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
        // name of this app, that gets treated as the POS label by the terminal.
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, "TerminalPOSDemo");
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, "1.0.0");

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
        Log.d("Response", intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
        Message message = null;
        try {
            message = Message.fromJson(intent.getStringExtra(Message.INTENT_EXTRA_MESSAGE));
        } catch (Exception e) {
            Toast.makeText(this, "Error reading intent.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        handleResponse(message.getResponse());
    }

    private void setErrorCondition(MessageCategory mc, SaleToPOIResponse r){
        switch (mc){
            case Payment:
                errorCondition = r.getPaymentResponse().getResponse().getErrorCondition();
                additionalResponse = r.getPaymentResponse().getResponse().getAdditionalResponse();
                break;
            case TransactionStatus:
                errorCondition = r.getTransactionStatusResponse().getResponse().getErrorCondition();
                additionalResponse = r.getTransactionStatusResponse().getResponse().getAdditionalResponse();
                break;
            default:
                errorCondition = ErrorCondition.Unknown;
                additionalResponse = "---";
        }
    }

    private void handleResponse(SaleToPOIResponse response) {
        this.response = response;


        if(response != null) {
            try {
                TextView textViewJson = findViewById(R.id.tvResults);
                Log.d("Response", response.toJson());
                textViewJson.setText(response.toJson()); //prints to cart page

                GlobalClass globalClass = (GlobalClass)getApplicationContext();
                globalClass.setResponse(response);

                //parse response
                MessageHeader mh = response.getMessageHeader();
                MessageCategory mc = mh.getMessageCategory();

                openActivityResult(mc, response);

                }
                catch (Exception e){
                    Log.d("Error", "Invalid Response ==>" + e.getMessage() );
                }

        }
    }

    public void openActivityResult(MessageCategory mc, SaleToPOIResponse r) {
        Intent intent = new Intent(this, ActivityResult.class);

        PaymentReceipt paymentreceipt = null;
        PaymentResponse pr = null;
        PaymentResult paymentRes = null;
        PaymentResponseCardData cardData = null;

        TransactionStatusResponse tsr = null;

        String receiptOutput = "";
        String paymentResult = "";

        intent.putExtra("txnType", mc.toString());

        switch (mc){
            case Payment:
                pr = r.getPaymentResponse();
                paymentResult = pr.getResponse().getResult().name();
                paymentRes = pr.getPaymentResult();
                break;
            case TransactionStatus:
                tsr = r.getTransactionStatusResponse();
                pr = tsr.getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse();
                paymentResult = tsr.getResponse().getResult().name();
                paymentRes = pr.getPaymentResult();
                break;
            default:

        }
        intent.putExtra("result", paymentResult);

        if(paymentResult != "Success"){ // REFUSAL
            setErrorCondition(mc, r);
            intent.putExtra("errorCondition", errorCondition);
            intent.putExtra("additionalResponse", additionalResponse);
        }
        else{

            switch (mc){
                case Payment:
                    ///PaymentAcquirerData
                    intent.putExtra("ApprovalCode", paymentRes.getPaymentAcquirerData().getApprovalCode());
                    intent.putExtra("TransactionID", paymentRes.getPaymentAcquirerData().getAcquirerTransactionID().getTransactionID());
                    break;
                case TransactionStatus:
                    pr = tsr.getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse();
                    paymentRes = pr.getPaymentResult();
                    break;
            }
            paymentreceipt = pr.getPaymentReceipt().get(0);
            receiptOutput = paymentreceipt.getReceiptContentAsHtml();
            cardData = paymentRes.getPaymentInstrumentData().getCardData();

            ///AmountsResp
            intent.putExtra("AuthorizedAmount", paymentRes.getAmountsResp().getAuthorizedAmount().toString());
//            intent.putExtra("TotalFeesAmount", paymentRes.getAmountsResp().getTotalFeesAmount().toString());
            intent.putExtra("CashBackAmount", paymentRes.getAmountsResp().getCashBackAmount().toString());
            intent.putExtra("TipAmount", paymentRes.getAmountsResp().getTipAmount().toString());
            intent.putExtra("SurchargeAmount", paymentRes.getAmountsResp().getSurchargeAmount().toString());

            ///PaymentInstrumentData
            intent.putExtra("PaymentInstrumentType", paymentRes.getPaymentInstrumentData().getPaymentInstrumentType());

            ///CardData
            intent.putExtra("PaymentBrand", cardData.getPaymentBrand().toString());
            intent.putExtra("MaskedPAN", cardData.getMaskedPAN());
            intent.putExtra("EntryMode", cardData.getEntryMode().toString());
            intent.putExtra("Account", Optional.ofNullable(cardData.getAccount()).orElse("not specified"));

//            ///PaymentAcquirerData
//            intent.putExtra("ApprovalCode", paymentRes.getPaymentAcquirerData().getApprovalCode());
//            intent.putExtra("TransactionID", paymentRes.getPaymentAcquirerData().getAcquirerTransactionID().getTransactionID());

            ///PaymentReceipt
            intent.putExtra("OutputXHTML", receiptOutput);

        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra("prevClass", this.getClass());
        startActivity(intent);
    }
}