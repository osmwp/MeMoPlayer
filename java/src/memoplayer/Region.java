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
//#ifndef BlackBerry
import javax.microedition.lcdui.Graphics;
//#endif

public class Region {
    int x0, y0, x1, y1;

    Region () {
    }

    Region (int a, int b, int c, int d) {
        setInt (a, b, c, d);
    }

    void setFloat (int a, int b, int c, int d) {
        x0 = a<<16; y0 = b<<16; x1 = c<<16; y1 = d<<16;
    }

    void setInt (int a, int b, int c, int d) {
        if(a<0) a=0;
        if(b<0) b=0;
        x0 = a; y0 = b; x1 = c; y1 = d;
    }

    void set (int a, int b, int c, int d) {
        x0 = a; y0 = b; x1 = c; y1 = d;
    }
    
    void set (Region r) {
        x0 = r.x0; y0 = r.y0; x1 = r.x1; y1 = r.y1;
    }
    
    void set (Region r, int w, int h) {
        x0 = r.x0; y0 = r.y0; x1 = r.x1; y1 = r.y1;
        if (y0<0) y0 = r.y0 = 0;
        if (y1>h) y1 = r.y1 = h;
    }
    
    Region get(){
        return this;
    }

    int getWidth () { 
        //System.out.println("REGION x1:"+x1+" x0:"+x0+" (x1 - x0):"+(x1 - x0));
        return (x1 - x0); 
    }
    
    int getHeight () { 
        //System.out.println("REGION y1:"+y1+" y0:"+y0+" (y1 - y0):"+(y1 - y0));
        return (y1 - y0);
    }
    
    boolean equals (Region r) {
        return equals2 (r.x0, r.y0, r.x1, r.y1); 
    }

    boolean equals2 (int a, int b, int c, int d) {
        return (x0 == a) && (y0 == b) && (x1 == c) && (y1 == d); 
    }

    //MCP: Unsuded code...
    //boolean intersects (Region r) {
    //    return (r.x0 < x1 && r.x1 > x0) || (r.y0 < y1 && r.y1 > y0);
    //}

    boolean applyIntersection (Region A, int Bx0, int By0, int Bx1, int By1) {
        x0 = A.x0 > Bx0 ? A.x0 : Bx0;
        x1 = A.x1 < Bx1 ? A.x1 : Bx1;
        y0 = A.y0 > By0 ? A.y0 : By0;
        y1 = A.y1 < By1 ? A.y1 : By1;
        return (x0 < x1) && (y0 < y1);
    }
    
    boolean applyIntersection(Region A, Region B) {
        return applyIntersection(A, B.x0, B.y0, B.x1, B.y1);
    }
    
    boolean intersects (int Bx0, int By0, int Bx1, int By1) {
        if (x0 > Bx0) Bx0 = x0;
        if (x1 < Bx1) Bx1 = x1;
        if (y0 > By0) By0 = y0;
        if (y1 < By1) By1 = y1;
        return (Bx0 < Bx1) && (By0 < By1);
    }
    
    boolean intersects (Region B) {
        return intersects(B.x0, B.y0, B.x1, B.y1);
    }
    
    /**
     * Indique si le noeud est inclus dans une region
     * Utiliser pour liberer un noeud si il est entierement
     * au dessous de la zone video
     * @param r Region
     * @return boolean
     */
    boolean include (Region r) {//fait partie de la zone video
        return (x0>r.x0 && x1<r.x1)&&(y0>r.y0 && y1<r.y1);
    }

    void toInt () {
        x0 = FixFloat.fix2int (x0+0x7FFF); y0 = FixFloat.fix2int (y0+0x7FFF);
        x1 = FixFloat.fix2int (x1+0x7FFF); y1 = FixFloat.fix2int (y1+0x7FFF);
    }

    void toFloat () {
        x0 = x0 << 16; y0 = y0 << 16;
        x1 = x1 << 16; y1 = y1 << 16;
    }
    
    public void setClipRegion (Graphics g) {
        g.setClip (x0, y0, x1-x0+1, y1-y0+1);
    }
    
    public String toString () {
        return ("["+x0+" "+y0+", "+x1+" "+y1+" W:"+(x1-x0)+" H:"+(y1-y0)+"]");
    }

    public void add (Region r) {
        x0 = (r.x0 < x0 ? r.x0 : x0); 
        y0 = (r.y0 < y0 ? r.y0 : y0); 
        x1 = (r.x1 > x1 ? r.x1 : x1); 
        y1 = (r.y1 > y1 ? r.y1 : y1); 
    }

    public void addInt (int nx0, int ny0, int nx1, int ny1) {
        x0 = (nx0 < x0 ? nx0 : x0);
        y0 = (ny0 < y0 ? ny0 : y0);
        x1 = (nx1 > x1 ? nx1 : x1);
        y1 = (ny1 > y1 ? ny1 : y1);
    }
    
    // RC 13/10/07: return the rotation computed and Normalize the regions (i.e. non negative surface)
    int getRotationAndNormalize () {
        boolean dx = x1 < x0;
        boolean dy = y1 < y0;
        int rotation = 0;
        if (dx) { // ho ho a rotation happened!
            rotation = 270;
            int a = x0; x0 = x1; x1 = a;
        }
        if (dy) { // ho ho a rotation happened!
            rotation = dx ? 180 : 90;
            int a = y0; y0 = y1; y1 = a;
        }
        return rotation;
    }
    public void purge () {}
}
