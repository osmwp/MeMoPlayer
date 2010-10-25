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

public class Matrix {
    final static int MAX_STACK = 128; 
    final static int FIXONE = 0x10000;
    final static int IDENTITY_ARRAY [] = {
        FIXONE, 0, 0, 
        0, FIXONE, 0, 
        0, 0, FIXONE 
    };

//    final static Matrix s_a = new Matrix ();
//    final static Matrix s_b = new Matrix ();
//     final static int [] SIN_DATA = { 0, 1 };
//     final static int [] COS_DATA = { 0, 1 };

    int m_ptr;
    boolean m_isStack;
    int [] m_buffer;

    Matrix () {
        this (false);
    }
    
    Matrix (boolean isStack) {
        m_isStack = isStack;
        m_ptr = 0;
        m_buffer = new int [m_isStack ? 9*MAX_STACK : 9];
        loadIdentity ();
    }
    
    void loadIdentity () {
        System.arraycopy (IDENTITY_ARRAY, 0, m_buffer, m_ptr, 9);
    }

    void translate (int x, int y) {
        m_buffer [m_ptr+6] += FixFloat.fixMul (m_buffer [m_ptr+0], x) + FixFloat.fixMul (m_buffer [m_ptr+3], y);
        m_buffer [m_ptr+7] += FixFloat.fixMul (m_buffer [m_ptr+1], x) + FixFloat.fixMul (m_buffer [m_ptr+4], y);
//         s_b.loadIdentity ();
//         s_b.m_buffer[6] = x;
//         s_b.m_buffer[7] = y;
//         s_a.setValueFrom (this);
//         multiply (s_a, s_b);
    }

/** added by RCA 13/10/07
     * add a rotate component to the current matrix. This is done by pre computing a rotation matrix (R) and pre multiply it with the current matrix (C)
     * the code above is the result of C = R*C with some optimisations
     * param angle the rotation angle, in radians and fixfloat. Exemple of conversion: ((deg*3.1418)/180) << 16
     */
    void rotate (int angle) {
        int c = FixFloat.fixCos (angle);
        int s = FixFloat.fixSin (angle);
        int a = m_buffer[m_ptr+0];
        int b = m_buffer[m_ptr+3];
        int d = m_buffer[m_ptr+1];
        int e = m_buffer[m_ptr+4];
        m_buffer[m_ptr+0] = FixFloat.fixMul (a, c) + FixFloat.fixMul (b, s) ;
        m_buffer[m_ptr+1] = FixFloat.fixMul (d, c) + FixFloat.fixMul (e, s) ;
        m_buffer[m_ptr+3] = FixFloat.fixMul (a, -s) + FixFloat.fixMul (b, c) ;
        m_buffer[m_ptr+4] = FixFloat.fixMul (d, -s) + FixFloat.fixMul (e, c) ;

//         s_b.loadIdentity ();
//         s_b.m_buffer[0] = c;
//         s_b.m_buffer[4] = c;
//         s_b.m_buffer[1] = s;
//         s_b.m_buffer[3] = -s;
//         s_a.setValueFrom (this);
//         multiply (s_a, s_b);
    }

    void scale (int sx, int sy) { // RC 13/10/07 fix computation for non diagonal elements
         m_buffer[m_ptr+0] = FixFloat.fixMul (m_buffer [m_ptr+0], sx);
         m_buffer[m_ptr+3] = FixFloat.fixMul (m_buffer [m_ptr+3], sy);
         m_buffer[m_ptr+1] = FixFloat.fixMul (m_buffer [m_ptr+1], sx);
         m_buffer[m_ptr+4] = FixFloat.fixMul (m_buffer [m_ptr+4], sy);

//         s_b.loadIdentity ();
//         s_b.m_buffer[0] = sx;
//         s_b.m_buffer[4] = sy;
//         s_a.setValueFrom (this);
//         multiply (s_a, s_b);
    }
/*
    void scale (int sx, int sy) {
        m_buffer[m_ptr+0] = FixFloat.fixMul (m_buffer [m_ptr+0], sx);
        m_buffer[m_ptr+1] = FixFloat.fixMul (m_buffer [m_ptr+1], sx);
        m_buffer[m_ptr+2] = FixFloat.fixMul (m_buffer [m_ptr+2], sx);
        m_buffer[m_ptr+3] = FixFloat.fixMul (m_buffer [m_ptr+3], sy);
        m_buffer[m_ptr+4] = FixFloat.fixMul (m_buffer [m_ptr+4], sy);
        m_buffer[m_ptr+5] = FixFloat.fixMul (m_buffer [m_ptr+5], sy);
    }*/

    void revTransform (Point p) {
        int det = FixFloat.fixMul (m_buffer[m_ptr+1], m_buffer[m_ptr+3]) - FixFloat.fixMul (m_buffer[m_ptr  ], m_buffer[m_ptr+4]);
        p.x -= m_buffer[m_ptr+6];
        p.y -= m_buffer[m_ptr+7];
        int x = FixFloat.fixMul (m_buffer[m_ptr  ], p.x) - FixFloat.fixMul (m_buffer[m_ptr+1], p.y);
        int y = FixFloat.fixMul (m_buffer[m_ptr+4], p.y) - FixFloat.fixMul (m_buffer[m_ptr+3], p.x);
        p.x = FixFloat.fixDiv (x, det);
        p.y = FixFloat.fixDiv (y, det);
    }

    void transform (Point p) {
        int x = FixFloat.fixMul (m_buffer[m_ptr  ], p.x) + FixFloat.fixMul (m_buffer[m_ptr+3], p.y) + m_buffer[m_ptr+6];
        int y = FixFloat.fixMul (m_buffer[m_ptr+1], p.x) + FixFloat.fixMul (m_buffer[m_ptr+4], p.y) + m_buffer[m_ptr+7];
        p.x = x;
        p.y = y;
    }
    
    void transform (Region r) {
        int x = FixFloat.fixMul (m_buffer[m_ptr ], r.x0) + FixFloat.fixMul (m_buffer[m_ptr+3], r.y0) + m_buffer[m_ptr+6];
        //System.out.printn("transform x:"+x);
        int y = FixFloat.fixMul (m_buffer[m_ptr+1], r.x0) + FixFloat.fixMul (m_buffer[m_ptr+4], r.y0) + m_buffer[m_ptr+7];
        //System.out.println("transform y:"+y+"  m_buffer[m_ptr+7]:"+m_buffer[m_ptr+7]);
        r.x0 = x;
        r.y0 = y;
        x = FixFloat.fixMul (m_buffer[m_ptr ], r.x1) + FixFloat.fixMul (m_buffer[m_ptr+3], r.y1) + m_buffer[m_ptr+6];
        //System.out.println("transform x1:"+x);
        y = FixFloat.fixMul (m_buffer[m_ptr+1], r.x1) + FixFloat.fixMul (m_buffer[m_ptr+4], r.y1) + m_buffer[m_ptr+7];
        // System.out.println("transform y1:"+y+"  m_buffer[m_ptr+7]:"+m_buffer[m_ptr+7]);
        r.x1 = x;
        r.y1 = y;
    }
    
//     int transformX (int fx, int fy) {
//         return FixFloat.fixMul (m_buffer[m_ptr+0], fx) + FixFloat.fixMul (m_buffer[m_ptr+3], fy) + m_buffer[m_ptr+6];
//     }

//     int transformY (int fx, int fy) {
//         return FixFloat.fixMul (m_buffer[m_ptr+1], fx) + FixFloat.fixMul (m_buffer[m_ptr+4], fy) + m_buffer[m_ptr+7];
//     }

    void pop () {
        if (m_isStack && m_ptr > 8) {
            m_ptr -= 9;
        }
    }

    void push () {
        if (m_isStack && m_ptr < (MAX_STACK-1)*9) {
            m_ptr += 9;
            System.arraycopy (m_buffer, m_ptr-9, m_buffer, m_ptr, 9);
        }
    }

//     int getScaleX () {
//         return (FIXONE);
//     }
//     int getScaleY () {
//         return (FIXONE);
//     }

    void setValueFrom (Matrix  m) {
        System.arraycopy (m.m_buffer, m.m_ptr, m_buffer, m_ptr, 9);
    }

    void copyFrom (Matrix  m) {
        if (m_isStack && m.m_isStack) {
            System.arraycopy (m.m_buffer, 0, m_buffer, 0, 9*MAX_STACK);
            m_ptr = m.m_ptr;
        } else if (!m_isStack && !m.m_isStack) {
            System.arraycopy (m.m_buffer, 0, m_buffer, 0, 9);
        }
    }
    
//     protected void setRotation (int angle) {
//     }
    
    protected void multiply (Matrix a, Matrix b) {
        m_buffer[m_ptr+0] = FixFloat.fixMul (a.m_buffer[0], b.m_buffer[0]) + FixFloat.fixMul (a.m_buffer[3], b.m_buffer[1]) + FixFloat.fixMul (a.m_buffer[6], b.m_buffer[2]);
        m_buffer[m_ptr+1] = FixFloat.fixMul (a.m_buffer[1], b.m_buffer[0]) + FixFloat.fixMul (a.m_buffer[4], b.m_buffer[1]) + FixFloat.fixMul (a.m_buffer[7], b.m_buffer[2]);
        m_buffer[m_ptr+2] = FixFloat.fixMul (a.m_buffer[2], b.m_buffer[0]) + FixFloat.fixMul (a.m_buffer[5], b.m_buffer[1]) + FixFloat.fixMul (a.m_buffer[8], b.m_buffer[2]);

        m_buffer[m_ptr+3] = FixFloat.fixMul (a.m_buffer[0], b.m_buffer[3]) + FixFloat.fixMul (a.m_buffer[3], b.m_buffer[4]) + FixFloat.fixMul (a.m_buffer[6], b.m_buffer[5]);
        m_buffer[m_ptr+4] = FixFloat.fixMul (a.m_buffer[1], b.m_buffer[3]) + FixFloat.fixMul (a.m_buffer[4], b.m_buffer[4]) + FixFloat.fixMul (a.m_buffer[7], b.m_buffer[5]);
        m_buffer[m_ptr+5] = FixFloat.fixMul (a.m_buffer[2], b.m_buffer[3]) + FixFloat.fixMul (a.m_buffer[5], b.m_buffer[4]) + FixFloat.fixMul (a.m_buffer[8], b.m_buffer[5]);

        m_buffer[m_ptr+6] = FixFloat.fixMul (a.m_buffer[0], b.m_buffer[6]) + FixFloat.fixMul (a.m_buffer[3], b.m_buffer[7]) + FixFloat.fixMul (a.m_buffer[6], b.m_buffer[8]);
        m_buffer[m_ptr+7] = FixFloat.fixMul (a.m_buffer[1], b.m_buffer[6]) + FixFloat.fixMul (a.m_buffer[4], b.m_buffer[7]) + FixFloat.fixMul (a.m_buffer[7], b.m_buffer[8]);
        m_buffer[m_ptr+8] = FixFloat.fixMul (a.m_buffer[2], b.m_buffer[6]) + FixFloat.fixMul (a.m_buffer[5], b.m_buffer[7]) + FixFloat.fixMul (a.m_buffer[8], b.m_buffer[8]);

    }
    
    //protected void set (int i, int val) { m_buffer[m_ptr+i] = val; }
    
    protected int get (int i) { return m_buffer[m_ptr+i]; }
    
    //protected void set (int y, int x, int val) {    m_buffer[m_ptr+y*3+x] = val; }
    
    //protected int get (int y, int x) { return m_buffer [m_ptr+y*3+x]; }
    
    void print (String msg) {
        System.out.println ("matrix: "+msg+"/"+FixFloat.fix2float(m_buffer[0]));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print (""+FixFloat.fix2float (get (i+j*3))+" ");
            }
            System.out.println ("");
        }
    }

}
