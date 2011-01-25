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

import java.util.LinkedHashMap;

import javax.microedition.midlet.MIDlet;

import com.orange.memoplayer.Widget;
import com.orange.memoplayer.WidgetUpdate;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public abstract class Displayable {    
    protected CommandListener commandListener;
    private LinkedHashMap<Command, Integer> commands;
    protected View viewWithSoftKeys;
    protected View view;
    private Command backCommand;
    private Command lsk, rsk;
    private boolean menuChanged = true;

    public Displayable() {
        commands = new LinkedHashMap<Command, Integer> (2, 0.75f);
    }

    public void addCommand (Command command) {
        menuChanged = true;
        if (!commands.containsKey (command)) {
            commands.put (command, -1);
            switch (command.getCommandType ()) {
            case Command.BACK:
            case Command.CANCEL:
                backCommand = command;;
                break;
            }
        }
    }

    public void removeCommand (Command command) {
        menuChanged = true;
        commands.remove (command);
        if (command == backCommand) {
            backCommand = null;
        }
    }

    public Command getBackCommand () {
        return backCommand;
    }

    public CommandListener getCommandListener () {
        return this.commandListener;
    }

    public void setCommandListener (CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public int getWidth () {
        if (view != null) {
            return view.getWidth();
        }
        return 0;
    }

    public int getHeight () {
        if (view != null) {
            return view.getHeight();
        }
        return 0;
    }

    public void show (Display d) {
        ContextWrapper c = d.getMIDlet ().getContext ();
        if (c != null) {
            if (c instanceof Activity) {
                Activity a = (Activity)c;
                menuChanged = true;
                updateSoftKeys (a);
                if (viewWithSoftKeys != null) {
                    a.setContentView (viewWithSoftKeys);
                } else {
                    a.setContentView (view);
                }
                view.requestFocus ();
            } else if (c instanceof WidgetUpdate) {
                
            }
        }
    }
    
    public void hide (Display d) {
        viewWithSoftKeys = null;
        view = null;
        lsk = rsk = null;
        menuChanged = true;
    }
    
    private void updateSoftKeys (Context c) {
        if (!commands.isEmpty ()) {
            lsk = findCommandByType (Command.OK);
            rsk = findCommandByType (Command.CANCEL);
            if (rsk == null) {
                rsk = findCommandByType (Command.BACK);
            }
            if (lsk != null || rsk != null) {
                LinearLayout ll = new LinearLayout (c);
                ll.setOrientation (LinearLayout.VERTICAL);
                view.setLayoutParams (new LayoutParams (
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
                ll.addView (view);
                LinearLayout skb = new LinearLayout (c);
                skb.setOrientation (LinearLayout.HORIZONTAL);
                skb.setGravity (Gravity.CENTER);
                skb.setWeightSum (1.0f);
                Button lb = new Button (c);
                lb.setOnClickListener (new View.OnClickListener() {
                    public void onClick(View v) {
                        commandListener.commandAction (lsk, Displayable.this);
                    }
                });
                lb.setText (lsk.getLabel ());
                LayoutParams lp = new LinearLayout.LayoutParams (
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0.5f);
                lb.setLayoutParams (lp);
                skb.addView (lb);
                Button rb = new Button (c);
                rb.setText (rsk.getLabel ());
                rb.setOnClickListener (new View.OnClickListener() {
                    public void onClick(View v) {
                        commandListener.commandAction (rsk, Displayable.this);
                    }
                });
                rb.setLayoutParams (lp);
                skb.addView (rb);
                ll.addView (skb);
                viewWithSoftKeys = ll;
            }
        }
        menuChanged = true;
    }
    
    private Command findCommandByType (int type) {
        for (Command c : commands.keySet ()) {
            if (c.getCommandType () == type) {
                return c;
            }
        }
        return null;
    }
    
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (menuChanged) {
            menuChanged = false;
            menu.clear ();
            int i = 0;
            for (Command c : commands.keySet ()) {
                if (c != lsk && c != rsk) { // exclude soft keys
                    menu.add (Menu.NONE, i, c.getPriority (), c.getLabel ());
                    commands.put (c, i++);
                }
            }
            return i != 0;
        }
        return false;
    }
    
    public boolean onOptionsItemSelected (MenuItem item) {
        if (commandListener != null) {
            int menuItemId = item.getItemId ();
            for (Command c : commands.keySet ()) {
                if (commands.get (c) == menuItemId) {
                    commandListener.commandAction (c, this);
                    return true;
                }
            }
        }
        return false;
    }
}
