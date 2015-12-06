/**
 * This file is part of DSCAutoRename application.
 *
 * Copyright (C) 2015 Claudiu Ciobotariu
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

import android.content.ContentResolver;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.model.FileRenameData;

/**
 * A thread used to cleanup the media storage data base.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class MediaStorageCleanupThread implements Runnable {
	private final static String TAG = MediaStorageCleanupThread.class.getName();
	private DSCApplication mApplication;
	private ContentResolver mContentResolver;
	private List<String> mUpdateMediaStorageFiles;
	private List<FileRenameData> mDeleteMediaStorageFiles;
	private int onScanCompletedCount;

	public MediaStorageCleanupThread(DSCApplication application,
									 List<String> updateMediaStorageFiles,
									 List<FileRenameData> deleteMediaStorageFiles) {
		this.mApplication = application;
		this.mUpdateMediaStorageFiles = new ArrayList<String>(updateMediaStorageFiles);
		this.mDeleteMediaStorageFiles = new ArrayList<FileRenameData>(deleteMediaStorageFiles);
	}

	@Override
	public void run() {
		mContentResolver = mApplication.getContentResolver();
		updateMediaStorage();
	}


	/**
	 * Update media storage for renamed files.
	 */
	private void updateMediaStorage() {
		if (!mUpdateMediaStorageFiles.isEmpty()) {
			final int size = mUpdateMediaStorageFiles.size();
			String[] filesToScan = new String[size];
			filesToScan = mUpdateMediaStorageFiles.toArray(filesToScan);
			onScanCompletedCount = 0;
			MediaScannerConnection.scanFile(
					DSCApplication.getAppContext(),
					filesToScan,
					null,
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) {
							onScanCompletedCount++;
							mApplication.logD(TAG, "File " + path + " was scanned successfully: " + uri);
							if (onScanCompletedCount == size) {
								cleanupMediaStore(mDeleteMediaStorageFiles);
							}
						}
					});
		} else {
			cleanupMediaStore(mDeleteMediaStorageFiles);
		}
	}

	/**
	 * Method used to cleanup the media store. This mostly should remove the old zeros files references.
	 */
	private void cleanupMediaStore(List<FileRenameData> deleteMediaStorageFiles) {
		mApplication.logD(TAG, "cleanupMediaStore list size: " + deleteMediaStorageFiles.size());
		if (!deleteMediaStorageFiles.isEmpty()) {
			for (FileRenameData data : deleteMediaStorageFiles) {
				try {
					int count = mContentResolver.delete(data.getUri(), MediaStore.MediaColumns.DATA + "=?", new String[]{"" + data.getData()});
					mApplication.logD(TAG, "" + count + " item(s) removed from Media Store: " + data);
				} catch (Exception ex) {
					mApplication.logE(TAG, "Cannot be updated the content resolver, data: "
							+ data + " Exception: " + ex.getMessage(), ex);
				}
			}
		}
	}
}
