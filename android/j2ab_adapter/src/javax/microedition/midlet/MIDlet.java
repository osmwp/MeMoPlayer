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

package javax.microedition.midlet;

import java.util.Properties;

import javax.microedition.io.ConnectionNotFoundException;

import android.app.Activity;
import android.app.Service;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

public abstract class MIDlet {
    public static final String PROTOCOL_HTTP = "http://";
    public static final String PROTOCOL_HTTPS = "https://";
    public static final String PROTOCOL_SMS = "sms:";
    public static final String PROTOCOL_PHONE = "tel:";
    public static final String PROTOCOL_EMAIL = "email:";

    public static MIDlet DEFAULT_MIDLET;
    
    private ContextWrapper mContext;
    private Properties mProperties;
    
    private Handler mHandler = new Handler ();
    private Thread mEventThread = Thread.currentThread ();
    private Object mLock = new Object();
    
    protected MIDlet() {
        DEFAULT_MIDLET = this;
    }

    public void setup (ContextWrapper context, Properties properties) {
        mContext = context;
        mProperties = properties;
    }
    
    public Handler getHandler () {
        return mHandler;
    }

    public boolean post (Runnable r) {
        return mHandler.post (r);
    }
    
    public void invokeAndWait (final Runnable runnable) {
        if (Thread.currentThread () == mEventThread) {
            runnable.run ();
        } else {
            Runnable r = new Runnable () {
                public void run () {
                    synchronized (mLock) {
                        runnable.run ();
                        mLock.notify ();
                    }
                }
            };
            synchronized (mLock) {
                mHandler.post (r);
                try {
                    mLock.wait ();
                } catch (InterruptedException ex) {
                    ex.printStackTrace ();
                }
            }
        }
    }

    public ContextWrapper getContext () {
        return mContext;
    }

    protected abstract void destroyApp (boolean unconditional)
            throws MIDletStateChangeException;

    protected abstract void pauseApp () throws MIDletStateChangeException;

    protected abstract void startApp () throws MIDletStateChangeException;

    public final void notifyDestroyed () {
        if (mContext instanceof Activity) {
            ((Activity)mContext).finish ();
        } else if (mContext instanceof Service) {
            ((Service)mContext).stopSelf ();
        }
    }

    public final void doDestroyApp (boolean unconditional)
            throws MIDletStateChangeException {
        destroyApp(unconditional);
    }

    public final void doStartApp () throws MIDletStateChangeException {
        startApp();
    }

    public final void doPauseApp () throws MIDletStateChangeException {
        pauseApp();
    }

    public boolean platformRequest (String url)
            throws ConnectionNotFoundException {
        Uri content = Uri.parse (url);
        String action;
        if (url.startsWith (PROTOCOL_PHONE)) {
            action = Intent.ACTION_DIAL;
        } else {
            action = Intent.ACTION_DEFAULT;
        }
        Intent intent = new Intent (action, content);
        this.getContext ().startActivity (intent);
        return false;
    }

    public String getAppProperty (String key) {
        return mProperties.getProperty(key);
    }
}
