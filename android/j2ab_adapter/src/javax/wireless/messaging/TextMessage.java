package javax.wireless.messaging;

import java.util.Date;

public class TextMessage implements Message {
    
    protected TextMessage() {
        mTimeStamp = new Date();
    }
    
    protected TextMessage (String addr, String text) {
        mPayloadText = text;
        mAddress = addr;
        mTimeStamp = new Date();
    }
    
    private String mPayloadText;
    private String mAddress;
    private Date mTimeStamp;
    
    public String getPayloadText() {
        return mPayloadText;
    }
    
    public void setPayloadText(String data)  {
        mPayloadText = data;
    }

    public String getAddress() {
        return mAddress;
    }

    public Date getTimestamp() {
        return mTimeStamp;
    }

    public void setAddress(String addr) {
        mAddress = addr;
    }
}
