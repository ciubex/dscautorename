/**
 * This file is part of DSCAutoRename application.
 * <p/>
 * Copyright (C) 2015 Claudiu Ciobotariu
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.dscautorename.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.model.FileRenameData;
import ro.ciubex.dscautorename.model.MediaStoreEntity;

/**
 * A thread used to cleanup the media storage data base.
 *
 * @author Claudiu Ciobotariu
 */
public class MediaStorageCleanupThread implements Runnable {
	private final static String TAG = MediaStorageCleanupThread.class.getName();
	private DSCApplication mApplication;
	private ContentResolver mContentResolver;
	private List<MediaStoreEntity> mFilesToScan;

	public MediaStorageCleanupThread(DSCApplication application, List<MediaStoreEntity> filesToScan) {
		this.mApplication = application;
		if (filesToScan != null) {
			this.mFilesToScan = new ArrayList<MediaStoreEntity>(filesToScan);
		} else {
			this.mFilesToScan = new ArrayList<MediaStoreEntity>();
		}
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
		if (!mFilesToScan.isEmpty()) {
			List<Uri> uris = new ArrayList<Uri>();
			uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			if (mApplication.isRenameVideoEnabled()) {
				uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
				uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
			}
			for (MediaStoreEntity entity : mFilesToScan) {
				if (entity.getUri() != null) {
					updateFileRecord(entity.getUri(), entity);
				} else {
					for (Uri uri : uris) {
						if (updateFileRecord(uri, entity)) {
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Update the media store database with data file details.
	 *
	 * @param uri    The file URI.
	 * @param entity The renamed file entity details.
	 * @return True if the media store was updated.
	 */
	private boolean updateFileRecord(Uri uri, MediaStoreEntity entity) {
		String whereClause;
		String whereParam;
		if (entity.getId() != -1) {
			whereClause = MediaStore.MediaColumns._ID + "=?";
			whereParam = "" + entity.getId();
		} else {
			whereClause = MediaStore.MediaColumns.DATA + "=?";
			whereParam = entity.getOldData();
		}
		return updateMediaStoreData(uri, entity.getData(), entity.getTitle(),
				entity.getDisplayName(), whereClause, whereParam);
	}

	/**
	 * Update the media store database with data file details.
	 *
	 * @param uri         The file URI.
	 * @param data        The file data, normally this is the file path.
	 * @param title       The file title, usually is the file name without path and
	 *                    extension.
	 * @param displayName The file display name, usually is the file name without the
	 *                    path.
	 * @param whereClause An SQL WHERE clause.
	 * @param whereParam  The SQL WHERE parameter.
	 * @return True if the media store was updated.
	 */
	private boolean updateMediaStoreData(Uri uri, String data, String title, String displayName,
										 String whereClause, String whereParam) {
		boolean result = false;
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DATA, data);
		contentValues.put(MediaStore.MediaColumns.TITLE, title);
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
		try {
			int count = mContentResolver.update(uri, contentValues,
					whereClause,
					new String[]{whereParam});
			result = (count == 1);
			mApplication.logD(TAG, "Media store update where: " + whereParam + " data: " + data + " result:" + result);
		} catch (Exception ex) {
			mApplication.logE(TAG, "Cannot be updated the content resolver: "
					+ uri.toString() + " Exception: " + ex.getMessage(), ex);
		}
		return result;
	}
}
