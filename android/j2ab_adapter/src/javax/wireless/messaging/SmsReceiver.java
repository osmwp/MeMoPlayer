package javax.wireless.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            //---retrieve the SMS message received---
            byte[][] pdus = (byte[][]) bundle.get("pdus");
            for (byte[] pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu(pdu);
                new TextMessage(sms.getOriginatingAddress(), sms.getMessageBody());
            }
        }
    }
}