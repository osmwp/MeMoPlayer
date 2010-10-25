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

import javax.microedition.lcdui.Canvas;
import javax.microedition.media.MediaException;
import javax.microedition.media.control.VideoControl;
import javax.microedition.midlet.MIDlet;

import android.content.res.Resources.Theme;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.AbsoluteLayout;

public class AndroidVideoControl implements VideoControl, SurfaceHolder.Callback {
    private final static String TAG = "AndroidVideoControl";
    static final int USE_DIRECT_VIDEO = 1;

    private Canvas mCanvas;
    private MediaPlayer mPlayer;
    private MIDlet mMIDlet;
    private SurfaceHolder mHolder;
    private SurfaceView mSurfaceView;
    private Object mLockUntilSurfaceCreated = new Object ();
    private boolean mSurfaceCreated = false;
    private int mX, mY, mW, mH;
    
    public AndroidVideoControl (MediaPlayer player) {
        mPlayer = player;
        mMIDlet = MIDlet.DEFAULT_MIDLET; // Arg...
    }

    // Only accessed from AndroidPlayer
    public void release () {
        Log.d (TAG, "releasing ressources...");
        if (mSurfaceView != null && mCanvas != null) {
            mMIDlet.invokeAndWait (new Runnable () {
                public void run () {
                    //mMIDlet.getToolkit ().getLayout ().removeView (mSurfaceView);
                    mCanvas.removeSurfaceView ();
                }
            });
        }
        
        mPlayer = null;
        mMIDlet = null;
        mSurfaceView = null;
        mHolder = null;
        mLockUntilSurfaceCreated = null;
    }
    
    public int getDisplayHeight () {
        return mH;
    }

    public int getDisplayWidth () {
        return mW;
    }

    public int getDisplayX () {
        return mX;
    }

    public int getDisplayY () {
        return mY;
    }

    public byte[] getSnapshot (String imageType) throws MediaException {
        return null;
    }

    public int getSourceHeight () {
        return mPlayer.getVideoHeight ();
    }

    public int getSourceWidth () {
        return mPlayer.getVideoWidth ();
    }

    public Object initDisplayMode (int mode, Object arg) {
        Log.d (TAG, "initDisplayMode: thread:"+Thread.currentThread ());
        if (mode == USE_DIRECT_VIDEO) {
            if (arg instanceof Canvas) {
                mCanvas = (Canvas) arg;
                mMIDlet.invokeAndWait (new Runnable () {
                    public void run () {
                        Log.d (TAG, "initVideoView: "+Thread.currentThread ());
                        mSurfaceCreated = false;
                        mPlayer.setScreenOnWhilePlaying (true);
                        mSurfaceView = new SurfaceView (mMIDlet.getContext ());
                        mCanvas.setSurfaceView (mSurfaceView);
                        //mMIDlet.getToolkit ().getLayout ().addView (mSurfaceView);
                        mHolder = mSurfaceView.getHolder ();
                        mHolder.addCallback (AndroidVideoControl.this);
                        mHolder.setType (SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                    }
                });
                Log.d (TAG,"initVideoView: Wait for suface to get ready...");
                while (mSurfaceCreated == false) {
                    try {
                        synchronized (mLockUntilSurfaceCreated) {
                            mLockUntilSurfaceCreated.wait ();
                        }
                    } catch (InterruptedException e) { }
                }
                Log.d (TAG,"initVideoView: Player now has display holder: "+mHolder);
            } else {
                throw new IllegalArgumentException ("Using direct video, but arg is not a Canvas !");
            }
        } else {
            throw new IllegalArgumentException ("Unsupported mode. Only supports USE_DIRECT_VIDEO");
        }
        return null;
    }

    public void setDisplayFullScreen (boolean fullScreenMode) throws MediaException {
        Log.d (TAG, "setDisplayFullScreen: "+fullScreenMode);
    }

    public void setDisplayLocation (final int x, final int y) {
        Log.d (TAG, "setDisplayLocation: x:"+x+" y:"+y);
        if (mSurfaceView == null) {
            throw new IllegalStateException ("No available surface. Did you call VideoControl.initDisplayMode() ?");
        }
        mMIDlet.invokeAndWait (new Runnable () {
            public void run () {
                AbsoluteLayout.LayoutParams alp = (AbsoluteLayout.LayoutParams)mSurfaceView.getLayoutParams ();
                alp.x = x;
                alp.y = y;
                mSurfaceView.requestLayout ();
            }
        });
        mX = x;
        mY = y;
    }

    public void setDisplaySize (final int width, final int height) throws MediaException {
        Log.d (TAG, "setDisplaySize: w:"+width+" h:"+height);
        if (mSurfaceView == null) {
            throw new IllegalStateException ("No available surface. Did you call VideoControl.initDisplayMode() ?");
        }
        mMIDlet.invokeAndWait (new Runnable () {
            public void run () {
                AbsoluteLayout.LayoutParams alp = (AbsoluteLayout.LayoutParams)mSurfaceView.getLayoutParams ();
                alp.width = width;
                alp.height = height;
                mSurfaceView.requestLayout ();
            }
        });
        mW = width;
        mH = height;
    }

    public void setVisible (final boolean visible) {
        Log.d (TAG, "setVisible: "+visible);
        if (mSurfaceView == null) {
            throw new IllegalStateException ("No available surface. Did you call VideoControl.initDisplayMode() ?");
        }
        mMIDlet.invokeAndWait (new Runnable () {
            public void run () {
                mSurfaceView.setVisibility (visible ? SurfaceView.VISIBLE : SurfaceView.INVISIBLE);
            }
        });
    }

    /*
     *  SurfaceHolder.Callback implementation
     */
    
    public void surfaceChanged (SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: "+holder+" format:"+format+" width:"+width+" height:"+height);
        mW = width;
        mH = height;
    }

    public void surfaceCreated (SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: thread:"+Thread.currentThread ());
        mPlayer.setDisplay (mHolder);
        synchronized (mLockUntilSurfaceCreated) {
            mSurfaceCreated = true;
            mLockUntilSurfaceCreated.notify ();   
        }
    }

    public void surfaceDestroyed (SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }
    
}
