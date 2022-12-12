package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.Context;
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

public class ActivityResult extends AppCompatActivity {

    private Button btnPrintReceipt;
    private TextView tvResult;

    private DMGDeviceImpl device = new DMGDeviceImpl();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device.init(getApplicationContext());

        setContentView(R.layout.activity_result);

        tvResult = (TextView) findViewById(R.id.tvReceipt);
        btnPrintReceipt = (Button) findViewById(R.id.btnPrintReceipt);
        TextView tvReceipt = (TextView) findViewById(R.id.tvReceipt);
        TextView tvMessageHead = (TextView) findViewById(R.id.tvMessageHead);
        TextView tvMessageDetail = (TextView) findViewById(R.id.tvMessageDetail);

        String errorCondition = "";
        String additionalResponse = "";

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
                case "Refund":
                    txntype = "Refund";
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
                case "refund":
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
            errorCondition = getIntent().getStringExtra("errorCondition");;
            additionalResponse = getIntent().getStringExtra("additionalResponse");;
            btnPrintReceipt.setVisibility(View.GONE);
            tvReceipt.setVisibility(View.GONE);
            tvMessageDetail.setVisibility(View.VISIBLE);

            switch (errorCondition){

                case "NotConnectedException":
                    tvMessageHead.setText("No Internet");
                    tvMessageDetail.setText("Please make sure POS is connected to the internet");
                    break;
                case "Timeout":
                    tvMessageHead.setText("Transaction Timeout");
                    tvMessageDetail.setText("Transaction Aborted");
                    break;
                case "NotFound":
                    tvMessageHead.setText("Transaction Not Found");
                    tvMessageDetail.setText(additionalResponse);
                    break;
                case "Aborted":
                case "Busy":
                case "Cancel":
                case "DeviceOut":
                case "InsertedCard":
                case "InProgress":
                case "LoggedOut":
                case "MessageFormat":
                case "NotAllowed":
                case "PaymentRestriction":
                case "Refusal":
                case "UnavailableDevice":
                case "UnavailableService":
                case "InvalidCard":
                case "UnreachableHost":
                case "WrongPIN":
                default:
                    tvMessageHead.setText("Transaction Failed");
                    String errorMessage = errorCondition + "\n\n" + additionalResponse;
                    tvMessageDetail.setText(errorMessage);
            }
        }

    tvReceipt.setMovementMethod(new ScrollingMovementMethod());

        btnPrintReceipt.setOnClickListener(v -> {
            try {
                startPrint();
            } catch (RemoteException | IOException e) {
                e.printStackTrace();
            }
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

}