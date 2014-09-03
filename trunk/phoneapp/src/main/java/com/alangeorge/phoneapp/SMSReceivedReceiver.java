package com.alangeorge.phoneapp;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

public class SMSReceivedReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceivedReceiver";

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

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            String from = null;
            StringBuffer wholeMessage = new StringBuffer();
            if (bundle != null){
                // retrieve the SMS message received
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus"); // PDU = Protocol Description Unit
                    msgs = new SmsMessage[pdus.length];
                    Log.d(TAG, "Received " + msgs.length + " message parts");
                    for(int i = 0; i < msgs.length; i++){
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        wholeMessage.append(msgBody);
                        Log.d(TAG, "SMS From: " + from + " message: " + msgBody);
                        MainActivity.displayMessage(context, "SMS From: " + from + " message: " + msgBody);
                    }
                }catch(Exception e){
                    Log.d(TAG, "Exception caught: " + e.getMessage());
                }

                if (wholeMessage.toString().startsWith(context.getString(R.string.sms_echo_command_prefix))) {
                    if ((from != null && ! "".equals(from))  && ! wholeMessage.toString().isEmpty()) {
                        SendEchoPushTask sendTask = new SendEchoPushTask(context);
                        sendTask.execute(wholeMessage.toString(), from);

                        if (sendTask.getException() != null) {
                            MainActivity.displayMessage(context, "Error sending Echo PN: " + sendTask.getException().getLocalizedMessage());
                        } else {
                            MainActivity.displayMessage(context, "ECHO PN sent with phone number " + from);
                        }
                    }
                }
            }
        }
    }

    private static class SendEchoPushTask extends AsyncTask<String, Void, Result> {
        private Context context;

        private Exception exception = null;

        public SendEchoPushTask(Context context) {
            this.context = context;
        }

        @Override
        protected Result doInBackground(String... params) {
            String wholeMessage = params[0];
            String fromPhoneNumber = params[1];

            String regid = wholeMessage.substring(context.getString(R.string.sms_echo_command_prefix).length());
            Log.d(TAG, "from:regid = " + fromPhoneNumber + ":" + regid);

            Sender sender = new Sender(context.getString(R.string.gcm_sender_key));
            Message.Builder builder = new Message.Builder();
            builder.addData(context.getString(R.string.gcm_message_field_phonenumber), fromPhoneNumber);
            Message message = builder.build();
            Result result = null;
            try {
                result = sender.send(message, regid, 5);
            } catch (IOException e) {
                this.exception = e;
            }
            Log.d(TAG, "sent PN with result: " + result);

            if (result != null && result.getErrorCodeName() != null) {
                exception = new Exception(result.getErrorCodeName());
            }

            if (result != null && result.getCanonicalRegistrationId() != null) {
                Log.i(TAG, "Canonical ID returned");
            }

            return result;
        }

        public Exception getException() {
            return exception;
        }
    }
}
