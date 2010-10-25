//#condition api.location
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

public class JSLocation {
    
    // constants
    static final double sq2p1 = 2.414213562373095048802e0;
    static final double sq2m1  = .414213562373095048802e0;
    static final double p4  = .161536412982230228262e2;
    static final double p3  = .26842548195503973794141e3;
    static final double p2  = .11530293515404850115428136e4;
    static final double p1  = .178040631643319697105464587e4;
    static final double p0  = .89678597403663861959987488e3;
    static final double q4  = .5895697050844462222791e2;
    static final double q3  = .536265374031215315104235e3;
    static final double q2  = .16667838148816337184521798e4;
    static final double q1  = .207933497444540981287275926e4;
    static final double q0  = .89678597403663861962481162e3;
    static final double PIO2 = 1.5707963267948966135E0;
    static final double nan = (0.0/0.0);
    
    // reduce
    private static double mxatan(double arg)
    {
        double argsq, value;

        argsq = arg*arg;
        value = ((((p4*argsq + p3)*argsq + p2)*argsq + p1)*argsq + p0);
        value = value/(((((argsq + q4)*argsq + q3)*argsq + q2)*argsq + q1)*argsq + q0);
        return value*arg;
    }

    // reduce
    private static double msatan(double arg)
    {
        if(arg < sq2m1)
            return mxatan(arg);
        if(arg > sq2p1)
            return PIO2 - mxatan(1/arg);
            return PIO2/2 + mxatan((arg-1)/(arg+1));
    }

    // implementation of atan
    public static double atan(double arg)
    {
        if(arg > 0)
            return msatan(arg);
        return -msatan(-arg);
    }

    // implementation of atan2
    public static double atan2(double arg1, double arg2)
    {
        if(arg1+arg2 == arg1)
        {
            if(arg1 >= 0)
            return PIO2;
                return -PIO2;
        }
        arg1 = atan(arg1/arg2);
        if(arg2 < 0)
       {
            if(arg1 <= 0)
                return arg1 + Math.PI;
            return arg1 - Math.PI;
        }
        return arg1;
    
    }
    
    // computes distance between two points.
    // Latitude and longitude are defined in decimal degree, integer (float value multiply by 100000)
    public static int getDistance(int paramLatSrc, int paramLongSrc, int paramLatDest, int paramLongDest) {
        double longSrc,latSrc,longDest,latDest, sinLongDist, sinLatDist, a, b;
        
        latSrc = Math.toRadians((double) paramLatSrc / 100000);
        longSrc = Math.toRadians((double)paramLongSrc / 100000);
        latDest = Math.toRadians((double)paramLatDest / 100000);
        longDest = Math.toRadians((double)paramLongDest / 100000);
        
        sinLongDist = Math.sin((longDest - longSrc)/2);
        sinLatDist = Math.sin((latDest - latSrc)/2);
        
        a = (sinLatDist*sinLatDist) + (Math.cos(latSrc)*Math.cos(latDest)*sinLongDist*sinLongDist);
        b = 2 * atan2(Math.sqrt(a) , Math.sqrt(1-a));
        
        return (int) Math.floor(6378137 * b);
    }

}
