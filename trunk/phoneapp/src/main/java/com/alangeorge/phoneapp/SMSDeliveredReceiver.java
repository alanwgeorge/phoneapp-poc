package com.alangeorge.phoneapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

public class SMSDeliveredReceiver extends BroadcastReceiver {
    private final static String TAG = "SMSDeliveredReceiver";
    public final static String DELIVERED_ACTION = "SMS_DELIVERED_ACTION";

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


        String message = "Unknown Delivered Result";
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                message = "SMS Delivered";
                break;
            case Activity.RESULT_CANCELED:
                message = "SMS Not Delivered";
                break;
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        MainActivity.displayMessage(context, message);
    }
}
