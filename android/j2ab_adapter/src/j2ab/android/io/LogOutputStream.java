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

package j2ab.android.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

public class LogOutputStream extends OutputStream {
    public static final int LOG_LEVEL = Log.ERROR;
    private ByteArrayOutputStream bos = new ByteArrayOutputStream ();
    private String name;

    public LogOutputStream(String name) {
        this.name = name;
    }

    @Override
    public void write (int b) throws IOException {
        if (b == (int) '\n') {
            String s = new String (this.bos.toByteArray ());
            Log.v (this.name, s);
            this.bos = new ByteArrayOutputStream ();
        } else {
            this.bos.write (b);
        }
    }

}
