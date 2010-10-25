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

import android.app.Activity;
import android.content.Context;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class TextBox extends Screen implements Runnable {
    // TODO : present title and maxSize sensibly
    private String title;
    private String text;
    private int maxSize;
    private int constraints;
    private MIDlet midlet;

    private TextView textView;

    public TextBox(String title, String text, int maxSize, int constraints) {
        this.title = title;
        this.text = text;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }
    
    public void show (Display d) {
        midlet = d.getMIDlet ();
        view = textView = TextField.createTextView (constraints, midlet.getContext ());
        textView.setText (text);
        super.show (d);
        if (textView instanceof EditText) {
            // Show soft keyboard
            InputMethodManager imm = (InputMethodManager)view.getContext ().getSystemService (Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput (view, InputMethodManager.SHOW_FORCED);
        }
    }
    
    public void hide (Display d) {
        // Hide soft keyboard
        if (textView != null) {
            InputMethodManager imm = (InputMethodManager)textView.getContext ().getSystemService (Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow (textView.getWindowToken(), 0);
        }
        textView = null;
        midlet = null;
        super.hide (d);
    }

    public int getMaxSize () {
        return maxSize;
    }

    public void setMaxSize (int maxSize) {
        this.maxSize = maxSize;
    }

    public String getString () {
        String result;
        if (textView != null) {
            result = textView.getText ().toString ();
        } else {
            result = text;
        }
        return result;
    }

    public void setString (String text) {
        this.text = text;
        if (textView != null) {
            midlet.getHandler ().post (this);
        }
    }

    public int getConstraints () {
        return constraints;
    }

    public void setConstraints (int constraints) {
        this.constraints = constraints;
        // TODO : adjust the view if it exists
    }

    public void run () {
        if (textView != null) {
            textView.setText (text);
        }
    }
}
