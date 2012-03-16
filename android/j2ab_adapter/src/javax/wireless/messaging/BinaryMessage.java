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

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public Date getTimestamp() {
        return null;
    }

    @Override
    public void setAddress(String addr) {
    }

}
