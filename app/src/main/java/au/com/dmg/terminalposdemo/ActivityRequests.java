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
import au.com.dmg.fusion.data.ErrorCondition;
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

    private Instant preauthTimestamp;
    private TextView tvAmount;

    private SaleToPOIResponse lastResponse = null;
    private PaymentType lastPaymentType= null;
    private PaymentType currentPaymentType= null;
    private String lastTxid = null;
    private String lastServiceID = null;
    private BigDecimal lastAuthorizedAmount = null;
    private POITransactionID lastPoiTransactionID = null;
    private String SalesReference = "";

    ErrorCondition errorCondition = null;
    String additionalResponse = "";

    BigDecimal bAmt = BigDecimal.valueOf(0);

    private long pressedTime;

    GlobalClass globalClass;

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

        // Getting latest transaction response
        globalClass = (GlobalClass)getApplicationContext();
        this.lastResponse = globalClass.getResponse();
        this.lastTxid = (lastResponse == null)? "" : lastResponse.getPaymentResponse().getPoiData().getPOITransactionID().getTransactionID();
        this.lastServiceID = (lastResponse == null)? "" : lastResponse.getMessageHeader().getServiceID();
        this.lastPaymentType = (lastResponse == null) ? PaymentType.Normal : lastResponse.getPaymentResponse().getPaymentResult().getPaymentType();

        Bundle bundle = getIntent().getExtras();

        this.currentPaymentType = (PaymentType) bundle.get("paymentType");

        TextView tvRequestTitle = (TextView) findViewById(R.id.tvRequestTitle);
        TextView txtAmountLabel = (TextView) findViewById(R.id.txtAmountLabel);
        TextView txtSalesReferenceLabel = (TextView) findViewById(R.id.txtSalesReferenceLabel);
        TextView tvSalesReference = (TextView) findViewById(R.id.tvSalesReference);
        TextView txtTransactionIDLabel = (TextView) findViewById(R.id.txtTransactionIDLabel);
        TextView tvTransactionID = (TextView) findViewById(R.id.tvTransactionID);
        Button btnSendReq = (Button) findViewById(R.id.btnSendReq);
        tvAmount = (TextView) findViewById(R.id.tvAmount);

        txtSalesReferenceLabel.setVisibility(View.GONE);
        tvSalesReference.setVisibility(View.GONE);
        txtTransactionIDLabel.setVisibility(View.GONE);
        tvTransactionID.setVisibility(View.GONE);

        switch (currentPaymentType) {
            case Refund:
                tvRequestTitle.setText("REFUND REQUEST");
                btnSendReq.setOnClickListener(v -> sendRefundRequest());
                break;
            case CashAdvance:
                tvRequestTitle.setText("CASHOUT REQUEST");
                btnSendReq.setOnClickListener(v -> sendCashOut());
                break;
            case FirstReservation:
//                globalClass.initPreauthorisationList();
                tvRequestTitle.setText("PREAUTH REQUEST");
                btnSendReq.setOnClickListener(v -> sendPreAuth());
                break;
            case Completion:
                tvRequestTitle.setText("COMPLETION REQUEST");
                GlobalClass.Preauthorisation preauthorisation = new GlobalClass.Preauthorisation();
                this.preauthTimestamp = (Instant) bundle.get("instant");
                preauthorisation = globalClass.getPreauthorisation(preauthTimestamp);
                this.lastAuthorizedAmount = preauthorisation.authorizedAmount;
                this.lastPoiTransactionID = preauthorisation.poiTransactionID;
//                this.SalesReference = (lastResponse == null)? "" : lastResponse.getPaymentResponse().getSaleData().getSaleReferenceID(); // TODO check documentation
                this.SalesReference = "";

                tvSalesReference.setText(SalesReference);
                tvTransactionID.setText((lastPoiTransactionID==null) ? "" : lastPoiTransactionID.getTransactionID());
                tvAmount.setText((lastAuthorizedAmount==null) ? "0.00" : lastAuthorizedAmount.toString());

                txtSalesReferenceLabel.setVisibility(View.VISIBLE);
                tvSalesReference.setVisibility(View.VISIBLE);
                txtTransactionIDLabel.setVisibility(View.VISIBLE);
                tvTransactionID.setVisibility(View.VISIBLE);


                btnSendReq.setOnClickListener(v -> sendCompletion());
                break;
            case Normal:
                tvRequestTitle.setText("TRANSACTION STATUS REQUEST");
                txtAmountLabel.setText("Service ID: \n" + ((lastServiceID == "")  ? "0" : lastServiceID));
                tvAmount.setVisibility(View.GONE);
                btnSendReq.setOnClickListener(v -> sendTransactionStatusRequest());
                break;

                // TODO: Below is for future availability
//            case "CardAcquisition":
//                tvRequestTitle.setText("CARD ACQUISITION REQUEST");
//                txtAmountLabel.setText("CARD:\n" + ((lastTxid == null)  ? "0" : lastTxid));
//                tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendCardAcquisitionRequest());
//                break;
//            case "Reversal":
//                tvRequestTitle.setText("REVERSAL REQUEST");
//                txtAmountLabel.setText("Last Transaction ID: " + ((lastTxid == null)  ? "0" : lastTxid));
//                tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendReversal());
//                break;
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
//        lastTxid = "7607251233";
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
        preauthTimestamp = Instant.ofEpochMilli(System.currentTimeMillis());
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
                                                                .timestamp(preauthTimestamp)
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
        BigDecimal inputAuthorizedAmount = null;
        //TODO: get value of transacctionid and salesreferenceid?
        String amt = tvAmount.getText().toString();
        if (amt == "0" || amt.equals(null) || amt.equals("")){
            inputAuthorizedAmount = this.lastAuthorizedAmount;
        } else
        {
            inputAuthorizedAmount = new BigDecimal(amt);;
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
                                                                .requestedAmount(inputAuthorizedAmount)
                                                                .build()
                                                )
                                                .originalPOITransaction(
                                                        new OriginalPOITransaction.Builder()
                                                                .POIID(getString(R.string.gPOIID))
                                                                .POITransactionID(new POITransactionID(lastPoiTransactionID.getTransactionID(), lastPoiTransactionID.getTimestamp()))
                                                                .saleID("")
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
        if (lastServiceID == null || lastPaymentType!=PaymentType.Normal) {
            lastServiceID = "";
        }

        SaleToPOIRequest request = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.TransactionStatus)
                                .messageType(MessageType.Request)
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
        this.lastResponse = response;

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

    public void openActivityResult(MessageCategory mc, SaleToPOIResponse r) {
        Intent intent = new Intent(this, ActivityResult.class);
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
                intent.putExtra("txnType", mc.toString());
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

        if(paymentResult != "Success"){ // REFUSAL //TODO: Remove "already completed" error preauth?
            setErrorCondition(mc, r);
            intent.putExtra("errorCondition", errorCondition);
            intent.putExtra("additionalResponse", additionalResponse);
        }
        else{

            switch (mc){
                case Payment:
                    intent.putExtra("ApprovalCode", paymentRes.getPaymentAcquirerData().getApprovalCode());
                    intent.putExtra("TransactionID", paymentRes.getPaymentAcquirerData().getAcquirerTransactionID().getTransactionID());

                    if(currentPaymentType == PaymentType.FirstReservation){
                        globalClass.setResponse(r);
                        globalClass.addPreauthorisation(r);
                    } else if(currentPaymentType == PaymentType.Completion){
                        globalClass.setResponse(r);
                        globalClass.removePreauthorisation(preauthTimestamp);
                    }
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

        intent.putExtra("prevClass", this.getClass());
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