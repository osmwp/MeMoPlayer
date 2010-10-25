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

import javax.microedition.lcdui.*;

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
