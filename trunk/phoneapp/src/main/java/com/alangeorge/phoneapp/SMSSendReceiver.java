package com.alangeorge.phoneapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

public class SMSSendReceiver extends BroadcastReceiver {

    private static final String TAG = "SMSSendReceiver";
    public static final String SENT_ACTION = "SMS_SENT_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(" + intent + ")");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> bundleKeys = extras.keySet();
            for (String key : bundleKeys) {
                Log.d(TAG, "intent bundle key:value " + key + ":" + extras.get(key).toString());
            }
        }

        String message = "Unknown Send Result";
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                message = "SMS Sent";
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                message = "SMS: Generic Failure";
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                message = "SMS: No Service";
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                message = "SMS: Null PDU";
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                message = "SMS: Radio Off";
                break;
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
        MainActivity.displayMessage(context, message);

    }
}
