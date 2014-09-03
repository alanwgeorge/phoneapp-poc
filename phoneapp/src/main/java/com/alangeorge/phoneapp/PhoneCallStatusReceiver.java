package com.alangeorge.phoneapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Set;

public class PhoneCallStatusReceiver extends BroadcastReceiver {


    private static final String TAG = "PhoneCallStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive(" + intent + ")");
        Bundle extras = intent.getExtras();
        Set<String> bundleKeys = extras.keySet();
        for (String key : bundleKeys) {
            Log.d(TAG, "intent bundle key:value " + key + ":" + extras.get(key).toString());
        }

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Log.d(TAG, "New Outgoing Call");
            MainActivity.displayMessage(context, "New Outgoing Call");
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != null && intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                TelephonyManager.EXTRA_STATE_RINGING)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER); // Phone number
            Log.d(TAG, "New Incoming Call, Phone ringing: " + incomingNumber);
            MainActivity.displayMessage(context, "New Incoming Call, Phone ringing: " + incomingNumber);
            // Ringing state
            // This code will execute when the phone has an incoming call
        } else if ((intent.getStringExtra(TelephonyManager.EXTRA_STATE) != null && intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE))) {
            // This code will execute when the call is disconnected
            Log.d(TAG, "Phone hung up");
            MainActivity.displayMessage(context, "Phone hung up");
        } else if ((intent.getStringExtra(TelephonyManager.EXTRA_STATE) != null && intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK))) {
            // This code will execute when the call is answered
            Log.d(TAG, "Phone answered");
            MainActivity.displayMessage(context, "Phone answered");
        }
    }
}
