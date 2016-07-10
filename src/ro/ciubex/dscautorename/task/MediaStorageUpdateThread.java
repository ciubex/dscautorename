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
package ro.ciubex.dscautorename.task;

import android.media.MediaScannerConnection;
import android.net.Uri;

import java.util.Set;

import ro.ciubex.dscautorename.DSCApplication;

/**
 * A thread used to cleanup the media storage data base.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class MediaStorageUpdateThread implements Runnable {
    private final static String TAG = MediaStorageUpdateThread.class.getName();
    private DSCApplication mApplication;
    private Set<String> mFilesToUpdate;

    public MediaStorageUpdateThread(DSCApplication application, Set<String> filesToUpdate) {
        mApplication = application;
        mFilesToUpdate = filesToUpdate;
    }

    @Override
    public void run() {
        final int size = mFilesToUpdate.size();
        String[] filesToScan = new String[size];
        filesToScan = mFilesToUpdate.toArray(filesToScan);
        MediaScannerConnection.scanFile(mApplication.getApplicationContext(), filesToScan, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        mApplication.logD(TAG, "File " + path + " was scanned successfully: " + uri);
                        if (uri != null && mApplication.isSendBroadcastEnabled()) {
                            mApplication.sendBroadcastMessage(uri);
                        }
                    }
                }
        );
    }
}
