package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.PaymentInstrumentType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.request.paymentrequest.POIData;
import au.com.dmg.fusion.response.CardAcquisitionResponse;
import au.com.dmg.fusion.response.ResponseResult;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.TransactionStatusResponse;
import au.com.dmg.fusion.response.paymentresponse.AdditionalAmount;
import au.com.dmg.fusion.response.paymentresponse.AmountsResp;
import au.com.dmg.fusion.response.paymentresponse.PaymentAcquirerData;
import au.com.dmg.fusion.response.paymentresponse.PaymentInstrumentData;
import au.com.dmg.fusion.response.paymentresponse.PaymentReceipt;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponseCardData;
import au.com.dmg.fusion.response.paymentresponse.PaymentResult;
import au.com.dmg.fusion.response.reversalresponse.ReversalResponse;

public class ActivityResult extends AppCompatActivity {

    private static String noValue = "Not received";
    private Button btnPrintReceipt;
    private Button btnBack;
    WebView tvReceipt;
    String outputXHTML;
    Bitmap bitmap;
    Boolean isApproved = false;

//    Class prevClass;

    private TerminalDevice device = new TerminalDevice();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device.init(getApplicationContext());

        setContentView(R.layout.activity_result);

        Bundle bundle = getIntent().getExtras();
//        prevClass = (Class) bundle.get("prevClass");

        btnPrintReceipt = (Button) findViewById(R.id.btnPrintReceipt);
        btnBack = (Button) findViewById(R.id.btnBack);
        tvReceipt = (WebView) findViewById(R.id.wvReceipt);
        tvReceipt.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        TextView tvMessageHead = (TextView) findViewById(R.id.tvMessageHead);
        TextView tvMessageDetail = (TextView) findViewById(R.id.tvMessageDetail);

        ErrorCondition errorCondition = null;
        String additionalResponse = "";
        SaleToPOIResponse saleToPOIResponse = null;
        Message message = null;

        PaymentResponse paymentResponse = null;
        TransactionStatusResponse transactionStatusResponse = null;
        ReversalResponse reversalResponse = null;
        CardAcquisitionResponse cardAcquisitionResponse = null;

        PaymentResult paymentResult = null;
        PaymentType paymentType = null;

        ResponseResult responseResult = null;

        String details = "";


        MessageCategory mc = (MessageCategory)bundle.getSerializable("messageCategory");

        try {
            message = Message.fromJson(bundle.getString("message"));
            saleToPOIResponse = message.getResponse();
        } catch (IOException e) {
//            throw new RuntimeException(e);
            Utils.showLog("Error", "Invalid Response ==>" + e.getMessage() );
        }

        //Check the response
       switch (mc){
           case Payment:
               paymentResponse = saleToPOIResponse.getPaymentResponse();
               break;
           case TransactionStatus:
               transactionStatusResponse = saleToPOIResponse.getTransactionStatusResponse();
               paymentResponse = transactionStatusResponse.getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse();
               break;
           case Reversal:
               reversalResponse = saleToPOIResponse.getReversalResponse();
               break;
           case CardAcquisition:
               cardAcquisitionResponse = saleToPOIResponse.getCardAcquisitionResponse();
               break;
       }
        if (paymentResponse!=null){
            paymentResult = paymentResponse.getPaymentResult();
            responseResult = paymentResponse.getResponse().getResult();
        } //TODO: add reversal and card acquisition responses here

        //TODO: add home button top? fix back button or do fullscreen

        ///PaymentReceipt
        List<PaymentReceipt> paymentReceipt = paymentResponse.getPaymentReceipt();
        outputXHTML = noValue;
        if(paymentReceipt!=null){
            outputXHTML = StringUtils.defaultIfEmpty(paymentReceipt.get(0).getReceiptContentAsHtml(), noValue);
        }

        if(responseResult.equals(ResponseResult.Success) || responseResult.equals(ResponseResult.Partial)){
            isApproved = true;
            paymentType = paymentResult.getPaymentType();

            ///AmountsResp
            AmountsResp amountsResp = paymentResult.getAmountsResp();
            String authorizedAmount = Optional.ofNullable(amountsResp.getAuthorizedAmount()).orElse(BigDecimal.ZERO).toString();
            String totalFeesAmount = Optional.ofNullable(amountsResp.getTotalFeesAmount()).orElse(BigDecimal.ZERO).toString();
            String cashBackAmount = Optional.ofNullable(amountsResp.getCashBackAmount()).orElse(BigDecimal.ZERO).toString();
            String tipAmount = Optional.ofNullable(amountsResp.getTipAmount()).orElse(BigDecimal.ZERO).toString();
            String surchargeAmount = Optional.ofNullable(amountsResp.getSurchargeAmount()).orElse(BigDecimal.ZERO).toString();
            String partialAuthorizedAmount = Optional.ofNullable(amountsResp.getPartialAuthorizedAmount()).orElse(BigDecimal.ZERO).toString();
            String requestedAmount =  Optional.ofNullable(amountsResp.getRequestedAmount()).orElse(BigDecimal.ZERO).toString();

            // Partial Payment Logic
            GlobalClass globalClass = (GlobalClass)getApplicationContext();
            if(responseResult.equals(ResponseResult.Partial)){
                BigDecimal remainingAmount = amountsResp.getRequestedAmount().subtract(amountsResp.getPartialAuthorizedAmount());
                String transactionID = paymentResponse.getSaleData().getSaleTransactionID().getTransactionID();
                globalClass.updatePartialPayment(true, remainingAmount ,transactionID);
            }else{
                globalClass.updatePartialPayment(false, new BigDecimal(0) ,"");
            }

            List<AdditionalAmount> additionalAmounts = amountsResp.getAdditionalAmounts();
            String surchargeTax = "0.00";
            String liftFee = "0.00";

            ///AdditionalAmounts
            if(!(additionalAmounts ==null) && !additionalAmounts.isEmpty()){
                surchargeTax =  StringUtils.defaultIfEmpty(additionalAmounts.get(additionalAmounts.indexOf("SurchargeTax")).toString(), noValue);
                liftFee = StringUtils.defaultIfEmpty(additionalAmounts.get(additionalAmounts.indexOf("LiftFee")).toString(), noValue);
            }

            ///PaymentInstrumentData
            PaymentInstrumentData paymentInstrumentData = paymentResult.getPaymentInstrumentData();
            PaymentInstrumentType paymentInstrumentType = Optional.ofNullable(paymentInstrumentData.getPaymentInstrumentType())
                    .orElse(PaymentInstrumentType.Other);

            ///CardData
            PaymentResponseCardData cardData = paymentInstrumentData.getCardData();
            String paymentBrand = StringUtils.defaultIfEmpty(cardData.getPaymentBrand().toString(), noValue);
            String maskedPAN =  StringUtils.defaultIfEmpty(cardData.getMaskedPAN(), noValue);
            String entryMode = StringUtils.defaultIfEmpty(cardData.getEntryMode().toString(), noValue);
            String account = StringUtils.defaultIfEmpty(cardData.getAccount(), noValue);
            String cardExpiry = StringUtils.defaultIfEmpty(cardData.getExpiry(), noValue);

            ///PaymentAcquirerData
            PaymentAcquirerData paymentAcquirerData = paymentResult.getPaymentAcquirerData();
            String acquirerTransactionID = noValue;
            if (paymentAcquirerData != null){
                acquirerTransactionID = StringUtils.defaultIfEmpty(paymentAcquirerData.getAcquirerTransactionID().getTransactionID(), noValue);
            }


            ///POIData
            POIData poiData = paymentResponse.getPoiData();
            String poiTransactionID = noValue;
            if(poiData!=null){
                poiTransactionID = StringUtils.defaultIfEmpty(poiData.getPOITransactionID().getTransactionID(), noValue);
            }

            switch (mc) {
                case Payment:
                case TransactionStatus:
                    if(paymentType.equals(PaymentType.Normal)){
                        tvMessageHead.setText("Payment " + responseResult);
                    } else if (paymentType.equals(PaymentType.FirstReservation)) {
                        tvMessageHead.setText("Preauthorisation " + responseResult);
                    } else{
                        tvMessageHead.setText(paymentType.toString()  + " " + responseResult);
                    }
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    String trimmedJSON = replaceOutputXHTML(message);

                    details = "<b>Authorized Amount:</b> $" + authorizedAmount + "<br>"
                            + "<b>Total Fees Amount:</b> $" + totalFeesAmount + "<br>"
                            + "<b>Partial Auth Amount:</b> $" + partialAuthorizedAmount + "<br>"
                            + "<b>Requested Amount:</b> $" + requestedAmount + "<br>"
                            + "<b>Surcharge:</b> $" + surchargeAmount + "<br>"
                            + "<b>Tip:</b> $" + tipAmount  + "<br>"
                            + "<b>Payment Brand:</b> " + paymentBrand  + "<br>"
                            + "<b>Acquirer Transaction ID:</b><br>" + acquirerTransactionID  + "<br>"
                            + "<b>POI Transaction ID:</b><br>" + poiTransactionID  + "<br>"
                            + "<b>Masked PAN:</b> " + maskedPAN  + "<br>"
                            + "<b>Entry Mode:</b> " + entryMode  + "<br>"
                            + "<b>Account:</b> " + account  + "<br>"
                            + "<b>Card Expiry:</b> " + cardExpiry  + "<br>"
                            + "<b>Payment Instrument Type:</b><br>" + paymentInstrumentType+ "<br>"
                            + "<b>Trimmed JSON Response:</b><br>" + prettyPrintJson(trimmedJSON) + "<br>"
//                            + "<b>Full JSON Response:</b><br>" + message + "<br>"
                    ;

                    tvMessageDetail.setText(HtmlCompat.fromHtml(details, HtmlCompat.FROM_HTML_MODE_COMPACT));
                    break;
                case Abort:
                    tvMessageHead.setText("Transaction Cancelled");
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));


                    details = "<b>Authorized Amount:</b> $" + authorizedAmount + "<br>"
                            + "<b>Surcharge:</b> $" + surchargeAmount + "<br>"
                            + "<b>Tip:</b> $" + tipAmount  + "</b><br>"
                            + "<b>Full JSON Response:</b><br>" + message + "<br>";

                    tvMessageDetail.setText(HtmlCompat.fromHtml(details, HtmlCompat.FROM_HTML_MODE_COMPACT));
                    break;
                default:
                    tvMessageHead.setText("Error \n" + mc);
                    break;
            }
        } else {
            String errorMessage = "";
            tvMessageHead.setTextColor(Color.parseColor("#FF0000"));
            errorCondition = paymentResponse.getResponse().getErrorCondition();
            additionalResponse = paymentResponse.getResponse().getAdditionalResponse();

            tvMessageHead.setText("Transaction " + responseResult);
            errorMessage = "<b>ErrorCode:</b> " + errorCondition
                    + "<br><br>"
                    + "<b>AdditionalResponse:</b><br>" + additionalResponse + "</b><br>"
                    + "<b>Full JSON Response:</b><br>" + message + "<br>";
            tvMessageDetail.setText(HtmlCompat.fromHtml(errorMessage, HtmlCompat.FROM_HTML_MODE_COMPACT));

        }

        tvMessageDetail.setMovementMethod(new ScrollingMovementMethod());

        // Show receipt if available, else show details
//        if(!outputXHTML.equals(noValue)){
//            WebSettings settings = tvReceipt.getSettings();
//            settings.setDefaultTextEncodingName("utf-8");
//            tvReceipt.loadDataWithBaseURL(null, outputXHTML, "text/html", "utf-8", null);
//            settings.setTextSize(WebSettings.TextSize.SMALLER);
//        }

        btnPrintReceipt.setOnClickListener(v -> {
            try {
                startPrint();
            } catch (RemoteException | IOException e) {
                e.printStackTrace();
            }
        });
        btnBack.setOnClickListener(view -> {
//            openActivityPrevious(prevClass);
            openActivityMain();
        });
    }
    private void startPrint() throws RemoteException, IOException{
        new Thread(new Runnable(){
            public void run() {
                try {
                    byte[] image = Utils.readAssetsFile(getApplicationContext(), "DMGReceipt.png");
                    Bitmap logoBitmap = BitmapFactory.decodeByteArray(image, 0,image.length);
                    bitmap = Utils.generateReceiptBitmap(getApplicationContext(), outputXHTML, isApproved);

                    //TODO ADD print text or createbitmap
                    device.printBitmap(logoBitmap);
                    device.printBitmap(bitmap);
                } catch (RemoteException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    // TODO: For future enhancement, uncomment below
//    public void openActivityPrevious(Class prevClass) {
//        Class nextClass = MainActivity.class;
//        //Redirect to previous page
//        if(prevClass.equals(ActivityRequests.class)){
//            nextClass = ActivityOtherRequests.class;
//        }else{
//            nextClass = ActivityPayment.class;
//        }
//        Intent intent = new Intent(this, nextClass);
//
//        startActivity(intent);
//    }

    public void openActivityMain(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    public static <T> T defaultWhenNull(@Nullable T object, @NonNull T def) {
        return (object == null) ? def : object;
    }

    public String replaceOutputXHTML(Message message){
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        JsonFactory factory = mapper.getFactory();
        JsonParser createParser = null;
        String path = null;
        String path2 = null;
        String trimmedMessage = "";
        try {
            createParser = factory.createParser(message.toString());
            JsonNode actualObj1 = mapper.readTree(createParser);
            path = actualObj1.findPath("PaymentReceipt").get(0).get("OutputContent").get("OutputXHTML").toString();
            path2 = actualObj1.findPath("PaymentReceipt").get(1).get("OutputContent").get("OutputXHTML").toString();
            trimmedMessage = message.toString().replace(path, "\"\"");
            trimmedMessage = trimmedMessage.replace(path2, "\"\"");
        } catch (IOException e) {
            // TODO log cannot trim
            trimmedMessage = message.toString();
        }
        return trimmedMessage;
    }

    public String prettyPrintJson(String uglyJsonString) {
        JSONObject json = null;
        String prettyJson = null;
        try {
            json = new JSONObject(uglyJsonString);
            prettyJson = json.toString(2);
        } catch (JSONException e) {
            //TODO
            prettyJson = uglyJsonString;
        }
        return prettyJson;
    }
}