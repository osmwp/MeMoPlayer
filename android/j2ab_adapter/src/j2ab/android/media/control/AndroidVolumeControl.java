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

package j2ab.android.media.control;

import javax.microedition.media.control.VolumeControl;

import android.media.MediaPlayer;

public class AndroidVolumeControl implements VolumeControl {

    private MediaPlayer mPlayer;
    int mVolume;
    boolean mMuted;
    
    public AndroidVolumeControl (MediaPlayer player) {
        mPlayer = player;
        setLevel (50);
    }
    
    // Only accessed from AndroidPlayer
    public void release () {
        mPlayer = null;
    }
    
    public int getLevel () {
        return mVolume;
    }

    public boolean isMuted () {
        return mMuted;
    }

    public void setLevel (int level) {
        mVolume = level;
        mPlayer.setVolume (level/100f, level/100f);
        mMuted = level == 0;
    }

    public void setMute (boolean mute) {
        if (mute != mMuted) {
            if (mute) {
                mPlayer.setVolume (0, 0);
                mMuted = true;
            } else {
                setLevel (mVolume);
            }
        }
    }
}
