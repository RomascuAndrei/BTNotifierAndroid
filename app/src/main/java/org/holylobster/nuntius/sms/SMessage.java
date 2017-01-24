package org.holylobster.nuntius.sms;

import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Created by fly on 21/06/15.
 */
public class SMessage {
    private String sender;
    private String senderNum;
    private String message;
    private Context context;


    public SMessage(String senderNum, String message) {
        this.senderNum = senderNum;
        this.message = message;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public String getSenderNum() {
        return senderNum;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SMessage{" +
                "sender='" + sender + '\'' +
                ", senderNum='" + senderNum + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
