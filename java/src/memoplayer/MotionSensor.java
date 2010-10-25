/*
 * Copyright (C) 2010 France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package memoplayer;
import java.io.IOException;
import javax.microedition.io.Connector;

//#ifdef api.sensor
//JSR 256 Mobile Sensor API
import javax.microedition.sensor.*;
//#endif

// filter elements to remove jitters 
// this is done by weigting new data with previous data
// warning: first entries may be incorrect
class LowPass {
    int [] m_data; 
    int m_size; // the number of samples to average 
    int m_current = 0; // the next free room
    int m_sum; // accumulate the sum of all data stored

    LowPass (int size) {
        m_data = new int [m_size = size];
        m_current = m_sum = 0; 
    }

    int filter (int n) {
        int old = m_data [m_current];
        m_data [m_current] = n;
        m_current = (m_current + 1) % m_size;
        m_sum += n - old;
        return FixFloat.time2fix(m_sum / m_size);
        //return n;
    }
}

public class MotionSensor extends Node 
//#ifdef api.sensor
implements DataListener 
//#endif
{
    
    int m_rotation = 0;
    private int x = 0;
    private int y = 0;
    private int z = 0;
    boolean m_rotationChanged = false;
    boolean m_accelerationChanged = false;

    private final String URL = "sensor:acceleration;contextType=user;model=ST_LIS302DL;location=inside";
//#ifdef api.sensor
    private SensorConnection sensor;
//#endif
    
    LowPass m_lpx, m_lpy, m_lpz;
    MotionSensor () {
        super (4);
        m_field[0] = new SFBool (true, this); // enabled
        m_field[1] = new SFVec3f (0, 0, 0);  // acceleration
        m_field[2] = new SFInt32 (0);  // rotation
        m_field[3] = new SFBool (false);  // isAvailable
        m_lpx = new LowPass (3);
        m_lpy = new LowPass (3);
        m_lpz = new LowPass (3);
    }
    public void start (Context C) {
        try {
//#ifdef api.sensor
            sensor = (SensorConnection)Connector.open(URL);
            ((SFBool)m_field[3]).setValue (true);
//#else
            ((SFBool)m_field[3]).setValue (false);
//#endif
            fieldChanged (m_field[0]);
        } catch (Exception e) {
            //e.printStackTrace();
//#ifdef api.sensor
            sensor = null;
//#endif
        }
    }

    public void stop (Context C) {
//#ifdef api.sensor
        try {
            if (sensor != null) {
                sensor.close();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
//#endif
    }
    
    public boolean compose (Context C, Region clip, boolean forceUpdate) {
        boolean result = false;
        if (m_accelerationChanged) {
            ((SFVec3f)m_field[1]).setValue (x,y,z);
            m_accelerationChanged = false;
            result = true;
        }
        if (m_rotationChanged) {
            ((SFInt32)m_field[2]).setValue (m_rotation);
            m_rotationChanged = false;
            result = true;
        }
        return result;
    }
    
    public void fieldChanged (Field f) {
//#ifdef api.sensor
        if (sensor != null) {
            if (((SFBool)f).getValue ()) {
                sensor.setDataListener (this, 1);
            } else {
                sensor.setDataListener (null, 0);
            }
        }
//#endif
    }
    
//#ifdef api.sensor
    public void dataReceived (SensorConnection sensor, Data[] data, boolean isDataLost){
        if (data.length == 3) {
            x = m_lpx.filter (data[0].getIntValues()[0]);
            y = m_lpy.filter (data[1].getIntValues()[0]);
            z = m_lpz.filter (data[2].getIntValues()[0]);
            
            int oldRotation = m_rotation;
            if (x>40000 && y>-10000 && y<10000 && z>14000){
                m_rotation=90;
            } else if(x>-30000 && y<-47000 && z>32000){
                m_rotation=180;
            } else if(x<-40000 && y<5000 && y>-5000 && z>33000){
                m_rotation=270;
            } else if(x<10000 && x>-10000 && y>33000 && z>-52000){
                m_rotation=0;
            }
            //Logger.println ("rot:"+newRotation+" x:"+x+", y:"+y+", z:"+z); 
            m_accelerationChanged = true;
            if (oldRotation != m_rotation){
                m_rotationChanged = true;
            }
            MiniPlayer.wakeUpCanvas();
        }
    }
    
    private int getMax (int[] values){
        int max = 0;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs (max) < Math.abs (values[i])) {
                max = values[i];
            } 
        }
        return max;
    }
//#endif

    /*
    private void printInfo() {
        //To query for all available sensors:
        //SensorInfo[] si = SensorManager.findSensors(null, null);
        //To query for a specific sensor:
        int[] channels = new int[3];
        String[] channelNames = new String[3];
        
        SensorInfo[] si = SensorManager.findSensors("acceleration", SensorInfo.CONTEXT_TYPE_USER);
        
        for (int i = 0; i < si.length; i++) {
            SensorInfo s = si[i];
            Logger.println("getDescription: " + s.getDescription());
            Logger.println("isAvailable: " + s.isAvailable());
            Logger.println("isAvailabilityPushSupported: " + s.isAvailabilityPushSupported());
            Logger.println("isConditionPushSupported: " + s.isConditionPushSupported());
            Logger.println("getConnectionType: " + s.getConnectionType());
            Logger.println("getContextType: " + s.getContextType());
            Logger.println("getMaxBufferSize: " + s.getMaxBufferSize());
            Logger.println("getModel: " + s.getModel());
            Logger.println("getQuantity: " + s.getQuantity());
            //URL = s.getUrl();
            Logger.println("getUrl: " + s.getUrl());
            Logger.println("Properties:");
            String[] propNames = s.getPropertyNames();
            for (int r = 0; r < propNames.length; r++) {
                Logger.println("  " + propNames[r] + ": " + s.getProperty(propNames[r]));
            }
            ChannelInfo[] cInfos = s.getChannelInfos();
            if (cInfos.length != 3) {
                Logger.println("Should support 3 channels but reports " + cInfos.length);
            } else {
                Logger.println("Channels:");
                for (int r = 0; r < cInfos.length; r++) {
                    ChannelInfo c = cInfos[r];
                    channelNames[r] = c.getName();
                    Logger.println("  getName: " + channelNames[r]);
                    int type = c.getDataType();
                    switch (type) {
                        case ChannelInfo.TYPE_INT:
                            Logger.println("  Data type: TYPE_INT");
                            break;
                        case ChannelInfo.TYPE_DOUBLE:
                            Logger.println("  Data type: TYPE_DOUBLE");
                            Logger.println("Should be TYPE_INT");
                            break;
                        case ChannelInfo.TYPE_OBJECT:
                            Logger.println("  Data type: TYPE_OBJECT");
                            Logger.println("Should be TYPE_INT");
                            break;
                    }
                    Logger.println("  getScale: " + c.getScale());
                    Logger.println("  getUnit: " + c.getUnit().toString());
                    Logger.println("  getAccuracy: " + c.getAccuracy());
                    MeasurementRange[] ranges = c.getMeasurementRanges();
                    Logger.println("  Measurement range:");
                    for (int p = 0; p < ranges.length; p++) {
                        MeasurementRange m = ranges[p];
                        Logger.println("    getSmallestValue:" + m.getSmallestValue());
                        Logger.println("    getLargestValue: " + m.getLargestValue());
                        Logger.println("    getResolution: " + m.getResolution());
                    }
                }
            }
        }
    }*/
}

