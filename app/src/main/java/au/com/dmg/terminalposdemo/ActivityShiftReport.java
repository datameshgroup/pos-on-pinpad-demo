package au.com.dmg.terminalposdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arkapp.iosdatettimepicker.utils.OnDateTimeSelectedListener;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.ServiceIdentification;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.adminrequest.AdminRequest;
import au.com.dmg.fusion.request.adminrequest.PrintShiftTotalsRequest;

public class ActivityShiftReport extends AppCompatActivity {
    private Button btnPrintShiftReport;
    private Button btnPrintShiftReportBlank;
    private TextInputEditText txtShiftNumber;
    private TextInputEditText txtShiftStart;
    private TextInputEditText txtShiftEnd;
    private ImageView ivStart;
    private ImageView ivEnd;
    private String sentServiceID = "";

    private Instant instantShiftStart;
    private Instant instantShiftEnd;

    DateTimeFormatter patternFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    ZoneId zoneId = ZoneId.systemDefault();
    TimeZone localTimeZone = TimeZone.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_report_request);

        btnPrintShiftReport = (Button) findViewById(R.id.btnPrintShiftReport);
        btnPrintShiftReportBlank = (Button) findViewById(R.id.btnPrintShiftReportBlank);

        ivStart = (ImageView) findViewById(R.id.ivStart) ;
        ivEnd = (ImageView) findViewById(R.id.ivEnd) ;

        txtShiftNumber = (TextInputEditText) findViewById(R.id.txtShiftNumber);
        txtShiftStart = (TextInputEditText) findViewById(R.id.txtShiftStart);
        txtShiftEnd = (TextInputEditText) findViewById(R.id.txtShiftEnd);

        ivStart.setOnClickListener(
                v -> showPicker(txtShiftStart)
        );

        ivEnd.setOnClickListener(
                v -> showPicker(txtShiftEnd)
        );

        btnPrintShiftReport.setOnClickListener(v -> {
            if (areInputsValid()) {
                printShiftReport();
            }
        });

        btnPrintShiftReportBlank.setOnClickListener(v -> {
                printShiftReportBlank();
        });

//        registerReceiver(terminalInfoReceiver,  new IntentFilter("fusion_broadcast_receiver"));
    }

    private void showPicker(TextInputEditText txtShiftDateTime) {
        Calendar startDate;

        if (txtShiftDateTime.getText().length() == 0) {
            startDate = Calendar.getInstance(localTimeZone);
        } else {
            try {
                String stringDate = String.valueOf(txtShiftDateTime.getText());
                LocalDateTime localDateTime = LocalDateTime.parse(stringDate, patternFormatter);
                startDate = convertToCalendar(localDateTime);
            } catch (DateTimeParseException | IllegalArgumentException e) {
                startDate = Calendar.getInstance(localTimeZone);
            }
        }


        OnDateTimeSelectedListener dateTimeSelectedListener = selectedDateTime -> {
            String formattedDateTime = patternFormatter.format(selectedDateTime.toInstant().atZone(zoneId));
            txtShiftDateTime.setText(formattedDateTime);
        };

        startDate.add(Calendar.DAY_OF_MONTH, -1);
        com.arkapp.iosdatettimepicker.ui.DialogDateTimePicker dialog = new com.arkapp.iosdatettimepicker.ui.DialogDateTimePicker(
                ActivityShiftReport.this,
                startDate,
                2,
                dateTimeSelectedListener,
                "Select date and time"
        );

        dialog.setCancelBtnText("Cancel");
        dialog.setSubmitBtnText("Set");
        dialog.show();
    }

    private String generateServiceID() {
        return java.util.UUID.randomUUID().toString();
    }



    public Calendar convertToCalendar(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(zoneId).toInstant();
        Date date = Date.from(instant);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        System.out.println("LocalDateTime: " + localDateTime);
        System.out.println("Calendar: " + calendar);
        return calendar;
    }

    private boolean areInputsValid() {
        return isFieldValid(txtShiftNumber, "Shift Number")
                && isFieldValid(txtShiftStart, "Shift Start")
                && isFieldValid(txtShiftEnd, "Shift End")
                && areInstantsValid();
    }

    private boolean areInstantsValid() {
        if (isInstantValid(txtShiftStart, "Shift Start")
                && isInstantValid(txtShiftEnd, "Shift End")
                && instantShiftEnd.isAfter(instantShiftStart)){
            return true;
        }else{
            showToast("Shift Start should not be greater than Shift End");
            return false;
        }
    }


    private boolean isFieldValid(TextInputEditText field, String fieldName) {
        if (field.getText().length() < 1) {
            showToast(fieldName + " cannot be empty.");
            return false;
        }
        return true;
    }

    private boolean isInstantValid(TextInputEditText shiftDate, String fieldName) {
        String stringShiftDate = shiftDate.getText().toString();

        try {
            if(fieldName.equals("Shift Start")){
                instantShiftStart = LocalDateTime.parse(stringShiftDate, patternFormatter).atZone(zoneId).toInstant();
            } else {
                instantShiftEnd = LocalDateTime.parse(stringShiftDate, patternFormatter).atZone(zoneId).toInstant();
            }
            return true;

        } catch (DateTimeParseException e) {
            showToast(fieldName + " is not a valid Date format (yyyy-MM-dd HH:mm:ss).");
            return false;
        }
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    private void printShiftReport() {
        this.sentServiceID = generateServiceID();
        Log.d("TerminalPOSDemo","Printing shift report...ServiceID: " + this.sentServiceID);

        PrintShiftTotalsRequest printShiftTotalsRequest = new PrintShiftTotalsRequest.Builder()
                .shiftNumber(Objects.requireNonNull(txtShiftNumber.getText()).toString())
                .shiftStartTime(instantShiftStart)
                .shiftEndTime(instantShiftEnd)
                .build();

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .build()
                )
                .request(new AdminRequest.Builder()
                        .serviceIdentification(ServiceIdentification.PrintShiftTotals)
                        .printShiftTotalsRequest(printShiftTotalsRequest)
                        .build())
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message adminRequestMessage = new Message(adminRequest);
        Utils.showLog("adminRequestMessage", adminRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, adminRequestMessage.toJson());
        sendBroadcast(intent);
        finish();
    }

    private void printShiftReportBlank() {
        this.sentServiceID = generateServiceID();
        Log.d("TerminalPOSDemo","Printing shift report with no iputs...ServiceID: " + this.sentServiceID);

        SaleToPOIRequest adminRequest = new SaleToPOIRequest.Builder()
                .messageHeader(
                        new MessageHeader.Builder()
                                .messageClass(MessageClass.Service)
                                .messageCategory(MessageCategory.Admin)
                                .messageType(MessageType.Request)
                                .serviceID(this.sentServiceID)
                                .build()
                )
                .request(new AdminRequest.Builder()
                        .serviceIdentification(ServiceIdentification.PrintShiftTotals)
                        .build())
                .build();

        Intent intent = new Intent(Message.INTENT_ACTION_BROADCAST);
        Message adminRequestMessage = new Message(adminRequest);
        Utils.showLog("adminRequestMessage", adminRequestMessage.toJson());
        intent.putExtra(Message.INTENT_EXTRA_MESSAGE, adminRequestMessage.toJson());
        sendBroadcast(intent);
        finish();
    }

}
