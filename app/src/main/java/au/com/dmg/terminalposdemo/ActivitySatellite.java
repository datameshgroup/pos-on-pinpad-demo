package au.com.dmg.terminalposdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import au.com.dmg.fusion.data.PaymentType;

public class ActivitySatellite extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite);

//        Button btnReversalReq = (Button) findViewById(R.id.btnReversalReq);
        Button btnCashoutReq = (Button) findViewById(R.id.btnCashoutReq);
        Button btnRefundReq = (Button) findViewById(R.id.btnRefundReq);
        Button btnPreauthReq = (Button) findViewById(R.id.btnPreauthReq);
        Button btnCompletionReq = (Button) findViewById(R.id.btnCompletionReq);
        Button btnTransactionStatusReq = (Button) findViewById(R.id.btnTransactionStatusReq);
        Button btnCardAcquisitionReq = (Button) findViewById(R.id.btnCardAcquisitionReq);

        btnRefundReq.setOnClickListener(v -> openActivityRequests(PaymentType.Refund));
        btnCashoutReq.setOnClickListener(v -> openActivityRequests(PaymentType.CashAdvance));

        btnPreauthReq.setOnClickListener(v -> openActivityRequests(PaymentType.FirstReservation));
//        btnCompletionReq.setOnClickListener(v -> openActivityRequests(PaymentType.Completion));
        btnCompletionReq.setOnClickListener(v -> openActivityPreauthorisationList());
        btnTransactionStatusReq.setOnClickListener(v -> openActivityRequests(PaymentType.Normal));
//        btnReversalReq.setOnClickListener(v -> openActivityRequests("Reversal")); //NOT YET AVAILABLE
//        btnCardAcquisitionReq.setOnClickListener(v -> openActivityRequests("CardAcquisition")); //NOT YET AVAILABLE

    }
    public void openActivityRequests(PaymentType req) {
        Intent intent = new Intent(this, ActivityRequests.class);
        intent.putExtra("paymentType", req);
        startActivity(intent);
    }

    public void openActivityPreauthorisationList(){
//        Intent intent = new Intent(this, ActivityPreauthorisationList.class);
//        startActivity(intent);


        Intent reqIntent = new Intent("au.com.dmg.axispay");
        reqIntent.putExtra("TransType", "Completion Transaction");
        reqIntent.putExtra("Amount",5000);
        reqIntent.putExtra("OrigTransId" , "665eb261e5e25233ac024c4b");
        reqIntent.putExtra("POS", "Android POS App!");
        reqIntent.putExtra("EntryMode" , "File");
        reqIntent.putExtra("TokenRequestedType", "Customer");
        reqIntent.putExtra("TokenValue" , "6E5B3B37C880AE5DBD97168342D7377527F9149431B235");
        reqIntent.putExtra("Source", "POS App V0.00.00");


        startActivityForResult(reqIntent, 100);
    }

}