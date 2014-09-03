package com.alangeorge.phoneapp;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by ageo on 4/15/14.
 */
public class PhoneListener extends PhoneStateListener {
    private static final String TAG = "PhoneListener";

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE: // Hangup
                Log.d(TAG, "TelephonyManager.CALL_STATE_IDLE: " + incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK: //Outgoing
                Log.d(TAG, "TelephonyManager.CALL_STATE_OFFHOOK: " + incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_RINGING: //Incoming
                Log.d(TAG, "TelephonyManager.CALL_STATE_RINGING: " + incomingNumber);
                break;
            default:
                Log.d(TAG, "TelephonyManager." + state + ": " + incomingNumber);
        }
    }

}