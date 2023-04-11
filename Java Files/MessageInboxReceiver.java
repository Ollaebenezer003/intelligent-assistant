package com.example.olla.smartassistant;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class MessageInboxReceiver extends BroadcastReceiver {

    public static final String SMS_BUNDLE = "pdus";

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String smsMessageStr = "";
        String format = bundle.getString("format");

        // Retrieve the sms message received
        Object[] smsPdus = (Object[]) bundle.get(SMS_BUNDLE);

        if (smsPdus != null){
            // check the android version
            boolean isVersionM = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);

            // fill the msgsarray
            msgs = new SmsMessage[smsPdus.length];

            for (int i = 0; i < msgs.length; ++i) {

                // check android version and use appropriate createFromPdu

                if (isVersionM) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) smsPdus[i], format);
                } else {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) smsPdus[i]);
                }

                smsMessageStr += "SMS From: " + msgs[i].getOriginatingAddress();
                ;
                smsMessageStr += " :" + msgs[i].getMessageBody() + "\n";

                Toast.makeText(context, smsMessageStr, Toast.LENGTH_SHORT).show();

                // Update the UI with message
                MessageInbox inst = MessageInbox.instance();
                inst.updateList(smsMessageStr);
            }
        }

    }

}
