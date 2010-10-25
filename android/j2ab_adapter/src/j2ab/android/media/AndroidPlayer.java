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

package j2ab.android.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import j2ab.android.media.control.AndroidVideoControl;
import j2ab.android.media.control.AndroidVolumeControl;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.midlet.MIDlet;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.util.Log;

public class AndroidPlayer implements Player, OnCompletionListener, OnBufferingUpdateListener,
                                   OnSeekCompleteListener, OnErrorListener {

    private final static String TAG = "AndroidPlayer";
    
    private MediaPlayer mPlayer;
    private String mLocator;
    private String mContentType;
    private boolean mBuffering;
    private int mState = UNREALIZED;
    
    private AndroidVideoControl mVideoControl;
    private AndroidVolumeControl mVolumeControl;
    
    public AndroidPlayer (InputStream ins, String type) throws IOException {
        if (ins == null) {
            throw new RuntimeException ("Player: InputStream is null !");
        }
        // MediaPlayer cannot read byte streams... using a temp file
        File temp = File.createTempFile ("mediaplayer", "dat", MIDlet.DEFAULT_MIDLET.getContext ().getFilesDir ());
        FileOutputStream out = new FileOutputStream (temp);
        byte buf[] = new byte[2048];
        int numread = ins.read (buf);
        while (numread > 0) {
            out.write (buf, 0, numread);
            numread = ins.read (buf);
        }
        ins.close ();
        out.close ();
        mLocator = temp.getAbsolutePath ();
        mContentType = type;
    }
    
    public AndroidPlayer (String locator) {
        mLocator = locator; 
    }

    public void realize () throws MediaException {
        switch (mState) {
        case CLOSED:
            throw new IllegalStateException("Player is already closed !");
        case UNREALIZED:
            if (mPlayer == null) {
                MIDlet.DEFAULT_MIDLET.invokeAndWait (new Runnable () {
                    public void run () {
                        mPlayer = new MediaPlayer ();
                        mPlayer.setOnCompletionListener (AndroidPlayer.this);
                        mPlayer.setOnBufferingUpdateListener (AndroidPlayer.this);
                        mPlayer.setOnSeekCompleteListener (AndroidPlayer.this);
                        mPlayer.setOnErrorListener (AndroidPlayer.this);
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        try {
                            mPlayer.setDataSource (mLocator);
                        } catch (IOException e) {
                            mPlayer = null;
                            Log.e (TAG, "Exception: "+e.getMessage ());
                        }
                    }
                });
                if (mPlayer == null) {
                    throw new MediaException ("Cannot realize player");
                }
            }    
            mState = REALIZED;
            break;
        case REALIZED:
        case PREFETCHED:
        case STARTED:
            return; // ignore
        }
    }
    
    public void prefetch () throws MediaException {
        switch (mState) {
        case CLOSED:
            throw new IllegalStateException("Player is already closed !");
        case UNREALIZED:
            realize ();
        case REALIZED:
        case PREFETCHED:
            try {
                mPlayer.prepare ();
            } catch (IOException e) {
                throw new MediaException ("Cannot prefetch player: "+e.getMessage ());
            }
            mState = PREFETCHED;
            break;
        case STARTED:
            return; // ignore
        }
        
    }
    
    public void start () throws MediaException {
        switch (mState) {
        case CLOSED: 
            throw new IllegalStateException("Player is already closed !");
        case UNREALIZED: 
        case REALIZED:
            prefetch ();
        case PREFETCHED:
            mPlayer.start ();
            mState = STARTED;
            Log.i(TAG,"start: DONE");
            updatePlayerListener (PlayerListener.STARTED, "");
            break;
        case STARTED: 
            return; // ignore
        }
    }

    public void stop () throws MediaException {
        switch (mState) {
        case CLOSED: 
            throw new IllegalStateException("Player is already closed !");
        case UNREALIZED: 
        case REALIZED:
        case PREFETCHED:
            return; // ignore
        case STARTED: 
            mPlayer.stop ();
            mState = PREFETCHED;
            updatePlayerListener (PlayerListener.STOPPED, "");
        }
    }
    
    public void deallocate () {
        //TODO: implement realize() interruption feature ? see MMAPI specifications... 
        switch (mState) {
        case CLOSED: 
            throw new IllegalStateException("Player is already closed !");
        case UNREALIZED: 
        case REALIZED:
            return; // ignore
        case PREFETCHED:
        case STARTED: 
            try { stop (); } catch (Exception e) {} // ignore errors
        }
        mPlayer.release ();
    }
    
    public void close () {
        if (mVideoControl != null) {
            mVideoControl.release ();
            mVideoControl = null;
        }
        if (mVolumeControl != null) {
            mVolumeControl.release ();
            mVolumeControl = null;
        }
        mPlayer.release ();
        mPlayer = null;
        mState = CLOSED;
        // Remove temp file when created from local stream
        if (mLocator.endsWith ("dat")) {
            File f = new File (mLocator);
            if (f.exists ()) {
                f.delete ();
            }
        }
        updatePlayerListener (PlayerListener.CLOSED, "");
    }

    public void setLoopCount (int count) {
        //TODO ignored for now...
    }

    public long setMediaTime (long now) throws MediaException {
        //TODO ignored for now
        return 0;
    }

    public String getContentType () {
        if (mState == CLOSED || mState == UNREALIZED) throw new IllegalStateException("Player is already stopped !");
        return mContentType != null ? mContentType : "unknown";
    }

    public long getDuration () {
        if (mState == CLOSED) throw new IllegalStateException("Player is already stopped !");
        return mPlayer != null ? mPlayer.getDuration () : TIME_UNKNOWN;
    }

    public long getMediaTime () {
        if (mState == CLOSED) throw new IllegalStateException("Player is already stopped !");
        return mPlayer != null ? mPlayer.getCurrentPosition () : TIME_UNKNOWN;
    }

    public int getState () {
        return mState;
    }
    
    public Control getControl (String controlType) {
        if (controlType.equals ("VideoControl")) {
            if (mVideoControl == null) {
                mVideoControl = new AndroidVideoControl (mPlayer);
            }
            return mVideoControl;
        } else if (controlType.equals ("VolumeControl")) {
            if (mVolumeControl == null) {
                mVolumeControl = new AndroidVolumeControl (mPlayer);
            }
            return mVolumeControl;
        }
        return null;
    }

    public Control[] getControls () {
        return new Control[] { getControl ("VideoControl"), getControl ("VolumeControl") };
    }

    /*
     * PlayerListenner handling
     */
    
    class AsyncEvent {
        String m_event;
        AsyncEvent m_next;
        
        public AsyncEvent (String event, AsyncEvent next) {
            m_event = event;
            m_next = next;
        }
        
        public void dispatch () {
            if (m_next != null) m_next.dispatch ();
            m_playerListener.playerUpdate (AndroidPlayer.this, m_event, "");
        }
    }
    
    private AsyncEvent m_events;
    private PlayerListener m_playerListener; // KISS: only support one listener...
    private Thread m_listenerThread;
    
    private synchronized void updatePlayerListener (String event, String eventData) {
        if (m_playerListener != null) {
            m_events = new AsyncEvent (event, m_events);
            if (m_listenerThread == null) {
                m_listenerThread = new Thread () {
                    public void run () {
                        while (true) {
                            if (m_events != null) {
                                m_events.dispatch ();
                                m_events = null;
                            }
                            try {
                                synchronized (m_listenerThread) {
                                    m_listenerThread.wait ();
                                }
                            } catch (InterruptedException e) {}
                        }
                    }
                };
                m_listenerThread.start ();
            } else {
                synchronized (m_listenerThread) {
                    m_listenerThread.notify ();
                }  
            }
        }
    }
    
    public synchronized void addPlayerListener (PlayerListener playerListener) {
        m_playerListener = playerListener; 
    }
    
    public synchronized void removePlayerListener (PlayerListener playerListener) {
        if (playerListener == m_playerListener) {
            m_playerListener = null;
            m_events = null;
            m_listenerThread.interrupt ();
            m_listenerThread = null;
        }
    }
    
    /*
     *  MediaPlayer listeners
     */

    public void onCompletion (MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        mState = PREFETCHED; // TODO CONCURENCY ACCESS !!
        updatePlayerListener (PlayerListener.END_OF_MEDIA, "");
    }

    public void onBufferingUpdate (MediaPlayer mp, int percent) {
        Log.d(TAG, "onBufferingUpdate: "+percent);
        if (percent<100) {
            if (!mBuffering) {
                mBuffering = true;
                updatePlayerListener (PlayerListener.BUFFERING_STARTED, "");
            }
        } else {
            mBuffering = false;
            updatePlayerListener (PlayerListener.BUFFERING_STOPPED, "");
        }
    }

    public void onSeekComplete (MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete");
    }

    public boolean onError (MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError: "+(what==MediaPlayer.MEDIA_ERROR_UNKNOWN ? "unknown" : "server died")+ " code:"+extra);
        return false;
    }   
}
