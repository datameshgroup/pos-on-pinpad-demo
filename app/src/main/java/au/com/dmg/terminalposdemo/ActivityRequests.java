package au.com.dmg.terminalposdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.ReversalReason;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.cardacquisitionrequest.CardAcquisitionRequest;
import au.com.dmg.fusion.request.paymentrequest.AmountsReq;
import au.com.dmg.fusion.request.paymentrequest.OriginalPOITransaction;
import au.com.dmg.fusion.request.paymentrequest.POITransactionID;
import au.com.dmg.fusion.request.paymentrequest.PaymentData;
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest;
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction;
import au.com.dmg.fusion.request.paymentrequest.SaleData;
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID;
import au.com.dmg.fusion.request.reversalrequest.ReversalRequest;
import au.com.dmg.fusion.request.transactionstatusrequest.TransactionStatusRequest;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.TransactionStatusResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentReceipt;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponseCardData;
import au.com.dmg.fusion.response.paymentresponse.PaymentResult;

public class ActivityRequests extends AppCompatActivity {

    private String requestName;
    private TextView tvRequestTitle;
    private TextView txtAmountLabel;
    private TextView tvAmount;
    private Button btnSendReq;

    private SaleToPOIResponse response = null;
    private String lastTxid = null;
    private String lastServiceID = null;

    String errorCondition = "";
    String additionalResponse = "";

    BigDecimal bAmt = BigDecimal.valueOf(0);

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
        setContentView(R.layout.activity_requests);

        GlobalClass globalClass = (GlobalClass)getApplicationContext();
        this.lastTxid = globalClass.getgLasttxnID();
        this.lastServiceID = globalClass.getgLastServiceID();

        Bundle bundle = getIntent().getExtras();

        String requestName = bundle.getString("requestName");

        tvRequestTitle = (TextView) findViewById(R.id.tvRequestTitle);
        txtAmountLabel = (TextView) findViewById(R.id.txtAmountLabel);
        btnSendReq = (Button) findViewById(R.id.btnSendReq);
        tvAmount = (TextView) findViewById(R.id.tvAmount);

        switch (requestName) {

            case "refund":
                tvRequestTitle.setText("REFUND REQUEST");
                btnSendReq.setOnClickListener(v -> sendRefundRequest());
                break;
            case "reversal":
                tvRequestTitle.setText("REVERSAL REQUEST");
                txtAmountLabel.setText("Last Transaction ID: " + ((lastTxid == null)  ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
                btnSendReq.setOnClickListener(v -> sendReversal());
                break;
            case "cashout":
                tvRequestTitle.setText("CASHOUT REQUEST");
                btnSendReq.setOnClickListener(v -> sendCashOut());
                break;
            case "preauth":
                tvRequestTitle.setText("PREAUTH REQUEST");
                txtAmountLabel.setText("ID: " + ((lastTxid == null)  ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
                btnSendReq.setOnClickListener(v -> sendPreAuth());
                break;
            case "completion":
                tvRequestTitle.setText("COMPLETION REQUEST");
                txtAmountLabel.setText("ID: " + ((lastTxid == null)  ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
                btnSendReq.setOnClickListener(v -> sendCompletion());
                break;
            case "txnstatus":
                tvRequestTitle.setText("TRANSACTION STATUS REQUEST");
                txtAmountLabel.setText("ID:\n" + ((lastTxid == null)  ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
                btnSendReq.setOnClickListener(v -> sendTransactionStatusRequest());
                break;
            case "cardacq":
                tvRequestTitle.setText("CARD ACQUISITION REQUEST");
                txtAmountLabel.setText("CARD:\n" + ((lastTxid == null)  ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
                btnSendReq.setOnClickListener(v -> sendCardAcquisitionRequest());
                break;
            default:
                tvRequestTitle.setText("no match");
                txtAmountLabel.setVisibility(View.GONE);
                tvAmount.setVisibility(View.GONE);
        }

    }

    private void sendRefundRequest() {
        bAmt = new BigDecimal(tvAmount.getText().toString());
        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Payment)
                                .messageType(MessageType.Request)
                                .serviceID(generateRandomServiceID())
                                .build()
                )
                .request(
                        new PaymentRequest.Builder()
                                .saleData(
                                        new SaleData.Builder()
                                                .operatorLanguage("en")
                                                .saleTransactionID(
                                                        new SaleTransactionID.Builder()
                                                                .transactionID(generateTransactionId())
                                                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                                                .build()
                                                ).build()
                                )
                                .paymentTransaction(
                                        new PaymentTransaction.Builder()
                                                .amountsReq(
                                                        new AmountsReq.Builder()
                                                                .currency("AUD")
                                                                .requestedAmount(bAmt)
                                                                .build()
                                                )
                                                .build()
                                )
                                .paymentData(
                                        new PaymentData.Builder()
                                                .paymentType(PaymentType.Refund)
                                                .build()
                                )
                                .build()
                )
                .build();

        sendRequest(request);
    }

    private void sendReversal(){
        lastTxid = "7607251233";
        if(lastTxid == null){
            Toast.makeText(this, "Send a transaction first.", Toast.LENGTH_SHORT).show();
            return;
        }
        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Reversal)
                                .messageType(MessageType.Request)
                                .serviceID(generateRandomServiceID())
                                .build()
                )
                .request(new ReversalRequest.Builder()
                        .reversalReason(ReversalReason.SignatureDeclined)
                        .originalPOITransaction(new OriginalPOITransaction.Builder()
                                .POIID(getString(R.string.gPOIID))
                                .saleID(getString(R.string.gsaleID))
                                .POITransactionID(new POITransactionID(lastTxid, Instant.ofEpochMilli(System.currentTimeMillis())))
                                .build())
                        .build())
                .build();

        sendRequest(request);
    }

    private void sendCashOut() {
        bAmt = new BigDecimal(tvAmount.getText().toString());
        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Payment)
                                .messageType(MessageType.Request)
                                .serviceID(generateRandomServiceID())
                                .build()
                )
                .request(
                        new PaymentRequest.Builder()
                                .saleData(
                                        new SaleData.Builder()
                                                .operatorLanguage("en")
                                                .saleTransactionID(
                                                        new SaleTransactionID.Builder()
                                                                .transactionID(generateTransactionId())
                                                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                                                .build()
                                                ).build()
                                )
                                .paymentTransaction(
                                        new PaymentTransaction.Builder()
                                                .amountsReq(
                                                        new AmountsReq.Builder()
                                                                .currency("AUD")
                                                                .requestedAmount(bAmt)
                                                                .build()
                                                )
                                                .build()
                                )
                                .paymentData(
                                        new PaymentData.Builder()
                                                .paymentType(PaymentType.CashAdvance)
                                                .build()
                                )
                                .build()
                )
                .build();

        sendRequest(request);
    }

    private void sendPreAuth() {
        bAmt = new BigDecimal(tvAmount.getText().toString());
        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Payment)
                                .messageType(MessageType.Request)
                                .serviceID(generateRandomServiceID())
                                .build()
                )
                .request(
                        new PaymentRequest.Builder()
                                .saleData(
                                        new SaleData.Builder()
                                                .operatorLanguage("en")
                                                .saleTransactionID(
                                                        new SaleTransactionID.Builder()
                                                                .transactionID(generateTransactionId())
                                                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                                                .build()
                                                ).build()
                                )
                                .paymentTransaction(
                                        new PaymentTransaction.Builder()
                                                .amountsReq(
                                                        new AmountsReq.Builder()
                                                                .currency("AUD")
                                                                .requestedAmount(bAmt)
                                                                .build()
                                                )
                                                .build()
                                )
                                .paymentData(
                                        new PaymentData.Builder()
                                                .paymentType(PaymentType.FirstReservation)
                                                .build()
                                )
                                .build()
                )
                .build();

        sendRequest(request);
    }

    private void sendCompletion() {
        if (response == null) {
            Toast.makeText(this, "No prior transaction to perform completion", Toast.LENGTH_SHORT).show();
            return;
        }

        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Payment)
                                .messageType(MessageType.Request)
                                .serviceID(generateRandomServiceID())
                                .build()
                )
                .request(
                        new PaymentRequest.Builder()
                                .saleData(
                                        new SaleData.Builder()
                                                .operatorLanguage("en")
                                                .saleTransactionID(
                                                        new SaleTransactionID.Builder()
                                                                .transactionID(generateTransactionId())
                                                                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                                                .build()
                                                ).build()
                                )
                                .paymentTransaction(
                                        new PaymentTransaction.Builder()
                                                .amountsReq(
                                                        new AmountsReq.Builder()
                                                                .currency("AUD")
                                                                .requestedAmount(response.getPaymentResponse().getPaymentResult().getAmountsResp().getAuthorizedAmount())
                                                                .build()
                                                )
                                                .originalPOITransaction(
                                                        new OriginalPOITransaction.Builder()
                                                                .POIID(getString(R.string.gPOIID))
                                                                .POITransactionID(response.getPaymentResponse().getPoiData().getPOITransactionID())
                                                                .saleID(response.getPaymentResponse().getSaleData().getSaleTransactionID().getTransactionID())
                                                                .reuseCardDataFlag(true)
                                                                .build()
                                                )
                                                .build()
                                )
                                .paymentData(
                                        new PaymentData.Builder()
                                                .paymentType(PaymentType.Completion)
                                                .build()
                                )
                                .build()
                )
                .build();

        sendRequest(request);
    }

    private void sendTransactionStatusRequest() {
        if (lastServiceID == null) {
            Toast.makeText(this, "Perform a transaction first.", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: 1. check refund PaymentType, 2. check null serviceid transactionstatusrequest

        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.TransactionStatus)
                                .messageType(MessageType.Request)
//                                .serviceID(null)
                                .serviceID(lastServiceID)
                                .build()
                )
                .request(new TransactionStatusRequest())
                .build();

        sendRequest(request);
    }

    private void sendCardAcquisitionRequest() {
        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(new MessageHeader.Builder()
                        .serviceID(generateRandomServiceID())
                        .messageClass(MessageClass.Service)
                        .messageCategory(MessageCategory.CardAcquisition)
                        .messageType(MessageType.Request)
                        .build()
                )
                .request(new CardAcquisitionRequest.Builder()
                        .saleData(new SaleData.Builder()
                                .saleTransactionID(new SaleTransactionID.Builder()
                                        .transactionID(generateTransactionId())
                                        .timestamp(Instant.now())
                                        .build())
                                .tokenRequestedType("Customer")
                                .build())
                        .build()
                )
                .build();

        sendRequest(request);
    }

    private void sendRequest(SaleToPOIRequest request) {
        Intent intent = new Intent(Message.INTENT_ACTION_SALETOPOI_REQUEST);

        // wrapper of request.
        Message message = new Message(request);
        Log.d("Request", message.toJson());

        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
        // name of this app, that gets treated as the POS label by the terminal.
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, "DemoPOS");
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
    private void handleResponse(SaleToPOIResponse response) {
        this.response = response;
        //TextView textViewJson = findViewById(R.id.tvResults);
        Log.d("Response", response.toJson());
        //textViewJson.setText(response.toJson());
        MessageHeader mh = response.getMessageHeader();
        MessageCategory mc = mh.getMessageCategory();

        openActivityResult(mc, response);
    }
    private void setErrorCondition(MessageCategory mc, SaleToPOIResponse r){
        switch (mc){
            case Payment:
                errorCondition = r.getPaymentResponse().getResponse().getErrorCondition().toString();
                additionalResponse = r.getPaymentResponse().getResponse().getAdditionalResponse();
                break;
            case TransactionStatus:
                errorCondition = r.getTransactionStatusResponse().getResponse().getErrorCondition().toString();
                additionalResponse = r.getTransactionStatusResponse().getResponse().getAdditionalResponse();
                break;
            default:
                errorCondition = "Unknown Error";
                additionalResponse = "---";
        }
    }

    public void openActivityResult(MessageCategory mc, SaleToPOIResponse r) {
        Intent intent = new Intent(this, ActivityResult.class);
//        Response resp;
        PaymentReceipt paymentreceipt = null;
        PaymentResponse pr = null;
        PaymentResult paymentRes = null;
        PaymentResponseCardData cardData = null;

        TransactionStatusResponse tsr = null;

        String receiptOutput = "";
        String paymentResult = "";

        switch (mc){
            case Payment:
                pr = r.getPaymentResponse();
                paymentResult = pr.getResponse().getResult().name();
                paymentRes = pr.getPaymentResult();
                intent.putExtra("txnType", "Refund"); //van

                break;
            case TransactionStatus:
                tsr = r.getTransactionStatusResponse();
                paymentResult = tsr.getResponse().getResult().name();
                intent.putExtra("txnType", mc.toString());
                break;
            default:
                intent.putExtra("txnType", "OTHERS");
                paymentResult = "unknown"; // change for other transaction types
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

            ///PaymentReceipt
            intent.putExtra("OutputXHTML", receiptOutput);

        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
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

}