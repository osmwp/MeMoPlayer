package javax.wireless.messaging;

import java.util.Date;

public class BinaryMessage implements Message {
    private byte[] mPayloadData;
    
    public byte[] getPayloadData() {
        return mPayloadData;
    }
    
    public void setPayloadData(byte[] data)  {
        mPayloadData = data;
    }

    public String getAddress() {
        return null;
    }

    public Date getTimestamp() {
        return null;
    }

    public void setAddress(String addr) {
    }

}
