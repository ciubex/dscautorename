/**
 * This file is part of DSCAutoRename application.
 *
 * Copyright (C) 2016 Claudiu Ciobotariu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.dscautorename.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.task.FileRenameThread;

/**
 * This is a service used to rename files.
 *
 * @author Claudiu Ciobotariu
 */
public class FileRenameService extends Service implements FileRenameThread.Listener {
    private static final String TAG = FileRenameService.class.getName();
    public static final String APP_CHANNEL_ID = "ro.ciubex.dscautorename.service.FileRenameService";
    public static final String APP_CHANNEL_NAME = "DSC Auto Rename";

    private DSCApplication mApplication;
    private FileRenameThread mFileRenameThread;
    private boolean mStarted;
    private NotificationManager mManager;
    private Notification mNotification;
    private static final int NOTIFICATION_ID = 84555;

    /**
     * Called by the system when the service is first created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Context appCtx = getApplicationContext();
        if (appCtx instanceof DSCApplication) {
            mApplication = (DSCApplication) appCtx;
            mFileRenameThread = new FileRenameThread(mApplication, this, false, null);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannels();
            }
        }
    }

    /**
     * Create a notification channel, needed to attach a notification in Android O.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel androidChannel = new NotificationChannel(APP_CHANNEL_ID,
                APP_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        // Sets whether notifications posted to this channel should display notification lights
        androidChannel.enableLights(true);
        // Sets whether notification posted to this channel should vibrate.
        androidChannel.enableVibration(true);
        // Sets the notification light color for notifications posted to this channel
        androidChannel.setLightColor(Color.BLUE);
        // Sets whether notifications posted to this channel appear on the lockscreen or not
        androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(androidChannel);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is
     * being removed.
     */
    @Override
    public void onDestroy() {
        mStarted = false;
        mApplication.resetCameraServiceInstanceCount();
        mApplication.logD(TAG, "Service destroyed!");
        super.onDestroy();
    }

    /**
     * Called by the system every time a client explicitly starts the service by
     * calling startService(Intent), providing the arguments it supplied and a
     * unique integer token representing the start request. Do not call this
     * method directly.
     *
     * @param intent  The Intent supplied to startService(Intent), as given.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return We want this service to continue running until it is explicitly
     * stopped, so return sticky.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mStarted) {
            mStarted = true;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startNotificationForAPI26();
            }
            startRenameThread();
            mApplication.logD(TAG, "Service started!");

        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Start foreground service and put a notification up, as a requirement for Android O.
     */
    @TargetApi(Build.VERSION_CODES.O)
    private void startNotificationForAPI26() {
        startForeground(NOTIFICATION_ID, getNotification());
    }

    /**
     * Get the notification manager.
     *
     * @return The notification manager.
     */
    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    /**
     * Build the notification to be used for this foreground service.
     * @return The notification created.
     */
    @TargetApi(Build.VERSION_CODES.O)
    private Notification getNotification() {
        if (mNotification == null) {
            Notification.Builder builder = new Notification.Builder(getApplicationContext(), APP_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.rename_service_started_foreground))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true);
            mNotification = builder.build();
        }
        return mNotification;
    }

    /**
     * Start the rename thread.
     */
    private void startRenameThread() {
        new Thread(mFileRenameThread).start();
    }

    /**
     * Return the communication channel to the service. Return null because
     * clients can not bind to the service.
     *
     * @param intent Not used.
     * @return NULL
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onThreadStarted() {

    }

    @Override
    public void onThreadUpdate(int position, int max) {

    }

    @Override
    public void onThreadFinished(int count) {
        mStarted = false;
        mApplication.logD(TAG, "Service invoked onThreadFinished.");
        mApplication.rescheduleMediaContentJobService();
        stopSelf();
    }

    @Override
    public boolean isFinishing() {
        return false;
    }
}
