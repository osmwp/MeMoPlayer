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

class FixFloat {

    final static int int2fix (int i) { 
        return i << 16;
    }
    
    final static int time2fix (int i) { 
        long z = ((long)i)<<16;
        return (int)(z /1000);
    }

    final static int fix2time (int f) { 
        long z = ((long)f)*1000;
        return (int)(z >> 16); 
    }

    // convert from microseconds to second in fix format
    final static int micro2sec (long i) { 
        long z = (((long) i) << 16);
        return (int) ((z / 1000) >> 16);
    }
    
    final static int fix2int (int f) { 
        return f >> 16; 
    }
    final static short fix2short (int f) { 
        return (short)(f >> 16); 
    }
    
    final static int fix2frac (int f) { 
        return (f & 0xFFFF); 
    }

    final static float fix2float (int f) { 
        return (((float)f) / 65536.0f); 
    }

    final static int float2fix (float f) { 
        return (int)(f * 65536); 
    }
    
//     final static int time2fixB (int t) {
//         long z = (((long) t) << 48);
//         return (int) ((z / (1000<<16)) >> 16);
//     }

    final static String toString (int f) {
        double fl = f/65536.0;
        return (""+fl);
//         int integer = fix2int (f);
//         long accu = 0;
//         long coef = 1;
//         long  incr = 5;
//         for (int i = 0; i < 16; i++) {
//             coef *= 10;
//             if ( (f & 0x8000) > 0) {
//                 accu = accu * coef + incr;
//                 coef = 1;
//             }
//             f <<= 1;
//             incr = (incr * 10)/2;
//         }
//         while (accu > 100000) {
//             accu = (accu +5) / 10;
//         }
//         while ( accu > 0 && (accu % 10) == 0) {
//             accu /= 10;
//         }
//         return Integer.toString (integer)+"."+accu;
    }

    // Multiplies two 16:16 fixed-point numbers
//     final static int Mul (int x, int y) { 
//         long z = (long) x * (long) y;
//         return ((int) (z >> 16));
//         //return (((x)>>6) * ((y)>>6)) >> 4; 
//     }
    
    // Multiplies two 16:16 fixed-point numbers
    final static int fixMul (int x, int y) { 
        long z = (long) x * (long) y;
        
        //System.out.println("fixMul x:"+x+" y:"+y+"  fixMul z:"+z+" fixMul return:"+((int) (z >> 16)));
        return ((int) (z >> 16));
        //return (((x)>>6) * ((y)>>6)) >> 4; 
    }

    // Divides two 16:16 fixed-point numbers
//     public static int Div (int x, int y) {
//         long z = (((long) x) << 32);
//         return (int) ((z / y) >> 16);
//     }

    // Divides two 16:16 fixed-point numbers
    public static int fixDiv (int x, int y) {
        long z = (((long) x) << 32);
        return (int) ((z / y) >> 16);
    }

    final static int fixOne = 0x10000;

    // Compute square-root of a 16:16 fixed point number */
    final static int sqrt (int n) {
        int s = (n + 65536) >> 1;
        for (int i = 0; i < 8; i++) {
            //converge six times
            s = (s + fixDiv(n, s)) >> 1;
        }
        return s;
    }


    final static String str (int f) {
        return toString (f);
        //return (""+fix2int(f)+"/"+fix2frac(f));
    }


    public static final int PI = 205887;
    public static final int PI_TIMES_2 = 411774;
    public static final int PI_OVER_2 = PI/2;
    public static final int E = 178145;
    public static final int HALF = 2<<15;

    public static int rad2deg(int r) {
        return fixDiv(fixMul(r, 180<<16), PI);
    }
    
    /**
     * For the inverse tangent calls, all approximations are valid for |t| <= 1.
     * To compute ATAN(t) for t > 1, use ATAN(t) = PI/2 - ATAN(1/t).  For t < -1,
     * use ATAN(t) = -PI/2 - ATAN(1/t).
     */

    static final int SK1 = 498;
    static final int SK2 = 10882;

    /** Computes SIN(f), f is a fixed point number in radians.
     * 0 <= f <= 2PI
     */
    public static int fixSin (int f) {
        while (f > PI_TIMES_2) {
            f -= PI_TIMES_2;
        }
        while (f < 0) {
            f += PI_TIMES_2;
        }
        // If in range -pi/4 to pi/4: nothing needs to be done.
        // otherwise, we need to get f into that range and account for
        // sign change.
        
        int sign = 1;
        if ((f > PI_OVER_2) && (f <= PI)) {
            f = PI - f;
        } else if ((f > PI) && (f <= (PI + PI_OVER_2))) {
            f = f - PI;
            sign = -1;
        } else if (f > (PI + PI_OVER_2)) {
            f = (PI<<1)-f;
            sign = -1;
        }
        
        int sqr = fixMul(f,f);
        int result = SK1;
        result = fixMul(result, sqr);
        result -= SK2;
        result = fixMul(result, sqr);
        result += (1<<16);
        result = fixMul(result, f);
        return sign * result;
    }
    
    static final int CK1 = 2328;
    static final int CK2 = 32551;
    
    /** Computes COS(f), f is a fixed point number in radians.
     * 0 <= f <= PI/2
     */
    public static int fixCos (int f) {
        while (f > PI_TIMES_2) {
            f -= PI_TIMES_2;
        }
        while (f < 0) {
            f += PI_TIMES_2;
        }
        int sign = 1;
        if ((f > PI_OVER_2) && (f <= PI)) {
            f = PI - f;
            sign = -1;
        } else if ((f > PI_OVER_2) && (f <= (PI + PI_OVER_2))) {
            f = f - PI;
            sign = -1;
        } else if (f > (PI + PI_OVER_2)) {
            f = (PI<<1)-f;
        }
        
        int sqr = fixMul(f,f);
        int result = CK1;
        result = fixMul(result, sqr);
        result -= CK2;
        result = fixMul(result, sqr);
        result += (1<<16);
        return result * sign;
    }

    public static int arcTan (int f) {
        int sqr = fixMul(f,f);
        int result = 1365;
        result = fixMul(result, sqr);
        result -= 5579;
        result = fixMul(result, sqr);
        result += 11805;
        result = fixMul(result, sqr);
        result -= 21646;
        result = fixMul(result, sqr);
        result += 65527;
        result = fixMul(result,f);
        return result;
    }

    static final int AS1 = -1228;
    static final int AS2 = 4866;
    static final int AS3 = 13901;
    static final int AS4 = 102939;

    /** Compute ArcSin(f), 0 <= f <= 1
     */

    public static int arcSin (int f) {
        int fRoot = sqrt((1<<16)-f);
        int result = AS1;
        result = fixMul(result, f);
        result += AS2;
        result = fixMul(result, f);
        result -= AS3;
        result = fixMul(result, f);
        result += AS4;
        result = PI_OVER_2 - (fixMul(fRoot,result));
        return result;
    }


    /** Compute ArcCos(f), 0 <= f <= 1
     */

    public static int arcCos (int f) {
        int fRoot = sqrt((1<<16)-f);
        int result = AS1;
        result = fixMul(result, f);
        result += AS2;
        result = fixMul(result, f);
        result -= AS3;
        result = fixMul(result, f);
        result += AS4;
        result = fixMul(fRoot,result);
        return result;
    }

//#ifdef api.array
    //MCP: Non static part so a FixFloat can be represented by its own
    //     type for serialization during persistence and API Arrays 
    private int m_value;
    
    public FixFloat(int v) { m_value = v; }
    public FixFloat(float v) { m_value = float2fix(v); }
    public int get() { return m_value; }
    public String toString() { return toString(m_value); }
    
    //MCP: Treat FixFloat like Strings: comparision of the value for Hashtable
    public boolean equals(Object o) {
        if (o instanceof FixFloat) {
            FixFloat f = (FixFloat)o;
            return m_value == f.m_value; 
        }
        return super.equals(o);
    }
    public int hashCode() {
        return m_value;
    }
//#endif
}
