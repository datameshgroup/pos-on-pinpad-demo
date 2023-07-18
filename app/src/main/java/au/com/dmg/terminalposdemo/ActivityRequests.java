package au.com.dmg.terminalposdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;
import java.time.Instant;
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
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse;

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

    @SuppressLint("SetTextI18n")
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
                tvRequestTitle.setText("PREAUTH REQUEST");
                btnSendReq.setOnClickListener(v -> sendPreAuth());
                break;
            case Completion:
                tvRequestTitle.setText("COMPLETION REQUEST");
                GlobalClass.Preauthorisation preauthorisation;
                this.preauthTimestamp = (Instant) bundle.get("instant");
                preauthorisation = globalClass.getPreauthorisation(preauthTimestamp);
                this.lastAuthorizedAmount = preauthorisation.authorizedAmount;
                this.lastPoiTransactionID = preauthorisation.poiTransactionID;
//                this.SalesReference = (lastResponse == null)? "" : lastResponse.getPaymentResponse().getSaleData().getSaleReferenceID(); // TODO check/update documentation
                this.SalesReference = "";

                tvSalesReference.setText(SalesReference);
                tvTransactionID.setText((lastPoiTransactionID==null) ? "" : lastPoiTransactionID.getTransactionID());
                tvAmount.setText((lastAuthorizedAmount==null) ? "0.00" : lastAuthorizedAmount.toString());
                // TODO check if SalesReference is still a requirement for completion
//                txtSalesReferenceLabel.setVisibility(View.VISIBLE);
//                tvSalesReference.setVisibility(View.VISIBLE);
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
                                .serviceID(generateServiceID())
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
                                .serviceID(generateServiceID())
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
                                .serviceID(generateServiceID())
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
                                .serviceID(generateServiceID())
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
        //TODO: check if salesreferenceid is still needed
        String amt = tvAmount.getText().toString();
        if (amt.equals("0") || amt == null || amt.equals("")){
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
                                .serviceID(generateServiceID())
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
                        .serviceID(generateServiceID())
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
        Utils.showLog("Request", message.toJson());

        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, message.toJson());
        // name of this app, that gets treated as the POS label by the terminal.
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_NAME, GlobalClass.APPLICATION_NAME);
        intent.putExtra(Message.INTENT_EXTRA_APPLICATION_VERSION, GlobalClass.APPLICATION_VERSION);

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
        SaleToPOIResponse response = message.getResponse();
        this.lastResponse = response;

        //TextView textViewJson = findViewById(R.id.tvResults);
        Utils.showLog("Response", response.toJson());
        //textViewJson.setText(response.toJson());
        MessageHeader mh = response.getMessageHeader();
        MessageCategory mc = mh.getMessageCategory();

        openActivityResult(mc, response, message);
    }

    public void openActivityResult(MessageCategory mc, SaleToPOIResponse r, Message message) {
        Intent intent = new Intent(this, ActivityResult.class);

        Bundle bundle = new Bundle();
        bundle.putSerializable("messageCategory", mc);
        bundle.putString("message", message.toString());
        intent.putExtras(bundle);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("prevClass", this.getClass());


        PaymentResponse pr = null;
        TransactionStatusResponse tsr = null;

        String paymentResult = "";


        switch (mc){
            case Payment:
                pr = r.getPaymentResponse();
                paymentResult = pr.getResponse().getResult().name();
                break;
            case TransactionStatus:
                tsr = r.getTransactionStatusResponse();
                paymentResult = tsr.getResponse().getResult().name();
                break;
            default:
                paymentResult = "unknown"; // change for other transaction types
        }

        if(paymentResult.equals("Success")){  //TODO: Remove "already completed" error preauth?

            if (mc.equals(MessageCategory.Payment)){
                if(currentPaymentType == PaymentType.FirstReservation){
                    globalClass.setResponse(r);
                    globalClass.addPreauthorisation(r);
                } else if(currentPaymentType == PaymentType.Completion){
                    globalClass.setResponse(r);
                    globalClass.removePreauthorisation(preauthTimestamp);
                }
            }

        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra("prevClass", this.getClass());
        startActivity(intent);
    }

    private String generateTransactionId() {
        return java.util.UUID.randomUUID().toString();
    }
    private String generateServiceID() {
        return java.util.UUID.randomUUID().toString();
    }

}