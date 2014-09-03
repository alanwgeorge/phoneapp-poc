package com.alangeorge.phoneapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alangeorge.phoneapp.SMSSendReceiver.SENT_ACTION;
import static com.alangeorge.phoneapp.SMSDeliveredReceiver.DELIVERED_ACTION;


public class MainActivity extends ActionBarActivity {

    public static final String EXTRA_MESSAGE = "message";
    private static final String DISPLAY_MESSAGE_DISPLAY = "com.alangeorge.phoneapp.DISPLAY_MESSAGE";
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String SENDER_ID = "323620638301";
    private static final String MESSAGES_STATE_BUNDLE_KEY = "MESSAGES_STATE_BUNDLE_KEY";
    private static final String OUTGOING_PHONE_NUMBER_STATE_BUNDLE_KEY = "OUTGOING_PHONE_NUMBER_STATE_BUNDLE_KEY";

    private GoogleCloudMessaging gcm;
    private String regid = null;
    private AtomicInteger msgId = new AtomicInteger();
    private String simPhoneNumber = null;
    private String outgoingPhoneNumber = null;
    private TextView messagesTextView = null;
    private Button callButton = null;
    private Button smsButton = null;
    private Button pnButton = null;

    private Context context;

    public final BroadcastReceiver messagesHandlerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            messagesTextView.append(newMessage + '\n');
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        simPhoneNumber = manager.getLine1Number();
        outgoingPhoneNumber = simPhoneNumber;

        TextView simPhoneNumberTextView = (TextView) findViewById(R.id.phone_number_textView);
        messagesTextView = (TextView) findViewById(R.id.messagesTextView);
        callButton = (Button) findViewById(R.id.callButton);
        smsButton = (Button) findViewById(R.id.smsButton);
        pnButton = (Button) findViewById(R.id.pnButton);

        if (simPhoneNumber != null && !simPhoneNumber.isEmpty()) {
            simPhoneNumberTextView.setText(simPhoneNumber);
            callButton.setText(getString(R.string.call_button_prefix) + simPhoneNumber);
            smsButton.setText(getString(R.string.echo_sms_button_prefix) + simPhoneNumber);
            callButton.setEnabled(true);
            smsButton.setEnabled(true);
        } else {
            simPhoneNumberTextView.setText(getString(R.string.phone_number_not_found_label));
            callButton.setText(getString(R.string.phone_number_not_found_label));
            callButton.setEnabled(false);
            smsButton.setText(getString(R.string.phone_number_not_found_label));
            smsButton.setEnabled(false);
        }


        ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(new PhoneListener(), PhoneStateListener.LISTEN_CALL_STATE);

        registerReceiver(messagesHandlerReceiver, new IntentFilter(DISPLAY_MESSAGE_DISPLAY));

        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        if (regid != null) {
            pnButton.setText(getString(R.string.pn_button_prefix) + regid);
            pnButton.setEnabled(true);
        } else {
            pnButton.setText(getString(R.string.pn_reg_id_unknown_label));
            pnButton.setEnabled(false);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(MESSAGES_STATE_BUNDLE_KEY, messagesTextView.getText().toString());
        state.putString(OUTGOING_PHONE_NUMBER_STATE_BUNDLE_KEY, outgoingPhoneNumber);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(MESSAGES_STATE_BUNDLE_KEY)) {
            messagesTextView.setText(savedInstanceState.getCharSequence(MESSAGES_STATE_BUNDLE_KEY));
        }

        if (savedInstanceState.containsKey(OUTGOING_PHONE_NUMBER_STATE_BUNDLE_KEY)) {
            outgoingPhoneNumber = savedInstanceState.getCharSequence(OUTGOING_PHONE_NUMBER_STATE_BUNDLE_KEY).toString();
            changeOutgoingPhoneNumber(outgoingPhoneNumber);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(messagesHandlerReceiver);
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_change_outgoing_number:
                // get prompts.xml view
                LayoutInflater inflater = LayoutInflater.from(context);
                View promptsView = inflater.inflate(R.layout.phone_input_dialog, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView.findViewById(R.id.phoneNumberEditText);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        changeOutgoingPhoneNumber(userInput.getText().toString());
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    
    public void onCallButtonClick(View view) {
        Toast.makeText(this, "Clicked on Call Button", Toast.LENGTH_LONG).show();
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + outgoingPhoneNumber));
        startActivity(callIntent);
    }

    public void onSMSButtonClick(View view) {
        Toast.makeText(this, "Clicked on SMS Button", Toast.LENGTH_LONG).show();

        ArrayList<PendingIntent> sentSMSPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredSMSPendingIntents = new ArrayList<PendingIntent>();

        SmsManager sms = SmsManager.getDefault();
        // Message format: ECHO:provider|push registration id
        String message = "ECHO:" + "GCM|" + regid;
        ArrayList<String> msgParts = sms.divideMessage(message);

        for (String s : msgParts) {
            sentSMSPendingIntents.add(PendingIntent.getBroadcast(this, 0, new Intent(SENT_ACTION), 0));
            deliveredSMSPendingIntents.add(PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED_ACTION), 0));
        }
        sms.sendMultipartTextMessage(outgoingPhoneNumber, null, msgParts, sentSMSPendingIntents, deliveredSMSPendingIntents);
    }

    public void onPNButtonClick(View view) {
        Toast.makeText(this, "Clicked on PN Button", Toast.LENGTH_LONG).show();

        GcmIntentService.sendNotification(this, "helloworld");
    }

    public void changeOutgoingPhoneNumber(String newNumber) {
        this.outgoingPhoneNumber = newNumber;

        callButton.setText(getString(R.string.call_button_prefix) + outgoingPhoneNumber);
        smsButton.setText(getString(R.string.echo_sms_button_prefix) + outgoingPhoneNumber);
        callButton.setEnabled(true);
        smsButton.setEnabled(true);
    }

    public static void displayMessage(Context context, String message) {
        Intent intent = new Intent(DISPLAY_MESSAGE_DISPLAY);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d(TAG, msg);
                if (regid != null) {
                    pnButton.setText(getString(R.string.pn_button_prefix) + regid);
                    pnButton.setEnabled(true);
                } else {
                    pnButton.setText(getString(R.string.pn_reg_id_unknown_label));
                    pnButton.setEnabled(false);
                }
            }
        }.execute(null, null, null);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
    }
}
