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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.task.FileRenameThread;

/**
 * This is a service used to rename files.
 *
 * @author Claudiu Ciobotariu
 */
public class FileRenameService extends Service implements FileRenameThread.Listener {
    private static final String TAG = FileRenameService.class.getName();
    private DSCApplication mApplication;
    private FileRenameThread mFileRenameThread;
    private boolean mStarted;

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
        }
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
            new Thread(mFileRenameThread).start();
            mApplication.logD(TAG, "Service started!");
        }
        return super.onStartCommand(intent, flags, startId);
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
