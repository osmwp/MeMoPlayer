//#condition api.mm

package com.sonyericsson.media.control; 

import javax.microedition.media.*;

public interface DisplayModeControl extends Control { 
     public void setDisplayMode(int displayMode);  
     public int getDisplayMode(); 
}
