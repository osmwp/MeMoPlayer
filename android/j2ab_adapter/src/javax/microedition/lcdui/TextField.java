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

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TwoLineListItem;

public class TextField extends Item implements Runnable {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int CONSTRAINT_MASK = 0xFFFF;
    public static final int PASSWORD = 0x10000;
    public static final int UNEDITABLE= 0x20000;
    public static final int SENSITIVE = 0x40000;
    public static final int NON_PREDICTIVE = 0x80000;
    public static final int INITIAL_CAPS_WORD = 0x100000;
    public static final int INITIAL_CAPS_SENTENCE = 0x200000;

    public static TextView createTextView (int constraints, Context context) {
        TextView textView;
        if ((constraints & TextField.UNEDITABLE) == 0) {
            int type = InputType.TYPE_CLASS_TEXT;
            switch (constraints & CONSTRAINT_MASK) {
            case NUMERIC:
                type = InputType.TYPE_CLASS_NUMBER;
                break;
            case DECIMAL:
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                break;
            case PHONENUMBER:
                type = InputType.TYPE_CLASS_PHONE;
                break;
            case EMAILADDR:
                type |= InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                break;
            case URL:
                type |= InputType.TYPE_TEXT_VARIATION_URI;
                break;
            default:
                type |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                break;
            }
            if ((constraints & TextField.INITIAL_CAPS_SENTENCE ) !=0) {
                type |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            } else if ((constraints & TextField.INITIAL_CAPS_WORD) != 0) {
                type |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
            }
            if ((constraints & TextField.NON_PREDICTIVE) == 0) {
                type |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
            }
            if ((constraints & TextField.PASSWORD) > 0) {
                type |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
            }
            textView = new EditText (context);
            textView.setInputType(type);
        } else {
            textView = new TextView (context);
        }
        return textView;
    }

    private String label;
    private String text;
    private int constraints;
    private int maxSize;

    private TwoLineListItem view;
    private TextView labelView;
    private TextView textView;

    private MIDlet midlet;

    public TextField(String label, String text, int maxSize, int constraints) {
        this.label = label;
        this.text = text;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    @Override
    public void dispose () {
        this.text = this.textView.getText ().toString ();
        this.view = null;
        this.textView = null;
        this.labelView = null;
    }

    @Override
    public View getView () {
        return this.view;
    }

    @Override
    public void init (MIDlet midlet, ViewGroup parent) {
        this.midlet = midlet;
        Context context = midlet.getContext ();
        TwoLineListItem view = new TwoLineListItem (context);
        TextView labelView = new TextView (context);
        TextView textView = createTextView (this.constraints, context);

        labelView.setText (this.label);
        textView.setText (this.text);

        view.addView (labelView);
        view.addView (textView);

        this.view = view;
        this.labelView = labelView;
        this.textView = textView;

    }

    public String getLabel () {
        return this.label;
    }

    public void setLabel (String label) {
        this.label = label;
        if (this.labelView != null) {
            this.midlet.getHandler ().post (this);
        }
    }

    public String getString () {
        String text;
        if (this.textView != null) {
            text = this.textView.getText ().toString ();
        } else {
            text = this.text;
        }
        return text;
    }

    public void setString (String text) {
        this.text = text;
        if (this.textView != null) {
            this.textView.setText (text);
        }
    }

    public int getMaxSize () {
        return this.maxSize;
    }

    public void setMaxSize (int maxSize) {
        this.maxSize = maxSize;
    }

    public int getConstraints () {
        return this.constraints;
    }

    public void setConstraints (int constraints) {
        this.constraints = constraints;
    }

    // @Override
    public void run () {
        this.labelView.setText (label);
    }

}
