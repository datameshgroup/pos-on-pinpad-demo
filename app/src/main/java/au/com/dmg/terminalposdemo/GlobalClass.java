package au.com.dmg.terminalposdemo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import java.io.Serializable;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import au.com.dmg.fusion.request.paymentrequest.POITransactionID;
import au.com.dmg.fusion.response.SaleToPOIResponse;

// Storing last response on this class for transaction status and completion request
public class GlobalClass extends Application {

    ///Temporary response storage used for Transaction Status Request
    private SaleToPOIResponse gResponse;
    public SaleToPOIResponse getResponse() { return gResponse; }
    public void setResponse(SaleToPOIResponse response){ this.gResponse = response; }

    ///Temporary preauthorisation storage used for Completion Request

    static class Preauthorisation {
        Instant instant;
        String saleReferenceID;
        POITransactionID poiTransactionID;
        BigDecimal authorizedAmount;
    }

    ArrayList<Preauthorisation> preauthorisationList = new ArrayList<>();
    SharedPreferences sharedPrefs;

    public void initPreauthorisationList() {
        preauthorisationList = new ArrayList<>();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("GlobalPreauthorisation", "");
        Type type = new TypeToken<List<GlobalClass.Preauthorisation>>() {}.getType();
        preauthorisationList = gson.fromJson(json, type);
    }

    public void addPreauthorisation(SaleToPOIResponse response) {
        Instant timestamp = response.getPaymentResponse().getSaleData().getSaleTransactionID().getTimestamp();
        Preauthorisation preauthorisation = new Preauthorisation();
        preauthorisation.instant = timestamp;
        preauthorisation.poiTransactionID = response.getPaymentResponse().getPoiData().getPOITransactionID();
        preauthorisation.authorizedAmount = response.getPaymentResponse().getPaymentResult().getAmountsResp().getAuthorizedAmount();
        preauthorisation.saleReferenceID = "1"; //response.getPaymentResponse().getSaleData().getSaleReferenceID();
        preauthorisationList.add(preauthorisation);

        // save the task list to sharedpreferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(this.preauthorisationList);
        editor.putString("GlobalPreauthorisation", json);
        editor.commit();
    }

    public void removePreauthorisation(Instant timestamp) {
        int index = getIndex(timestamp);
        preauthorisationList.remove(index);

        // Repopulate sharedpref
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.clear();
        editor.apply();

        Gson gson = new Gson();
        String json = gson.toJson(preauthorisationList);
        editor.putString("GlobalPreauthorisation", json);
        editor.commit();
    }

    public Preauthorisation getPreauthorisation(Instant timestamp) {
        // Get index
        int index = getIndex(timestamp);
        return preauthorisationList.get(index);
    }

    public int getIndex(Instant timestamp){
        int listSize = preauthorisationList.size() - 1;
        for(int cnt = 0; cnt <= listSize; cnt++)
        {
            if(preauthorisationList.get(cnt).instant.equals(timestamp)){
                return cnt;
            }
        }
        return -1; //not found

    }


}
