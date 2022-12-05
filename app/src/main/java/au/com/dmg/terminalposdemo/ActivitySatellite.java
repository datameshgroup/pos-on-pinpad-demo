package au.com.dmg.terminalposdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import au.com.dmg.terminalposdemo.R;

public class ActivitySatellite extends AppCompatActivity {

    private Button btnReversalReq;
    private Button btnCashoutReq;
    private Button btnRefundReq;
    private Button btnPreauthReq;
    private Button btnCompletionReq;
    private Button btnTransactionStatusReq;
    private Button btnCardAcquisitionReq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite);

        btnReversalReq = (Button) findViewById(R.id.btnReversalReq);
        btnCashoutReq = (Button) findViewById(R.id.btnCashoutReq);
        btnRefundReq = (Button) findViewById(R.id.btnRefundReq);
        btnPreauthReq = (Button) findViewById(R.id.btnPreauthReq);
        btnCompletionReq = (Button) findViewById(R.id.btnCompletionReq);
        btnTransactionStatusReq = (Button) findViewById(R.id.btnTransactionStatusReq);
        btnCardAcquisitionReq = (Button) findViewById(R.id.btnCardAcquisitionReq);

        btnRefundReq.setOnClickListener(v -> openActivityRequests("refund"));
        btnCashoutReq.setOnClickListener(v -> openActivityRequests("cashout"));
        btnReversalReq.setOnClickListener(v -> openActivityRequests("reversal"));
        btnPreauthReq.setOnClickListener(v -> openActivityRequests("preauth"));
        btnCompletionReq.setOnClickListener(v -> openActivityRequests("completion"));
        btnTransactionStatusReq.setOnClickListener(v -> openActivityRequests("txnstatus"));
        btnCardAcquisitionReq.setOnClickListener(v -> openActivityRequests("cardacq"));

    }
    public void openActivityRequests(String req) {
        Intent intent = new Intent(this, ActivityRequests.class);

        intent.putExtra("requestName", req);

        startActivity(intent);
    }

}