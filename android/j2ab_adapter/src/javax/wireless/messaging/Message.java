package javax.wireless.messaging;

import java.util.Date;

public interface Message {
    public String getAddress();
    public Date getTimestamp();
    public void setAddress(java.lang.String addr);
}
