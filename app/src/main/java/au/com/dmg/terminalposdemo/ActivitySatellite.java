package au.com.dmg.terminalposdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.terminalposdemo.R;

public class ActivitySatellite extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite);

        Button btnReversalReq = (Button) findViewById(R.id.btnReversalReq);
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
        Intent intent = new Intent(this, ActivityPreauthorisationList.class);
        startActivity(intent);
    }

}