package com.johnwayner.plugins;

import android.app.Application;
import android.content.Context;

/**
 * Created by johnwayner on 6/19/15.
 */
@SuppressWarnings("DefaultFileTemplate")
public class WayneTrafficCanvasApplication extends Application {

    private static Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
    }

    public static Context getContext() {
        return applicationContext;
    }
}
