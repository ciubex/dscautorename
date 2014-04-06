/**
 * This file is part of DSCAutoRename application.
 * 
 * Copyright (C) 2014 Claudiu Ciobotariu
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.model.DSCImage;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

/**
 * An AsyncTask used to rename a file.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class RenameFileAsyncTask extends AsyncTask<Void, Void, Integer> {
	private final String TAG = getClass().getName();
	private DSCApplication mApplication;
	private ContentResolver mContentResolver;
	private Listener mListener;

	public interface Listener {
		public void onTaskStarted();

		public void onTaskFinished(int count);
	}

	public RenameFileAsyncTask(DSCApplication application) {
		this(application, null);
	}

	public RenameFileAsyncTask(DSCApplication application, Listener listener) {
		this.mApplication = application;
		this.mListener = listener;
	}

	/**
	 * Method invoked on the background thread.
	 * 
	 * @param params
	 *            In this case are not used parameters.
	 * @return Is returned a void value.
	 */
	@Override
	protected Integer doInBackground(Void... params) {
		mContentResolver = mApplication.getContentResolver();
		int count = 0;
		if (mContentResolver != null) {
			List<DSCImage> list = getImageList();
			while (!list.isEmpty() && !mApplication.isRenameFileTaskCanceled()) {
				for (DSCImage dscImage : list) {
					String oldFileName = dscImage.getmData();
					if (oldFileName != null) {
						File oldFile = getFile(oldFileName);
						if (oldFile != null && oldFile.exists()) {
							if (renameFile(oldFile, oldFileName)) {
								count++;
							}
						} else {
							Log.e(TAG, "The file:" + oldFileName
									+ " does not exist.");
						}
					} else {
						Log.e(TAG, "The content resolver does not exist.");
					}
				}
				list = getImageList();
			}
			mApplication.setRenameFileTaskBusy(false);
			if (count > 0) {
				mApplication.increaseFileRenameCount(count);
			}
		}
		return count;
	}

	/**
	 * Runs on the UI thread before doInBackground(Params...).
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (mListener != null) {
			mListener.onTaskStarted();
		}
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...). The specified
	 * result is the value returned by doInBackground(Params...).
	 * 
	 * @param result
	 *            The result of the operation computed by
	 *            doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(Integer count) {
		super.onPostExecute(count);
		if (mListener != null) {
			mListener.onTaskFinished(count);
		}
		mApplication.setRenameFileTaskCanceled(false);
	}

	/**
	 * Rename the old file with provided new name.
	 * 
	 * @param oldFile
	 *            The old file to be renamed.
	 * @param oldFileName
	 *            The old file name.
	 */
	private boolean renameFile(File oldFile, String oldFileName) {
		boolean success = false;
		int index = 0;
		String newFileName;
		File newFile;
		do {
			newFileName = getNewFileName(oldFile, oldFileName, index);
			newFile = new File(oldFile.getParentFile(), newFileName);
			index++;
		} while (newFile.exists());
		success = updateMediaStoreImages(oldFileName, newFile);
		if (success) {
			success = oldFile.renameTo(newFile);
			if (!success) {
				Log.e(TAG, "The file " + oldFileName + " cannot be renamed!");
			}
		} else {
			Log.e(TAG, "The file cannot be renamed: " + oldFileName);
		}
		return success;
	}

	/**
	 * Update the Media Store Image with the new file name.
	 * 
	 * @param oldFileName
	 *            The old image file name.
	 * @param newFileName
	 *            The new image file name.
	 * @return True if the image file name was updated.
	 */
	private boolean updateMediaStoreImages(String oldFileName, File newFile) {
		String oldImageId = getOldImageId(oldFileName);
		boolean result = false;
		if (oldImageId != null && oldImageId.length() > 0) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(MediaStore.MediaColumns.DATA,
					newFile.getAbsolutePath());
			contentValues.put(MediaStore.MediaColumns.TITLE,
					removeExtensionFileName(newFile.getName()));
			contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,
					newFile.getName());
			try {
				int count = mContentResolver.update(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						contentValues, "_id=" + oldImageId, null);
				result = (count == 1);
			} catch (Exception ex) {
				Log.e(TAG,
						"Cannot be updated the content resolver: "
								+ MediaStore.Images.Media.EXTERNAL_CONTENT_URI
										.toString() + " Exception: "
								+ ex.getMessage(), ex);
			}
		} else {
			Log.e(TAG, "No image id for file: " + oldFileName);
		}
		return result;
	}

	/**
	 * Get the old image id.
	 * 
	 * @param oldFileName
	 *            The old file name.
	 * @return The image id.
	 */
	private String getOldImageId(String oldFileName) {
		Cursor cursor;
		for (cursor = MediaStore.Images.Media.query(mContentResolver,
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.MediaColumns._ID },
				(new StringBuilder()).append(MediaStore.MediaColumns.DATA)
						.append("='").append(oldFileName).append("'")
						.toString(), null, null); cursor == null
				|| !cursor.moveToNext();) {
			return null;
		}

		String imageId = cursor.getString(0);
		cursor.close();
		return imageId;
	}

	/**
	 * 
	 * @param fullFilePath
	 * @return
	 */
	private String removeExtensionFileName(String fullFilePath) {
		if (fullFilePath != null && fullFilePath.length() > 0) {
			int idx = fullFilePath.lastIndexOf('.');
			if (idx > -1 && idx < fullFilePath.length()) {
				fullFilePath = fullFilePath.substring(0, idx);
			}
		}
		return fullFilePath;
	}

	/**
	 * Rename the file provided as parameter.
	 * 
	 * @param file
	 *            The file to be renamed.
	 * @param fullFileName
	 *            The old file name.
	 * @param index
	 *            The index used to generate a unique file name if the file is
	 *            already exist.
	 */
	private String getNewFileName(File file, String fullFileName, int index) {
		long milliseconds = file.lastModified();
		String pattern = mApplication.getFileNameFormat();
		Locale locale = mApplication.getLocale();
		String newFileName = new SimpleDateFormat(pattern, locale)
				.format(new Date(milliseconds));
		if (index > 0) {
			newFileName += "_" + index;
		}
		newFileName += getFileExtension(fullFileName);
		return newFileName;
	}

	/**
	 * Obtain the file extension, based on the full file name.
	 * 
	 * @param fileName
	 *            The file name.
	 * @return The file extension.
	 */
	private String getFileExtension(String fileName) {
		String ext = ".jpg";
		int idx = fileName.lastIndexOf(".");
		if (idx > 0) {
			ext = fileName.substring(idx);
		}
		return ext;
	}

	/**
	 * Get the file object based on the name provided.
	 * 
	 * @param fileName
	 *            The file name.
	 * @return The file object.
	 */
	private File getFile(String fileName) {
		File file = new File(fileName);
		if (file.exists() && file.isFile()) {
			return file;
		}
		return null;
	}

	/**
	 * Obtain a list with all images ready to be renamed.
	 * 
	 * @return A list with all images ready to be renamed.
	 */
	private List<DSCImage> getImageList() {
		List<DSCImage> list = new ArrayList<DSCImage>();
		Cursor cursor = null;
		String[] columns = new String[] { MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DATA };
		String where = MediaStore.MediaColumns.DATA + " LIKE ?";
		String whereArg = "%" + mApplication.getOriginalImagePrefix() + "%";

		String[] selectionArgs = new String[] { whereArg };
		try {
			cursor = mContentResolver.query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
					where, selectionArgs, null);
			if (cursor != null) {
				int id;
				String data;
				while (cursor.moveToNext()) {
					id = cursor.getInt(cursor
							.getColumnIndex(MediaStore.MediaColumns._ID));
					data = cursor.getString(cursor
							.getColumnIndex(MediaStore.MediaColumns.DATA));
					list.add(new DSCImage(id, data));
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, "getImageList Exception: " + ex.getMessage(), ex);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return list;
	}
}
