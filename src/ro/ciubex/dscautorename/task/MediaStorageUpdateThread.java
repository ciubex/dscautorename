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

import java.io.File;
import java.util.HashSet;
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
        useMediaScanner();
    }

    /**
     * Method used to invoke the media scanner to scan renamed files.
     */
    private void useMediaScanner() {
        MediaScannerConnection.scanFile(mApplication.getApplicationContext(), getFilesToScan(), null,
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

    /**
     * Prepare only existing files.
     *
     * @return Array of existing files.
     */
    private String[] getFilesToScan() {
        File file;
        Set<String> updates = new HashSet<>();
        for (String fileFullPath : mFilesToUpdate) {
            file = new File(fileFullPath);
            if (file.exists() && file.length() > 0) {
                updates.add(fileFullPath);
            }
        }
        String[] filesToScan = new String[updates.size()];
        filesToScan = updates.toArray(filesToScan);
        return filesToScan;
    }
}
