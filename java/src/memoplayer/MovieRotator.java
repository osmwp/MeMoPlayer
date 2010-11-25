//#condition api.mm
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

public class MovieRotator {
    final static int PORTRAIT = 0;
    final static int LANDSCAPE = 90;
    static boolean setOrientation (javax.microedition.media.Player player, int rotation) {
// not supported on blackberry
//#ifndef BlackBerry
        Logger.println ("setOrientation: "+rotation);
        int mode = 0;
        if (rotation >= 45 && rotation < 135) { // 90
            mode = javax.microedition.lcdui.game.Sprite.TRANS_ROT90<<4;
        } else if (rotation >= 135 && rotation < 225) { //180
            mode = javax.microedition.lcdui.game.Sprite.TRANS_ROT180<<4;
        } else if (rotation >= 225 && rotation < 315) { // 270
            mode = javax.microedition.lcdui.game.Sprite.TRANS_ROT270<<4;
        } else if (rotation >= 315 && rotation < 45) { // 0
            mode = 0;
        }
        try {
            ((com.sonyericsson.media.control.DisplayModeControl) player.getControl("DisplayModeControl")).
                setDisplayMode (mode);
            return (true);
        } catch (Exception e) {
            Logger.println ("MovieRotator.setOrientation: "+e);
        }
//#endif
        return (false);
    }
}
