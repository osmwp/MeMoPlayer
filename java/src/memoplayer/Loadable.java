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
//use by File Scene MediaObject


/**
 * @author cazoulatr
 * used by  File, Scene and MediaSensor to give the loading staus and progress
 *
 */
interface Loadable {
    final static int READY    = 1;
    final static int OK       = 1;
    final static int QUEUED   = 2;
    final static int ERROR    = 3;
    final static int OPENING  = 4;
    final static int LOADING  = 5;
    final static int LOADED   = 6;
    final static int CLOSED   = 7;
    final static int STOPPED  = 8;
    final static int PLAYING  = 9;
    final static int PAUSED   = 10;
    final static int EOM      = 11;
    final static int BUFFERING= 12;
    
    final static String[] STATE_MSG = {
    	"",
        "ready",
        "queued",
        "error",
        "opening",
        "playing",
        "loaded",
        "closed",
        "stopped",
        "playing",
        "paused",
        "endOfMedia",
        "buffering"
    };
    
    int getState (); // return the current state : one of the above constants

    int getCurrent (); // Return the current 

    int getDuration ();

    String getName ();

    String getErrorMessage ();
}
