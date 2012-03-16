package javax.wireless.messaging;

import java.io.IOException;

import javax.microedition.io.Connection;
import android.telephony.SmsManager;

public class MessageConnection implements Connection {

    public final static String TEXT_MESSAGE = "text";
    public final static String BINARY_MESSAGE = "binary";

    private String mName;

    public MessageConnection(String name) {
        mName = name;
    }

    public Message newMessage(String type) {
        if (TEXT_MESSAGE.equals(type)) {
            return new TextMessage();
        }
        if (BINARY_MESSAGE.equals(type)) {
            throw new UnsupportedOperationException("Binary not supported yet");
        }
        throw new IllegalArgumentException();
    }

    public Message newMessage(String type, String address) {
        final Message m = newMessage(type);
        m.setAddress(address);
        return m;
    }

    public int numberOfSegments(Message msg) {
        return msg == null ? 0 : 1;
    }

    public Message receive() {
        throw new UnsupportedOperationException("not implemented");
    }

    public void send(Message msg) {
        String number = mName.substring(6); // sms://
        if (msg instanceof TextMessage) {
            String payload = ((TextMessage)msg).getPayloadText();
            SmsManager.getDefault().sendTextMessage(number, null, payload, null, null);
            return;
        }
        throw new UnsupportedOperationException("no implemented");
    }

    public void setMessageListener(MessageListener l) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void close() throws IOException {
    }    
}
