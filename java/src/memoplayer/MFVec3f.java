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

class MFVec3f extends MFFloatBase {

    MFVec3f (Observer o) {
        super (3, o);
    }
    MFVec3f () {
        super (3);
    }

//     void decode (DataInputStream dis) {
//         //System.out.print ("decoding MFVec2f: #");
//         super.decode (dis);
//     }

}
