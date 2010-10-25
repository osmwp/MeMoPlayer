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

public interface Player extends Controllable {
    static int CLOSED = 0;
    static int PREFETCHED = 300;
    static int REALIZED = 200;
    static int STARTED = 400;
    static long TIME_UNKNOWN = -1;
    static int UNREALIZED = 100;

    void addPlayerListener (PlayerListener playerListener);

    void close ();

    void deallocate ();

    String getContentType ();

    long getDuration ();

    long getMediaTime ();

    int getState ();

    void prefetch () throws MediaException;

    void realize () throws MediaException;

    void removePlayerListener (PlayerListener playerListener);

    void setLoopCount (int count);

    long setMediaTime (long now) throws MediaException;

    void start () throws MediaException;

    void stop () throws MediaException;
}
