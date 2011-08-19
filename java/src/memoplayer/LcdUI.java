//#condition api.lcdui
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
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;

/**
 * Manages the display of LCDUI Alerts and Textboxes.
 */
public class LcdUI implements CommandListener {
    private static StringBuffer m_textInput;
    
    public static String getTextInput() {
        return m_textInput!=null ? m_textInput.toString() : "";
    }

    private static boolean m_commandOK = false;
    
    /**
     * Displays a TextBox to allow user to input text using the native mobile interface.
     * This call blocks the MyCanvas rendering thread until the user finishes its input.
     *
     * @param title title of the TextBox
     * @param text default text to display
     * @param size max size of the input text
     * @param type See javax.microedition.lcdui.TextField for values
     * @param okLabel
     * @param cancelLabel 
     * @return  true if OK has been selected, else false.
     */
    public static boolean displayTextBox(String title, String text, int size, int type,
            String okLabel, String cancelLabel) {
        
        // initialize or reset text input result
        if (m_textInput == null) {
            m_textInput = new StringBuffer();
        } else {
            m_textInput.setLength(0);
        }

        // truncate initial text if its length is greater than size to avoid an illegal argument exception
        if (text.length()>size) {
        	text = text.substring(0, size);
        }
        
        TextBox textBox = new TextBox(title, text, size, type);
        Command command = new Command(okLabel, Command.OK, 2);
        textBox.addCommand(command);
        command = new Command(cancelLabel, Command.CANCEL, 2);
        textBox.addCommand(command);
        textBox.setCommandListener(new LcdUI());

        Displayable d = MiniPlayer.s_display.getCurrent();
        MiniPlayer.s_display.setCurrent(textBox);
        
        // pause thread
        synchronized (MyCanvas.s_paintLock) {
            try { MyCanvas.s_paintLock.wait(); } catch (InterruptedException ie) { }
        }
        
        // Redisplay previous screen
        if (d != null) {
            MiniPlayer.s_display.setCurrent(d);
            if (d instanceof MyCanvas) {
                // prevents MyCanvas thread of pausing when MM.pause is used
                ((MyCanvas)d).isHidden = false;
            }
        }
            
        return m_commandOK;
    }

    /**
     * Displays an LCDUI alert.
     * @param title
     * @param message
     * @param image
     * @param type See AlertType for values
     * @param timeout Set to Alert.FOREVER to for user to dismiss the alert
     * @see javax.microedition.lcdui.AlertType
     * @see javax.microedition.lcdui.Alert
     */
    public static void displayAlert(String title, String message, Image image, AlertType type, int timeout) {
        Alert a = new Alert(title, message, image, type);
        a.setTimeout(timeout);
        Displayable d = MiniPlayer.s_display.getCurrent();
        if (d != null) {
            MiniPlayer.s_display.setCurrent(a, d);
        } else {
            MiniPlayer.s_display.setCurrent(a);
        }
    }

    /**
     * Handles the callback on the textbox. It waikes up
     * the thread blocked at the displayTextBox() methods.
     */
    public void commandAction(Command c, Displayable s) {
        if (s instanceof TextBox) {
            TextBox textBox = (TextBox)s;
            switch(c.getCommandType()) {
            case Command.OK:
                // get result from textbox
                String result = textBox.getString();
                m_textInput.append(result);
                m_commandOK = true;
                break;
            case Command.CANCEL:
                m_textInput.setLength(0);
                m_commandOK = false;
                break;
            }

            // waikeup thread
            synchronized (MyCanvas.s_paintLock) {
                MyCanvas.s_paintLock.notify();
            }
        }
    }
}
//#else
// BlackBerry section
import net.rim.device.api.ui.UiApplication;
//#ifdef BlackBerry.Touch
import net.rim.device.api.ui.VirtualKeyboard;
//#endif
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;

import net.rim.device.api.ui.MenuItem;

import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.AlertType;

public class LcdUI  {
    private static StringBuffer m_textInput=null;
    private static boolean textboxReturn=false;
    private static Object waitTextBox=null;
    private static TextBoxScreen textboxScreen;

    public static String getTextInput() {
        return m_textInput!=null ? m_textInput.toString() : "";
    }

    // class to launch asyncronously the TexBboxScreen
    private static class textBoxRun implements Runnable {
        String title;
        String text;
        String okLabel;
        String cancelLabel;
        int size;
        int type;
        textBoxRun(String _title, String _text, int _size, int _type,
            String _okLabel, String _cancelLabel) {
            title=_title;
            text=_text;
            okLabel=_okLabel;
            cancelLabel=_cancelLabel;
            size=_size;
            type=_type;
        }
        public void run()
        {
            displayTextBoxScreen(title, text, size, type, okLabel, cancelLabel);
        }
    }

    // display the TextBoxScreen on the screen
    public static boolean displayTextBox(String title, String text, int size, int type,
            String okLabel, String cancelLabel) {

        // initialize or reset text input result
        if (m_textInput == null) {
            m_textInput = new StringBuffer();
            waitTextBox=new Object();
        } else {
            m_textInput.setLength(0);
        }

        textboxReturn=false;
        // make asynchronous call in order to avoid Exception
        UiApplication.getUiApplication().invokeLater (new textBoxRun(title, text, size, type, okLabel, cancelLabel));

        // pause thread, wait TextBoxScreen to close
        synchronized (waitTextBox) {
            try { waitTextBox.wait(); } catch (InterruptedException ie) { }
        }
        
        // free resource
        textboxScreen=null;

//#ifdef BlackBerry.Touch
        // check if virtual keyboard is supported
        VirtualKeyboard vk = ((MainScreen)UiApplication.getUiApplication().getActiveScreen()).getVirtualKeyboard();
        if(vk!=null) {
            // hide it
            vk.setVisibility(VirtualKeyboard.HIDE);
        }
//#endif
        return textboxReturn;
    }

    // prepare, build and display the TextBoxScreen
    public static void displayTextBoxScreen(String title, String text, int size, int type,
            String okLabel, String cancelLabel) {
        
        long style=0;
        
        // check style
        switch(type&0xFFFF){
        case TextField.EMAILADDR:   style=BasicEditField.FILTER_EMAIL;        break;
        case TextField.NUMERIC:     style=BasicEditField.FILTER_INTEGER;      break;
        case TextField.PHONENUMBER: style=BasicEditField.FILTER_PHONE;        break;
        case TextField.URL:         style=BasicEditField.FILTER_URL;          break;
        case TextField.DECIMAL:     style=BasicEditField.FILTER_REAL_NUMERIC; break;
        case TextField.ANY:
        default:                    style=BasicEditField.FILTER_DEFAULT;      break;
        }
        
        BasicEditField textField=null;
        // check password flag
        if( (type&0x10000) != 0 ) {
            textField = new  PasswordEditField("",text,size,style);
        } else {
            textField = new  BasicEditField("",text,size,style);
        }

        textboxScreen = new TextBoxScreen(textField,title,okLabel,cancelLabel);

        // add the screen on the display stack
        UiApplication.getUiApplication().pushScreen( textboxScreen );
    }

    // TextBoxScreen screen
    private static final class TextBoxScreen extends MainScreen {

        BasicEditField textField;
        MenuItem menuItemOk;
        MenuItem menuItemCancel;
        
        public boolean retOK=false; 
        
        TextBoxScreen (BasicEditField _textField, String title, String labelOk, String labelCancel) {

            super();
            
            setTitle( new LabelField( title, LabelField.USE_ALL_WIDTH | LabelField.ELLIPSIS ) );

            textField = _textField;
            
            // add menu OK
            menuItemOk = new MenuItem(labelOk , 100000, 10){
                public void run() 
                {
                    m_textInput.append(textField.getText());
                    textboxReturn = true;
                    // Redisplay previous screen
                    UiApplication.getUiApplication().popScreen( textboxScreen );

                    notifyTextBox();
                }
            };

            // add menu Cancel
            menuItemCancel = new MenuItem(labelCancel , 100000, 10){
                public void run() 
                {
                    m_textInput.setLength(0);
                    textboxReturn = false;
                    // Redisplay previous screen
                    UiApplication.getUiApplication().popScreen( textboxScreen );
                    notifyTextBox();
                }
            };
            
            // add text field
            add( textField );
            
        }
        
        // cancel wait text box
        protected void notifyTextBox() {
            synchronized (waitTextBox) {
                waitTextBox.notify();
            }
        }

        // display the menu
        protected void makeMenu(Menu menu, int instance) {
            menu.add( menuItemOk );
            menu.add( menuItemCancel );
        }

        // close the window
        public boolean onClose() {
            UiApplication.getUiApplication().popScreen( textboxScreen );
            notifyTextBox();
            return false;
        }

        // called when 'back' button is pressed and changes are made
        protected boolean onSavePrompt(){
            // Redisplay previous screen
            UiApplication.getUiApplication().popScreen( textboxScreen );
            textboxReturn = false;
            notifyTextBox();
            return false;
        }
    }

    // display alert message
    public static void displayAlert(String title, String message, Image image, AlertType type, int timeout) {
        Dialog.alert(message);
    }
}
// End of BlackBerry section
//#endif
