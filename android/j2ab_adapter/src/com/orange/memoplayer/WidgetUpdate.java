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

package com.orange.memoplayer;


import javax.microedition.midlet.MIDlet;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetUpdate extends Service {
    public final static int DISPLAY_WIDGET = 0;
    public final static int DISPLAY_LOADER = 1;
    public final static int DISPLAY_ALERT = 2;
    
    private RemoteViews mRemoteViews;
    private AppWidgetManager mManager;
    private ComponentName mWidget;
    
    // Resources cannot be loaded by R. but must be loaded by name
    // if not we would have to preprocess Java sources when changing
    // the package name !
    private int mRWidget;
    private int mRWidgetLayout, mRImagebuffer;
    private int mRAlertLayout, mRAlertMessage;
    private int mRLoaderLayout, mRLoaderMessage;
    private Bitmap mImageBuffer = null;
    
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d("Widget.UpdateService", "onStart() : "+startId);
        if (mManager == null) {
            mManager = AppWidgetManager.getInstance(this);
        }
        if (mRWidget == 0) {
            mRWidget = getResources().getIdentifier("widget", "id", getPackageName());
        }
        if (mWidget == null) {
            mWidget = new ComponentName(this, Widget.class);
        }

        try {
            MIDlet midlet = Common.createMIDlet (this, Common.BOOT_WIDGET);
            if (midlet != null)  {
                midlet.doStartApp();
            }
        } catch( Exception ex ) {
            ex.printStackTrace();
            displayAlert (null);
        }

        Log.d("Widget.UpdateService", "widget updated");
    }
    
    public void onCreate() {
        Log.d("Widget.UpdateService", "onCreate()");
        super.onCreate();
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("Widget.UpdateService", "onConfigurationChanged()");
    }
    
    public void onDestroy() {
        Log.d("Widget.UpdateService", "onDestroy()");
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public void setImageBuffer (Bitmap buffer) {
        mImageBuffer = buffer;
    }

    public void displayWidget () {
        if (mImageBuffer != null) {
            if (mRWidgetLayout == 0) {
                mRWidgetLayout = getResources().getIdentifier("widget", "layout", getPackageName());
                mRImagebuffer = getResources().getIdentifier("imagebuffer", "id", getPackageName());
            }
            if (mRemoteViews == null || mRemoteViews.getLayoutId() != mRWidgetLayout) {
                mRemoteViews = getRemoteView(mRWidgetLayout);
            }
            
            synchronized (mImageBuffer) {
                mRemoteViews.setImageViewBitmap(mRImagebuffer, mImageBuffer);
            }
            mManager.updateAppWidget(mWidget, mRemoteViews);
            Log.i("Widget.UpdateService", "widget updated");
        } else {
            Log.i("Widget.UpdateService", "widget NOT updated");
        }
    }

    public void displayAlert(String message) {
        if (mRAlertLayout == 0) {
            mRAlertLayout = getResources().getIdentifier("widget_alert", "layout", getPackageName());
            mRAlertMessage = getResources().getIdentifier("alertmessage", "id", getPackageName());
        }
        if (message == null || message.length() == 0) {
            int res = getResources().getIdentifier("widget_alert", "string", getPackageName());
            message = getResources().getText(res).toString();
        }
        if (mRemoteViews == null || mRemoteViews.getLayoutId() != mRAlertLayout) {
            mRemoteViews = getRemoteView(mRAlertLayout);
        }
        mRemoteViews.setTextViewText(mRAlertMessage, message);
        mManager.updateAppWidget(mWidget, mRemoteViews);
        Log.d("Widget.displayAlert", "Alert displayed with message: "+message);

    }

    public void displayLoader(String message) {
        if (mRLoaderLayout == 0) {
            mRLoaderLayout = getResources().getIdentifier("widget_loader", "layout", getPackageName());
            mRLoaderMessage = getResources().getIdentifier("loadermessage", "id", getPackageName());
        }
        if (message == null || message.length() == 0) {
            int res = getResources().getIdentifier("widget_loader", "string", getPackageName());
            message = getResources().getText(res).toString();
        }
        if (mRemoteViews == null || mRemoteViews.getLayoutId() != mRLoaderLayout) {
            mRemoteViews = getRemoteView(mRLoaderLayout);
        }
        mRemoteViews.setTextViewText(mRLoaderMessage, message);
        mManager.updateAppWidget(mWidget, mRemoteViews);
        Log.d("Widget.displayAlert", "Loader displayed with message: "+message);
    }

    private RemoteViews getRemoteView (int layout) {
        RemoteViews rv = new RemoteViews(getPackageName(), layout);

        // When user clicks on widget, go fullscreen
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(this, 0, intent, 0);
        rv.setOnClickPendingIntent(mRWidget, pendingIntent);
        return rv;
    }
}