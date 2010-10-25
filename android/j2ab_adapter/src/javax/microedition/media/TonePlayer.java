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

package javax.microedition.media;

import javax.microedition.media.control.ToneControl;

public class TonePlayer implements Player, ToneControl, Controllable {

    // @Override
    public Control getControl (String controlType) {
        if (controlType.equals ("ToneControl")) {
            return this;
        } else {
            return null;
        }
    }

    // @Override
    public Control[] getControls () {
        // TODO Auto-generated method stub
        return new Control[] { this };
    }

    // @Override
    public void addPlayerListener (PlayerListener playerListener) {
    // TODO Auto-generated method stub

    }

    // @Override
    public void close () {
    // TODO Auto-generated method stub

    }

    // @Override
    public void deallocate () {
    // TODO Auto-generated method stub

    }

    // @Override
    public String getContentType () {
        // TODO Auto-generated method stub
        return null;
    }

    // @Override
    public long getDuration () {
        // TODO Auto-generated method stub
        return 0;
    }

    // @Override
    public long getMediaTime () {
        // TODO Auto-generated method stub
        return 0;
    }

    // @Override
    public int getState () {
        // TODO Auto-generated method stub
        return 0;
    }

    // @Override
    public void prefetch () {
    // TODO Auto-generated method stub

    }

    // @Override
    public void realize () {
    // TODO Auto-generated method stub

    }

    // @Override
    public void removePlayerListener (PlayerListener playerListener) {
    // TODO Auto-generated method stub

    }

    // @Override
    public void setLoopCount (int count) {
    // TODO Auto-generated method stub

    }

    // @Override
    public long setMediaTime (long now) {
        // TODO Auto-generated method stub
        return 0;
    }

    // @Override
    public void start () {
    // TODO Auto-generated method stub

    }

    // @Override
    public void stop () {
    // TODO Auto-generated method stub

    }

    // @Override
    public void setSequence (byte[] sequence) {

    }

}
