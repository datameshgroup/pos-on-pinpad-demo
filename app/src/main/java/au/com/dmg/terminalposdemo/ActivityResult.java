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
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;


import java.io.IOException;
import java.util.Objects;

import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.TransactionType;

public class ActivityResult extends AppCompatActivity {

    private Button btnPrintReceipt;
    private Button btnBack;
    private TextView tvResult;

    TransactionType transactionType;
//    Class prevClass;

    private DMGDeviceImpl device = new DMGDeviceImpl();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device.init(getApplicationContext());

        setContentView(R.layout.activity_result);

        Bundle bundle = getIntent().getExtras();
//        prevClass = (Class) bundle.get("prevClass");

        tvResult = (TextView) findViewById(R.id.tvReceipt);
        btnPrintReceipt = (Button) findViewById(R.id.btnPrintReceipt);
        btnBack = (Button) findViewById(R.id.btnBack);
        TextView tvReceipt = (TextView) findViewById(R.id.tvReceipt);
        TextView tvMessageHead = (TextView) findViewById(R.id.tvMessageHead);
        TextView tvMessageDetail = (TextView) findViewById(R.id.tvMessageDetail);

        tvReceipt.setMovementMethod(new ScrollingMovementMethod());

        ErrorCondition errorCondition = null;
        String additionalResponse = "";

        //TODO: Cleanup
        //TODO: add home button top? fix back button or do fullscreen
        //TODO: don't show completion page if no previous preauth
        String txntype = getIntent().getStringExtra("txnType");

        String result = getIntent().getStringExtra("result");

        ///AmountsResp
        String authorizedAmount = getIntent().getStringExtra("AuthorizedAmount");
//        String totalFeesAmount = getIntent().getStringExtra("TotalFeesAmount");
        String cashBackAmount = getIntent().getStringExtra("CashBackAmount");
        String tipAmount = getIntent().getStringExtra("TipAmount");
        String surchargeAmount = getIntent().getStringExtra("SurchargeAmount");

        ///PaymentInstrumentData
        String paymentInstrumentType = getIntent().getStringExtra("PaymentInstrumentType");

        ///CardData
        String paymentBrand = getIntent().getStringExtra("PaymentBrand");
        String maskedPAN = getIntent().getStringExtra("MaskedPAN");
        String entryMode = getIntent().getStringExtra("EntryMode");
        String account = getIntent().getStringExtra("Account");

        ///PaymentAcquirerData
        String approvalCode = "";
        String transactionID = "";

        ///PaymentReceipt
        String outputXHTML = getIntent().getStringExtra("OutputXHTML");

        if(Objects.equals(result, "Success")){
            switch (txntype) {
                case "Payment":
                    //PaymentAcquirerData
                    approvalCode = getIntent().getStringExtra("ApprovalCode");
                    transactionID = getIntent().getStringExtra("TransactionID");

                case "TransactionStatus":
                    tvMessageHead.setText("Payment " + result);
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    tvMessageDetail.setText("Authorized Amount: " + authorizedAmount + "\n"
                            + "Surcharge: " + surchargeAmount + "\n"
                            + "Tip: " + tipAmount  + "\n"
                            + "Payment Brand: " + paymentBrand  + "\n"
                            + "Transaction ID: " + transactionID  + "\n"
                            + "Masked PAN: " + maskedPAN  + "\n"
                            + "Approval Code: " + approvalCode  + "\n"
                            + "Entry Mode: " + entryMode  + "\n"
                            + "Account: " + account  + "\n"
                            + "Payment Instrument Type: " + paymentInstrumentType
                    );
                    tvMessageDetail.setVisibility(View.GONE);
                    tvReceipt.setText(HtmlCompat.fromHtml(outputXHTML, HtmlCompat.FROM_HTML_MODE_COMPACT));
                    break;
                case "Refund":
                    tvMessageHead.setText("Refund " + result);
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    tvMessageDetail.setText("Authorized Amount: " + authorizedAmount + "\n");

                    tvReceipt.setText(HtmlCompat.fromHtml(outputXHTML, 0));
                    break;
                case "cancel":
                    tvMessageHead.setText("Transaction Cancelled");
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    tvMessageDetail.setText("Authorized Amount: " + authorizedAmount + "\n"
                            + "Surcharge: " + surchargeAmount + "\n"
                            + "Tip: " + tipAmount
                    );

                    tvReceipt.setText(HtmlCompat.fromHtml(outputXHTML, 0));
                    break;
                default:
                    tvMessageHead.setText("Error \n" + txntype);
                    break;
            }

        }

        else
        {
            tvMessageHead.setTextColor(Color.parseColor("#FF0000"));
            errorCondition = (ErrorCondition) bundle.get("errorCondition");
            additionalResponse = getIntent().getStringExtra("additionalResponse");;

            btnPrintReceipt.setVisibility(View.GONE);
            tvReceipt.setVisibility(View.GONE);
            tvMessageDetail.setVisibility(View.VISIBLE);

            tvMessageHead.setText(errorCondition.toString());
            tvMessageDetail.setText(additionalResponse);
//            switch (errorCondition){
//                case Aborted:
//                    tvMessageDetail.setText("Transaction aborted");
//                    break;
//                case Busy:
//                    tvMessageDetail.setText("The system is busy, try later");
//                    break;
//                case Cancel:
//                    tvMessageDetail.setText("Transaction Cancelled");
//                    break;
//                case PaymentRestriction:
//                    tvMessageDetail.setText("");
//                    break;
//                case Refusal:
//                    tvMessageDetail.setText("");
//                    break;
//                case Unknown:
//                    tvMessageDetail.setText("");
//                    break;
//                case NotFound:
//                    tvMessageDetail.setText("");
//                    break;
//                case WrongPIN:
//                    tvMessageDetail.setText("");
//                    break;
//                case DeviceOut:
//                    tvMessageDetail.setText("");
//                    break;
//                case LoggedOut:
//                    tvMessageDetail.setText("");
//                    break;
//                case InProgress:
//                    tvMessageDetail.setText("");
//                    break;
//                case NotAllowed:
//                    tvMessageDetail.setText("");
//                    break;
//                case InvalidCard:
//                    tvMessageDetail.setText("");
//                    break;
//                case InsertedCard:
//                    tvMessageDetail.setText("");
//                    break;
//                case MessageFormat:
//                    tvMessageDetail.setText("");
//                    break;
//                case UnreachableHost:
//                    tvMessageDetail.setText("");
//                    break;
//                case UnavailableDevice:
//                    tvMessageDetail.setText("");
//                    break;
//                case UnavailableService:
//                    tvMessageDetail.setText("");
//                    break;
//                default:
//                    tvMessageHead.setText("Transaction Failed");
//                    String errorMessage = errorCondition + "\n\n" + additionalResponse;
//                    tvMessageDetail.setText(errorMessage);
//            }
        }

    tvReceipt.setMovementMethod(new ScrollingMovementMethod());

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

                    String x = String.valueOf(tvResult.getText());
                    Bitmap bitmap = Utils.generateReceiptBitmap(getApplicationContext(), x, true);
                    //TODO ADD print text or createbitmap
                    device.printBitmap(logoBitmap);
                    device.printBitmap(bitmap);
                } catch (RemoteException | IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
//    public void openActivityPrevious(Class prevClass) {
//        Class nextClass = MainActivity.class;
//        //Redirect to previous page
//        if(prevClass.equals(ActivityRequests.class)){
//            nextClass = ActivitySatellite.class;
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
}