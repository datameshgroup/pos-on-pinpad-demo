package au.com.dmg.terminalposdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.request.paymentrequest.POITransactionID;


public class ActivityPreauthorisationList extends AppCompatActivity {
    private ListView lvPreauthorisationList;
    private TextView txtEmpty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//      TODO:  super.onBackPressed();
        setContentView(R.layout.activity_preauthorisation_list);

        lvPreauthorisationList = (ListView) findViewById(R.id.lvPreauthorisationList);
        txtEmpty = (TextView) findViewById(R.id.txtEmpty);

        GlobalClass globalClass = (GlobalClass)getApplicationContext();
        globalClass.initPreauthorisationList();

        ArrayList<GlobalClass.Preauthorisation> preauthorisationList = new ArrayList<>();

        preauthorisationList = globalClass.preauthorisationList;

        if(preauthorisationList == null || preauthorisationList.isEmpty()){
            lvPreauthorisationList.setVisibility(View.GONE);

            txtEmpty.setText("No previous preauthorisation");
            txtEmpty.setVisibility(View.VISIBLE);

        }else{
            AdapterPreauthorisation adapterPreauthorisation = new AdapterPreauthorisation(this, preauthorisationList);
            lvPreauthorisationList.setAdapter(adapterPreauthorisation);

            lvPreauthorisationList.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapter, View v, int position,
                                        long arg3)
                {
                    GlobalClass.Preauthorisation preauth = (GlobalClass.Preauthorisation) adapterPreauthorisation.getItem(position);


                    System.out.println(preauth.instant.toString());
                    System.out.println(preauth.saleReferenceID);
                    System.out.println(preauth.poiTransactionID.getTransactionID());
                    System.out.println(preauth.authorizedAmount);

                    openActivityRequests(PaymentType.Completion, preauth);

                }
            });

        }

    }

    //TODO: update this to just instant
    public void openActivityRequests(PaymentType req, GlobalClass.Preauthorisation preauth) {
        Intent intent = new Intent(this, ActivityRequests.class);
        intent.putExtra("paymentType", req);

        intent.putExtra("instant", preauth.instant);
//        intent.putExtra("salesReferenceID", preauth.saleReferenceID);
//        intent.putExtra("poiTransactionID", (CharSequence) poiTransactionID);
//        intent.putExtra("authorizedAmount", preauth.authorizedAmount);

        startActivity(intent);
    }
}

class AdapterPreauthorisation extends BaseAdapter {
    private ArrayList<GlobalClass.Preauthorisation> listData;
    private LayoutInflater layoutInflater;
    private Context context;

    public AdapterPreauthorisation(Context context, ArrayList<GlobalClass.Preauthorisation> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        //      int type = getItemViewType(position);

        if (convertView == null) {

            convertView = layoutInflater.inflate(R.layout.preauth_layout, null);
            holder = new ViewHolder();

            holder.timestamp = (TextView) convertView.findViewById(R.id.tvTimestamp);
            holder.amount = (TextView) convertView.findViewById(R.id.tvAmount);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        String strTimeStamp = DateTimeFormatter.ofPattern("dd/MM/YY hh:mm:ss a")
                .withZone(ZoneId.of("Australia/Sydney"))
                .format(Instant.parse(listData.get(position).instant.toString()));

        @SuppressLint("DefaultLocale") String strAuthorizedAmount = String.format("$%.02f", listData.get(position).authorizedAmount);

        holder.timestamp.setText(strTimeStamp);
        holder.amount.setText(strAuthorizedAmount);

        return convertView;
    }

    static class ViewHolder {
        TextView timestamp;
        TextView amount;
    }

}