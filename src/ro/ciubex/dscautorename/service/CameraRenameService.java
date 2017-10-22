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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.receiver.CameraEventReceiver;

/**
 * This is a service which should keep active the camera broadcast receiver.
 *
 * @author Claudiu Ciobotariu
 */
public class CameraRenameService extends Service {
    private static final String TAG = CameraRenameService.class.getName();
    private DSCApplication mApplication;
    private AlarmManager mAlarmService;
    private CameraEventReceiver mCameraEventReceiver;
    private boolean mAlreadyStarted;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    public static final String ENABLE_CAMERA_RENAME_SERVICE = "enableCameraRenameService";

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            if (data != null) {
                handleReceivedMessage(data);
            }
        }
    }

    /**
     * Handle received message data.
     *
     * @param data Data received.
     */
    private void handleReceivedMessage(Bundle data) {
        if (checkServiceMessage(data.getString(CameraRenameService.ENABLE_CAMERA_RENAME_SERVICE))) {
            registerReceivers();
        } else {
            unregisterReceivers();
        }
    }

    /**
     * Check the service message.
     *
     * @param value The message to be checked.
     */
    private boolean checkServiceMessage(String value) {
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Return the communication channel to the service. Return null because
     * clients can not bind to the service.
     *
     * @param intent Not used.
     * @return The messenger binder.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /**
     * Return the communication channel to the service.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * Called by the system when the service is first created.
     */
    @Override
    public void onCreate() {
        Context appCtx = getApplicationContext();
        if (appCtx instanceof DSCApplication) {
            mApplication = (DSCApplication) appCtx;
            mAlarmService = (AlarmManager) mApplication.getSystemService(Context.ALARM_SERVICE);
            mApplication.logD(TAG, "onCreate()");
        }
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is
     * being removed.
     */
    @Override
    public void onDestroy() {
        unregisterReceivers();
        mApplication.logD(TAG, "onDestroy()");
    }

    /**
     * This is called if the service is currently running and the user has
     * removed a task that comes from the service's application.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        mApplication.logD(TAG, "onTaskRemoved()");
        unregisterReceivers();
        prepareForRestartService();
        super.onTaskRemoved(rootIntent);
    }

    /**
     * This is a workaround method to ensure service restart.
     */
    private void prepareForRestartService() {
        Intent restartService = new Intent(mApplication, CameraRenameService.class);
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                mApplication, 1, restartService, PendingIntent.FLAG_ONE_SHOT);
        mAlarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000, restartServicePI);
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
        if (!mAlreadyStarted) {
            mAlreadyStarted = true;
            mApplication.logD(TAG, "onStartCommand: " + startId);
            registerReceivers();
            if (mApplication.getCameraServiceInstanceCount() < 1) {
                mApplication.launchAutoRenameTask(null, true, null, false);
            }
            mApplication.increaseCameraServiceInstanceCount();
        }
        return Service.START_STICKY;
    }

    /**
     * Register all receivers.
     */
    private void registerReceivers() {
        mApplication.logD(TAG, "Register receivers.");
        if (mCameraEventReceiver == null) {
            mCameraEventReceiver = new CameraEventReceiver();
            final IntentFilter theFilter = new IntentFilter();
            int apiLevel = Build.VERSION.SDK_INT;
            if (apiLevel < 14) {
                theFilter.addAction("com.android.camera.NEW_PICTURE");
                theFilter.addAction("com.android.camera.NEW_VIDEO");
            }
            theFilter.addAction("android.hardware.action.NEW_PICTURE");
            theFilter.addAction("android.hardware.action.NEW_VIDEO");
            try {
                theFilter.addDataType("image/*");
                theFilter.addDataType("video/*");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                mApplication.logE(TAG, "Register the CameraEventReceiver, MalformedMimeTypeException:", e);
            }
            mApplication.getApplicationContext().registerReceiver(mCameraEventReceiver, theFilter);
            mApplication.logD(TAG, "Registered the CameraEventReceiver.");
        }
    }


    /**
     * Method used to unregister all receivers.
     */
    private void unregisterReceivers() {
        if (mCameraEventReceiver != null) {
            mApplication.logD(TAG, "Unregister receivers.");
            unregisterBroadcastReceiver(mCameraEventReceiver);
            mCameraEventReceiver = null;
            stopSelf();
        }
    }

    /**
     * Method used to unregister a broadcast receiver
     *
     * @param receiver The receiver to be unregistered.
     */
    private void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
        if (receiver != null) {
            int apiLevel = Build.VERSION.SDK_INT;
            if (apiLevel >= 7) {
                try {
                    mApplication.getApplicationContext().unregisterReceiver(receiver);
                } catch (IllegalArgumentException e) {
                }
            } else {
                mApplication.getApplicationContext().unregisterReceiver(receiver);
            }
        }
    }
}
