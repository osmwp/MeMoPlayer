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

package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.BitmapDrawable;

public class Alert extends Screen {

    public static final int FOREVER = -2;
    public static final long DEFAULT_TIMEOUT = 3000;

    private String title;
    private String message;
    private Image image;
    private AlertType type;
    private long timeout = DEFAULT_TIMEOUT;

    private AlertDialog alert;

    public Alert(String title) {
        this (title, null, null, null);
    }

    public Alert(String title, String message, Image image, AlertType type) {
        this.title = title;
        this.message = message;
        this.image = image;
        this.type = type;
    }

    public void show (Display d) {
        MIDlet midlet = d.getMIDlet ();
        AlertDialog.Builder builder = new AlertDialog.Builder (midlet.getContext ());
        if (title != null) {
            builder.setTitle (title);
        }
        if (message != null) {
            builder.setMessage (message);
        }
        if (image != null) {
            builder.setIcon (new BitmapDrawable (image.getBitmap ()));
        }
        builder.setCancelable (true);
        alert = builder.create ();
        // TODO : timeouts
    }

    public void hide (Display d) {
        alert = null;
    }
    
    public Dialog getDialog () {
        return this.alert;
    }

    public void setTimeout (long timeout) {
        this.timeout = timeout;
    }
}
