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

import com.orange.memoplayer.MainActivity;
import com.orange.memoplayer.Widget;
import com.orange.memoplayer.WidgetUpdate;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

public class Display {
    public static final int COLOR_BACKGROUND = 0;
    public static final int COLOR_FOREGROUND = 1;
    public static final int COLOR_BORDER = 2;
    public static final int COLOR_HIGHLIGHTED_BACKGROUND = 3;
    public static final int COLOR_HIGHLIGHTED_FOREGROUND = 4;
    public static final int COLOR_HIGHLIGHTED_BORDER = 5;

    public static final int LIST_ELEMENT = 0;
    public static final int ALERT = 1;
    public static final int CHOICE_GROUP_ELEMENT = 2;

    private static MIDlet sMidlet;
    private static Display sDisplay;

    public static Display getDisplay (MIDlet midlet) {
        if (sMidlet != midlet || sDisplay == null) {
            sDisplay = new Display (midlet);
            sMidlet = midlet;
        }
        return sDisplay;    
    }

    private Displayable current;
    private MIDlet midlet;

    private Display(MIDlet midlet) {
        this.midlet = midlet;
    }

    public Displayable getCurrent () {
        return current;
    }

    public int getColor (int colorSpecifier) {
        // TODO :is there any way to look this up
        int color;
        switch (colorSpecifier) {
        case COLOR_BACKGROUND:
            color = 0x000000;
            break;
        case COLOR_FOREGROUND:
            color = 0xFFFFFF;
            break;
        case COLOR_BORDER:
            color = 0x888888;
            break;
        case COLOR_HIGHLIGHTED_BACKGROUND:
            color = 0xff8600;
            break;
        case COLOR_HIGHLIGHTED_FOREGROUND:
            color = 0x000000;
            break;
        case COLOR_HIGHLIGHTED_BORDER:
            color = 0xAAAAAA;
            break;
        default:
            color = 0xFF0000;
            break;
        }
        return color;
    }

    public int getBestImageWidth (int imageType) {
        return 48;
    }

    public int getBestImageHeight (int imageType) {
        return 48;
    }

    public MIDlet getMIDlet () {
        return this.midlet;
    }

    public void setCurrent (Alert alert, Displayable current) {
        this.setCurrent (current);
        if (alert != null) {
            alert.getDialog ().show ();
        }
    }

    public void setCurrent (final Displayable current) {
        if (current != null) {
            if (current instanceof Alert) {
                setCurrent ((Alert) current, this.current);
            } else if (this.current != current) {
                final Displayable old = this.current;
                this.current = current;
                midlet.invokeAndWait (new Runnable () {
                    public void run () {
                        if (old != null) {
                            old.hide (Display.this);
                        }
                        if (current != null) {
                            current.show (Display.this);
                        }
                    }
                });
            }
        }
    }

    public boolean vibrate (int duration) {
        Vibrator v = (Vibrator) midlet.getContext ().getApplicationContext ().getSystemService (Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (duration == 0) {
                v.cancel ();
            } else {
                v.vibrate (duration);
            }
            return true;
        }
        return false;
    }

    
    public void callSerialy (Runnable r) {
        midlet.post (r);
    }

    public boolean isAppWidget () {
        return midlet.getContext() instanceof WidgetUpdate;
    }

    public boolean displayWidget () {
        if (midlet.getContext() instanceof WidgetUpdate) {
            midlet.post(new Runnable() {
                public void run() {
                    ((WidgetUpdate)midlet.getContext()).displayWidget();
                }
            });
            return true;
        } else if (midlet.getContext() instanceof MainActivity) {
            // From the fullscreen application, send an itent to the widget
            midlet.post(new Runnable() {
                public void run() {
                    Context context = midlet.getContext();
                    Intent intent = new Intent(context, Widget.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    context.sendBroadcast(intent);
                    Log.i("Display", "Launch APPWIDGET_UPDATE intent");
                }
            });
        }
        return false;
    }

    public boolean displayAlert (final String message) {
        if (midlet.getContext() instanceof WidgetUpdate) {
            midlet.post(new Runnable() {
                public void run() {
                    ((WidgetUpdate)midlet.getContext()).displayAlert(message);
                }
            });
            return true;
        }
        return false;
    }

    public boolean displayLoader (final String message) {
        if (midlet.getContext() instanceof WidgetUpdate) {
            midlet.post(new Runnable() {
                public void run() {
                    ((WidgetUpdate)midlet.getContext()).displayLoader(message);
                }
            });
            return true;
        }
        return false;
    }
}
