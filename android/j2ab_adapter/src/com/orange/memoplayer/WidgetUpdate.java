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
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetUpdate extends Service {
    
    RemoteViews mRemoteViews;
    AppWidgetManager mManager;
    ComponentName mWidget;
    
    // Resources cannot be loaded by R. but must be loaded by name
    // if not we would have to preprocess Java sources when changing
    // the package name !
    int mRLayout;
    int mRImagebuffer, mRWidget;
    
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d("Widget.UpdateService", "onStart() : "+startId);
        
        if (mRemoteViews == null) {
            mRLayout = getResources().getIdentifier("widget", "layout", getPackageName());
            mRImagebuffer = getResources().getIdentifier("imagebuffer", "id", getPackageName());
            mRWidget = getResources().getIdentifier("widget", "id", getPackageName());
            mRemoteViews = new RemoteViews(getPackageName(), mRLayout);
        }
        if (mWidget == null) {
            mWidget = new ComponentName(this, Widget.class);
        }
        
        MIDlet midlet = Common.createMIDlet (this, Common.BOOT_WIDGET);
        try {
            if (midlet != null)  {
                midlet.doStartApp();
            }
        } catch( Exception ex ) {
            ex.printStackTrace();
        }

        // When user clicks on widget, go fullscreen
        Intent defineIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(this, 0, defineIntent, 0);
        mRemoteViews.setOnClickPendingIntent(mRWidget, pendingIntent);
        
        // Push update for this widget to the home screen
        mManager = AppWidgetManager.getInstance(this);
        mManager.updateAppWidget(mWidget, mRemoteViews);
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
    
    // Called by the 
    public void update(Bitmap buffer) {
        mRemoteViews.setImageViewBitmap(mRImagebuffer, buffer);
        mManager.updateAppWidget(mWidget, mRemoteViews);
        Log.d("Widget.UpdateService", "widget updated (render)");
    }
}