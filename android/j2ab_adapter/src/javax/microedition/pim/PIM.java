/*
 * Copyright (C) 2009 The J2AB Project
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

package javax.microedition.pim;

import j2ab.android.pim.AndroidContactList;

public class PIM {
    public static final int READ_ONLY = 0x01;
    public static final int WRITE_ONLY = 0x02;
    public static final int READ_WRITE = 0x03;

    public static final int CONTACT_LIST = 1;

    private static PIM INSTANCE;

    public static final PIM getInstance () {
        if (INSTANCE == null) {
            INSTANCE = new PIM ();
        }
        return INSTANCE;
    }

    protected PIM() {

    }

    public String[] listPIMLists (int type) {
        return new String[] { "default" };
    }

    public PIMList openPIMList (int pimListType, int mode) {
        return openPIMList (pimListType, mode, null);
    }

    public PIMList openPIMList (int pimListType, int mode, String name) {
        return new AndroidContactList ();
    }
}
