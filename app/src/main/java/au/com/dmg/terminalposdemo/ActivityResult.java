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
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.util.List;
import au.com.dmg.devices.TerminalDevice;
import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
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

        if(responseResult.equals(ResponseResult.Success)){
            isApproved = true;
            paymentType = paymentResult.getPaymentType();

            ///AmountsResp
            AmountsResp amountsResp = paymentResult.getAmountsResp();
            String authorizedAmount = StringUtils.defaultIfEmpty(amountsResp.getAuthorizedAmount().toString(), noValue);
            String cashBackAmount = StringUtils.defaultIfEmpty(amountsResp.getCashBackAmount().toString(), noValue);
            String tipAmount = StringUtils.defaultIfEmpty(amountsResp.getTipAmount().toString(), noValue);
            String surchargeAmount = StringUtils.defaultIfEmpty(amountsResp.getSurchargeAmount().toString(), noValue);

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
            String paymentInstrumentType = StringUtils.defaultIfEmpty(paymentInstrumentData.getPaymentInstrumentType(), noValue);

            ///CardData
            PaymentResponseCardData cardData = paymentInstrumentData.getCardData();
            String paymentBrand = StringUtils.defaultIfEmpty(cardData.getPaymentBrand().toString(), noValue);
            String maskedPAN =  StringUtils.defaultIfEmpty(cardData.getMaskedPAN(), noValue);
            String entryMode = StringUtils.defaultIfEmpty(cardData.getEntryMode().toString(), noValue);
            String account = StringUtils.defaultIfEmpty(cardData.getAccount(), noValue);

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

                    details = "<b>Authorized Amount:</b> $" + authorizedAmount + "<br>"
                            + "<b>Surcharge:</b> $" + surchargeAmount + "<br>"
                            + "<b>Tip:</b> $" + tipAmount  + "<br>"
                            + "<b>Payment Brand:</b> " + paymentBrand  + "<br>"
                            + "<b>Acquirer Transaction ID:</b><br>" + acquirerTransactionID  + "<br>"
                            + "<b>POI Transaction ID:</b><br>" + poiTransactionID  + "<br>"
                            + "<b>Masked PAN:</b> " + maskedPAN  + "<br>"
                            + "<b>Entry Mode:</b> " + entryMode  + "<br>"
                            + "<b>Account:</b> " + account  + "<br>"
                            + "<b>Payment Instrument Type:</b><br>" + paymentInstrumentType+ "<br>"
                            + "<b>Full JSON Response:</b><br>" + message + "<br>";

                    tvMessageDetail.setText(HtmlCompat.fromHtml(details, HtmlCompat.FROM_HTML_MODE_COMPACT));
                    break;
                case Abort:
                    tvMessageHead.setText("Transaction Cancelled");
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));


                    details = "<b>Authorized Amount:</b> $" + authorizedAmount + "<br>"
                            + "<b>Surcharge:</b> $" + surchargeAmount + "<br>"
                            + "<b>Tip:</b> $" + tipAmount  + "<br>";

                    tvMessageDetail.setText(HtmlCompat.fromHtml(details, HtmlCompat.FROM_HTML_MODE_COMPACT));
                    break;
                default:
                    tvMessageHead.setText("Error \n" + mc);
                    break;
            }

        }
        else {
            String errorMessage = "";
            tvMessageHead.setTextColor(Color.parseColor("#FF0000"));
            errorCondition = paymentResponse.getResponse().getErrorCondition();
            additionalResponse = paymentResponse.getResponse().getAdditionalResponse();

            tvMessageHead.setText("Transaction " + responseResult);
            errorMessage = "<b>ErrorCode:</b> " + errorCondition
                    + "<br><br>"
                    + "<b>AdditionalResponse:</b><br>" + additionalResponse;
            tvMessageDetail.setText(HtmlCompat.fromHtml(errorMessage, HtmlCompat.FROM_HTML_MODE_COMPACT));

        }

        tvMessageDetail.setMovementMethod(new ScrollingMovementMethod());

        // Show receipt if available, else show details
        if(!outputXHTML.equals(noValue)){
            WebSettings settings = tvReceipt.getSettings();
            settings.setDefaultTextEncodingName("utf-8");
            tvReceipt.loadDataWithBaseURL(null, outputXHTML, "text/html", "utf-8", null);
            settings.setTextSize(WebSettings.TextSize.SMALLER);
        }

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
}