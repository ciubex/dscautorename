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

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.FileRenameData;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.util.RenamePatternsUtilities;
import ro.ciubex.dscautorename.util.Utilities;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

/**
 * An AsyncTask used to rename a file.
 *
 * @author Claudiu Ciobotariu
 */
public class RenameFileAsyncTask extends AsyncTask<Void, Void, Integer> {
	private final static String TAG = RenameFileAsyncTask.class.getName();
	private DSCApplication mApplication;
	private ContentResolver mContentResolver;
	private final WeakReference<Listener> mListener;
	private List<FileRenameData> mListFiles;
	private SelectedFolderModel[] mFoldersScanning;
	private static int mProgressPosition, mProgressLastPosition, mProgressMax;
	private static String mProgressMessage;
	private FileNameModel[] mFileNameModels;
	private List<String> mUpdateMediaStorageFiles;
	private FileRenameData mPreviousFileRenameData;
	private String mPreviousFileNameModel;
	private int mPreviousFileNameModelCount;
	private boolean mIsUriPermissionGranted;
	private RenamePatternsUtilities renamePatternsUtilities;

	public interface Listener {
		public void onTaskStarted();

		public void onTaskUpdate(int position, int max, String message);

		public void onTaskFinished(int count);

		public boolean isFinishing();
	}

	public RenameFileAsyncTask(DSCApplication application) {
		this(application, null);
	}

	public RenameFileAsyncTask(DSCApplication application, Listener listener) {
		this.mApplication = application;
		this.mListener = new WeakReference<Listener>(listener);
		mUpdateMediaStorageFiles = new ArrayList<String>();
		mApplication.setRenameFileTaskRunning(true);
		mFileNameModels = mApplication.getOriginalFileNamePattern();
		renamePatternsUtilities = new RenamePatternsUtilities(mApplication);
	}

	/**
	 * Method invoked on the background thread.
	 *
	 * @param params In this case are not used parameters.
	 * @return Is returned a void value.
	 */
	@Override
	protected Integer doInBackground(Void... params) {
		mContentResolver = mApplication.getContentResolver();
		mProgressPosition = 0;
		mProgressLastPosition = -1;
		boolean enableFilter;
		if (mContentResolver != null) {
			enableFilter = mApplication.isEnabledFolderScanning();
			if (enableFilter) {
				mFoldersScanning = mApplication.getSelectedFolders();
			}
			mApplication.updateMountedVolumes();
			mApplication.updateSelectedFolders();
			if (mApplication.getSdkInt() >= 21) {
				mIsUriPermissionGranted = mApplication.doGrantUriPermission(mContentResolver);
			}
			while (mApplication.isRenameFileRequested()) {
				mUpdateMediaStorageFiles.clear();
				mApplication.setRenameFileRequested(false);
				executeDelay();
				renamePatternsUtilities.buildPatterns();
				populateAllListFiles();
				if (!mListFiles.isEmpty()
						&& !mApplication.isRenameFileTaskCanceled()) {
					mPreviousFileNameModelCount = 0;
					mPreviousFileRenameData = null;
					mProgressPosition = 0;
					publishProgress();
					for (FileRenameData data : mListFiles) {
						renameCurrentFile(data);
					}
					if (mProgressPosition > 0) {
						mApplication.increaseFileRenameCount(mProgressPosition);
					}
					populateAllListFiles();
				}
				doForceUpdateMediaStorage();
			}
		}
		mApplication.setRenameFileTaskRunning(false);
		return mProgressPosition;
	}

	/**
	 * Update media storage for renamed files.
	 */
	private void doForceUpdateMediaStorage() {
		if (!mUpdateMediaStorageFiles.isEmpty()) {
			String[] filesToScan = new String[mUpdateMediaStorageFiles.size()];
			filesToScan = mUpdateMediaStorageFiles.toArray(filesToScan);
			MediaScannerConnection.scanFile(
					DSCApplication.getAppContext(),
					filesToScan,
					null,
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) {
							mApplication.logD(TAG, "File " + path + " was scanned successfully: " + uri);
						}
					});
		}
	}

	/**
	 * Rename current file.
	 *
	 * @param currentFile Current file data used by the rename process.
	 */
	private void renameCurrentFile(FileRenameData currentFile) {
		String oldFileName = currentFile.getData();
		boolean skipFile;
		if (oldFileName != null) {
			File oldFile = getFile(oldFileName);
			if (oldFile != null) {
				skipFile = false;
				if (mFoldersScanning != null && mFoldersScanning.length > 0) {
					skipFile = !checkScanningFolders(oldFile);
				}
				if (!skipFile) {
					if (oldFile.canRead() && oldFile.canWrite()) {
						if (mainRenameFile(currentFile, oldFile, oldFileName)) {
							mPreviousFileRenameData = currentFile;
							mProgressPosition++;
						}
					} else {
						mApplication.logE(TAG,
								"File can not be read and write: "
										+ oldFileName);
					}
				} else {
					mApplication.logD(TAG, "Skip rename file: " + oldFileName);
				}
			} else {
				mApplication.logE(TAG, "The file:" + oldFileName
						+ " does not exist.");
			}
		} else {
			mApplication.logE(TAG, "oldFileName is null.");
		}
		publishProgress();
	}

	/**
	 * Check if the file is located on scanning folders.
	 *
	 * @param fileToCheck File to be verified if is on a scanning folder.
	 * @return TRUE if the file is on a scanning folder.
	 */
	private boolean checkScanningFolders(File fileToCheck) {
		String folderTemp;
		for (SelectedFolderModel folder : mFoldersScanning) {
			folderTemp = folder.getFullPath();
			if (fileToCheck.getAbsolutePath().startsWith(folderTemp)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Stop the thread execution.
	 */
	private void executeDelay() {
		long sec = mApplication.getRenameServiceStartDelay();
		long delayMillis = sec * 1000;
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			mApplication.logE(TAG, "InterruptedException", e);
		}
	}

	/**
	 * Runs on the UI thread before doInBackground(Params...).
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (mListener.get() != null) {
			Listener listener = mListener.get();
			if (listener != null && !listener.isFinishing()) {
				listener.onTaskStarted();
			}
		}
	}

	/**
	 * Runs on the UI thread after publishProgress(Progress...) is invoked. The
	 * specified values are the values passed to publishProgress(Progress...).
	 *
	 * @param values Not used.
	 */
	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		if (mProgressLastPosition != mProgressPosition) {
			mProgressLastPosition = mProgressPosition;
			Listener listener = mListener.get();
			if (listener != null) {
				if (listener != null && !listener.isFinishing()) {
					mProgressMessage = DSCApplication.getAppContext().getString(
							mProgressPosition == 1 ? R.string.manually_file_rename_progress_1
									: R.string.manually_file_rename_progress_more,
							mProgressPosition, mProgressMax);
					mApplication.logD(TAG, "progress position: " + mProgressPosition);
					listener.onTaskUpdate(mProgressPosition, mProgressMax, mProgressMessage);
				}
			}
		}
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...). The specified
	 * result is the value returned by doInBackground(Params...).
	 *
	 * @param count The result of the operation computed by
	 *              doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(Integer count) {
		super.onPostExecute(count);
		if (mListener != null) {
			Listener listener = mListener.get();
			if (listener != null && !listener.isFinishing()) {
				listener.onTaskFinished(count);
			}
		}
		if (mListFiles != null) {
			mListFiles.clear();
		}
		mListFiles = null;
		mApplication.setRenameFileTaskCanceled(false);
	}

	/**
	 * Rename the old file with provided new name.
	 *
	 * @param data        Original data information.
	 * @param oldFile     The old file to be renamed.
	 * @param oldFileName The old file name.
	 */
	private boolean mainRenameFile(FileRenameData data, final File oldFile, String oldFileName) {
		boolean success = false;
		String newFileName;
		File newFile;
		File parentFolder;
		boolean exist;
		do {
			newFileName = getNewFileName(data, oldFile);
			parentFolder = oldFile.getParentFile();
			newFile = new File(parentFolder, newFileName);
			exist = newFile.exists();
		} while (exist && mPreviousFileNameModelCount < 1000);
		if (!exist) {
			int id = data.getId();
			data.setFullPath(newFile.getAbsolutePath());
			data.setFileName(newFile.getName());
			success = renameFileUseApiLevel(data, oldFile, newFile);
			if (success) {
				if (data.getUri() == null && id == -1) {
					addToMediaStorageFilesList(oldFile.getAbsolutePath());
					addToMediaStorageFilesList(data.getFullPath());
					mApplication.logD(TAG, "File renamed:"
							+ oldFileName + ", " + newFileName
							+ ", media forced to update.");
				} else {
					success = updateMediaStoreData(id, data.getUri(),
							data.getFullPath(), data.getFileTitle(),
							data.getFileName());
					mApplication.logD(TAG, "File renamed: " + id + ", "
							+ oldFileName + ", " + newFileName
							+ ", media updated: " + success);
				}
				renameZeroFile(parentFolder, data);
			} else {
				mApplication.logE(TAG, "Unable to rename: " + data);
			}
		} else {
			mApplication.logE(TAG, "The file cannot be renamed: " + oldFileName
					+ " to " + newFileName);
		}
		return success;
	}

	/**
	 * Add file to be scanned by the media storage.
	 * @param fileFullPath Full file path to be scanned.
	 */
	private void addToMediaStorageFilesList(String fileFullPath) {
		if (!mUpdateMediaStorageFiles.contains(fileFullPath)) {
			mUpdateMediaStorageFiles.add(fileFullPath);
		}
	}

	/**
	 * Rename the zero file if the counter is needed.
	 *
	 * @param parentFolder Parent folder.
	 * @param data         All rename data.
	 */
	private void renameZeroFile(File parentFolder, FileRenameData data) {
		File newFile, zeroFile;
		if (mPreviousFileRenameData != null && mPreviousFileNameModelCount == 1) {
			zeroFile = new File(parentFolder, mPreviousFileRenameData.getFileName());
			newFile = new File(parentFolder, data.getFileNameZero());
			if (renameFileUseApiLevel(mPreviousFileRenameData, zeroFile, newFile)) {
				mApplication.logD(TAG, "ZERO File renamed: "
						+ mPreviousFileRenameData.getId() + ", "
						+ zeroFile.getName() + ", " + newFile.getName()
						+ ", media updated: true");
				if (mPreviousFileRenameData.getUri() == null &&
						mPreviousFileRenameData.getId() == -1) {
					addToMediaStorageFilesList(mPreviousFileRenameData.getFullPath());
					addToMediaStorageFilesList(newFile.getAbsolutePath());
				} else {
					updateMediaStoreData(mPreviousFileRenameData.getId(),
							mPreviousFileRenameData.getUri(),
							newFile.getAbsolutePath(),
							data.getFileTitleZero(),
							data.getFileNameZero());
				}
			}
		}
	}

	/**
	 * Method used to call proper rename file method base on Android API version.
	 *
	 * @param data    File rename data info.
	 * @param oldFile Old file reference.
	 * @param newFile New file reference.
	 * @return True, if the rename process succeeded.
	 */
	private boolean renameFileUseApiLevel(FileRenameData data, File oldFile, File newFile) {
		int sdkInt = mApplication.getSdkInt();
		if (sdkInt < 19) {
			return renameFileApiLevelPriorKitKat(data, oldFile, newFile);
		} else if (sdkInt < 21) {
			return renameFileApiLevelKitKat(data, oldFile, newFile);
		} else if (sdkInt >= 21) {
			return renameFileApiLevelLollipop(data, oldFile, newFile);
		}
		return false;
	}

	/**
	 * Method used rename file for versions prior KitKat Android.
	 *
	 * @param data    File rename data info.
	 * @param oldFile Old file reference.
	 * @param newFile New file reference.
	 * @return True, if the rename process succeeded.
	 */
	private boolean renameFileApiLevelPriorKitKat(FileRenameData data, File oldFile, File newFile) {
		return oldFile.renameTo(newFile);
	}

	/**
	 * Method used rename file for KitKat Android versions.
	 *
	 * @param data    File rename data info.
	 * @param oldFile Old file reference.
	 * @param newFile New file reference.
	 * @return True, if the rename process succeeded.
	 */
	@TargetApi(19)
	private boolean renameFileApiLevelKitKat(FileRenameData data, File oldFile, File newFile) {
		try {
			return oldFile.renameTo(newFile);
		} catch (Exception e) {
			mApplication.logE(TAG, "Unable to rename file using KitKat method: " + data, e);
		}
		return false;
	}

	/**
	 * Method used rename file for Lollipop Android version.
	 *
	 * @param data    File rename data info.
	 * @param oldFile Old file reference.
	 * @param newFile New file reference.
	 * @return True, if the rename process succeeded.
	 */
	@TargetApi(21)
	private boolean renameFileApiLevelLollipop(FileRenameData data, File oldFile, File newFile) {
		boolean result = false;
		try {
			String fullFilePath = oldFile.getAbsolutePath();
			if (mIsUriPermissionGranted) {
				Uri oldUri = mApplication.getDocumentUri(fullFilePath);
				Uri newUri = null;
				if (oldUri != null) {
					newUri = DocumentsContract.renameDocument(mContentResolver, oldUri, newFile.getName());
				}
				if (newUri != null) {
					result = true;
				} else {
					mApplication.logD(TAG, "Can not be renamed using new API, rename using old Java File API: " + fullFilePath);
					result = oldFile.renameTo(newFile);
				}
			} else {
				mApplication.logD(TAG, "Uri permission not granted, rename using old Java File API: " + fullFilePath);
				result = oldFile.renameTo(newFile);
			}
		} catch (Exception e) {
			mApplication.logE(TAG, "Unable to rename file using Lollipop method: " + data, e);
		}
		return result;
	}

	/**
	 * Update media store files with following data details.
	 *
	 * @param id          The file ID.
	 * @param data        The file data, normally this is the file path.
	 * @param title       The file title, usually is the file name without path and
	 *                    extension.
	 * @param displayName The file display name, usually is the file name without the
	 *                    path.
	 * @return True if the file information was stored on DB.
	 */
	private boolean updateMediaStoreData(int id, Uri uri, String data,
										 String title, String displayName) {
		boolean result = false;
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DATA, data);
		contentValues.put(MediaStore.MediaColumns.TITLE, title);
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
		try {
			int count = mContentResolver.update(uri, contentValues,
					MediaStore.MediaColumns._ID + "=?",
					new String[]{"" + id});
			result = (count == 1);
		} catch (Exception ex) {
			mApplication.logE(TAG, "Cannot be updated the content resolver: "
					+ uri.toString() + " Exception: " + ex.getMessage(), ex);
		}
		return result;
	}

	/**
	 * Rename the file provided as parameter.
	 *
	 * @param data Original data information.
	 * @param file The file to be renamed.
	 */
	private String getNewFileName(FileRenameData data, File file) {
		String oldFileName = file.getName();
		String suffix;
		int idx = oldFileName.lastIndexOf(".");
		String extension = oldFileName.substring(idx);
		oldFileName = oldFileName.substring(0, idx);
		String fileNameZero;
		long milliseconds = 0;
		switch (mApplication.getRenameFileDateType()) {
			case 1:
				milliseconds = getDateAdded(data, file);
				break;
			case 2:
				milliseconds = getDateFromExif(data, file);
				break;
			default:
				milliseconds = file.lastModified();
		}
		String newFileName = mApplication.getFileNameFormatted(data.getFileNamePatternAfter(), new Date(milliseconds));
		if (newFileName.equals(mPreviousFileNameModel)) {
			mPreviousFileNameModelCount++;
		} else {
			mPreviousFileNameModel = newFileName;
			mPreviousFileNameModelCount = 0;
		}
		if (mPreviousFileNameModelCount > 0) {
			fileNameZero = newFileName + "_" + mApplication.getFormattedFileNameSuffix(0);
			suffix = mApplication.getFormattedFileNameSuffix(mPreviousFileNameModelCount);
			if (mApplication.isAppendOriginalNameEnabled()) {
				fileNameZero += "_" + oldFileName;
			}
			newFileName += "_" + suffix;
			data.setFileTitleZero(fileNameZero);
			fileNameZero += extension;
			data.setFileNameZero(fileNameZero);
		}
		if (mApplication.isAppendOriginalNameEnabled()) {
			newFileName += "_" + oldFileName;
		}
		data.setFileTitle(newFileName);
		newFileName += extension;
		return newFileName;
	}

	/**
	 * Obtain and calculate in milliseconds the date and time from EXIF meta
	 * data.
	 *
	 * @param data Original data.
	 * @param file The file object.
	 * @return The date and time from EXIF meta data.
	 */
	private long getDateFromExif(FileRenameData data, File file) {
		String dateTimeString = null;
		long milliseconds = -1;
		String fileName = file.getAbsolutePath();
		try {
			ExifInterface exifInterface = new ExifInterface(fileName);
			dateTimeString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
			if (dateTimeString != null) {
				Date datetime = Utilities.parseExifDateTimeString(dateTimeString);
				if (datetime != null) {
					milliseconds = datetime.getTime();
				}
			}
		} catch (IOException e) {
			mApplication.logE(TAG, "IOException:" + e.getMessage() + " file:"
					+ fileName, e);
		} catch (Exception e) {
			mApplication.logE(TAG, "Exception:" + e.getMessage() + " file:"
					+ fileName + " dateTimeString:" + dateTimeString, e);
		}
		if (milliseconds == -1) {
			milliseconds = getDateAdded(data, file);
		}
		return milliseconds;
	}

	/**
	 * Obtain and calculate in milliseconds the date and time when the file was
	 * added to media storage.
	 *
	 * @param data Original data.
	 * @param file The file object.
	 * @return The date and time in milliseconds when file was added.
	 */
	private long getDateAdded(FileRenameData data, File file) {
		long milliseconds = data.getDateAdded();
		String temp = String.valueOf(milliseconds);
		if (temp.length() == 10) {
			milliseconds = milliseconds * 1000;
		} else if (temp.length() > 13) {
			temp = temp.substring(0, 13);
			try {
				milliseconds = Long.parseLong(temp);
			} catch (Exception e) {
				milliseconds = file.lastModified();
			}
		}
		return milliseconds;
	}

	/**
	 * Get the file object based on the name provided.
	 *
	 * @param fileName The file name.
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
	 * Populate the list files accordingly with user choice.
	 */
	private void populateAllListFiles() {
		if (mListFiles == null) {
			mListFiles = new ArrayList<FileRenameData>();
		} else {
			mListFiles.clear();
		}
		if (mApplication.isEnabledScanForFiles()) {
			scanForFiles();
		} else {
			scanMediaStore();
		}
		mProgressMax = mListFiles.size();
	}

	/**
	 * Directly scan for files on selected folders.
	 */
	private void scanForFiles() {
		mApplication.logD(TAG, "Scanning for the files, it is not used the media store.");
		if (mFoldersScanning.length > 0) {
			File folder;
			for (SelectedFolderModel selectedFolder : mFoldersScanning) {
				folder = new File(selectedFolder.getFullPath());
				if (folder.exists() && folder.isDirectory()) {
					recursiveFolderScan(folder.listFiles());
				}
			}
		}
	}

	/**
	 * Search recursively for files.
	 *
	 * @param files List of files and folders for current folder.
	 */
	private void recursiveFolderScan(File[] files) {
		int index;
		String fileName;
		FileNameModel fileNameModel;
		FileRenameData originalData;
		if (files != null) {
			for (File file : files) {
				if (file.exists() && !file.isHidden()) {
					if (file.isDirectory()) {
						recursiveFolderScan(file.listFiles());
					}
					if (file.isFile()) {
						fileName = file.getName();
						index = renamePatternsUtilities.matchFileNameBefore(fileName);
						if (index > -1) {
							fileNameModel = mFileNameModels[index];
							originalData = new FileRenameData(-1, null,
									file.getAbsolutePath(),
									fileName,
									fileName,
									file.lastModified());
							originalData.setFileNamePatternBefore(fileNameModel.getBefore());
							originalData.setFileNamePatternAfter(fileNameModel.getAfter());
							mListFiles.add(originalData);
						}
					}
				}
			}
		}
	}

	/**
	 * Scan for files on media storage content.
	 */
	private void scanMediaStore() {
		mApplication.logD(TAG, "Scanning for the files using media store.");
		populateListFiles(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		populateListFiles(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		if (mApplication.isRenameVideoEnabled()) {
			populateListFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
			populateListFiles(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
		}
	}

	/**
	 * Obtain a list with all files ready to be renamed.
	 *
	 * @param uri The URI, using the content:// scheme, for the content to
	 *            retrieve.
	 */
	private void populateListFiles(Uri uri) {
		Cursor cursor = null;
		String[] columns = new String[] {
				MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DATA,
				MediaStore.MediaColumns.TITLE,
				MediaStore.MediaColumns.DISPLAY_NAME,
				MediaStore.MediaColumns.DATE_ADDED
		};
		try {
			// doQuery(mContentResolver, uri);
			cursor = mContentResolver.query(uri, columns, null, null, null);
			if (cursor != null) {
				int index, id;
				long dateAdded;
				String data, title, displayName, fileName;
				FileRenameData originalData;
				FileNameModel fileNameModel;
				while (cursor.moveToNext()) {
					data = cursor.getString(cursor
							.getColumnIndex(MediaStore.MediaColumns.DATA));
					fileName = getFileName(data);
					index = renamePatternsUtilities.matchFileNameBefore(fileName);
					if (index > -1) {
						fileNameModel = mFileNameModels[index];
						id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
						title = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE));
						displayName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
						dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
						originalData = new FileRenameData(id, uri, data, title, displayName, dateAdded);
						originalData.setFileNamePatternBefore(fileNameModel.getBefore());
						originalData.setFileNamePatternAfter(fileNameModel.getAfter());
						mListFiles.add(originalData);
					}
				}
			} else {
				mApplication.logD(TAG, "Method populateListFiles cursor is null!");
			}
		} catch (Exception ex) {
			mApplication.logE(TAG, "getImageList Exception: " + ex.getMessage(), ex);
		} finally {
			Utilities.doClose(cursor);
		}
	}

	/**
	 * Extract file name based on the full file path.
	 *
	 * @param fullPath Full file path.
	 * @return File name.
	 */
	private String getFileName(String fullPath) {
		char sep = File.separatorChar;
		String fileName = fullPath;
		int idx = fileName.lastIndexOf(sep);
		if (idx > 0) {
			fileName = fullPath.substring(idx + 1);
		}
		return fileName;
	}

	/**
	 * This is an utility method used to show columns and values from a table.
	 *
	 * @param cr
	 *            The application ContentResolver
	 * @param uri
	 *            The database URI path.
	 */
	private void doQuery(ContentResolver cr, Uri uri) {
		Cursor cursor = cr.query(uri, null, null, null, null);
		mApplication.logD(TAG, "Do query for URI: " + uri);
		if (cursor != null) {
			cursor.moveToFirst();
			int rows = cursor.getCount();
			int cols = cursor.getColumnCount();
			int i, j;
			String rowVal;
			mApplication.logD(TAG, uri.getPath());
			for (i = 0; i < rows; i++) {
				rowVal = "row[" + i + "]:";
				for (j = 0; j < cols; j++) {
					if (j > 0) {
						rowVal += ", ";
					}
					try {
						rowVal += cursor.getColumnName(j) + ": "
								+ cursor.getString(j);
					} catch (Exception e) {
						mApplication.logE(TAG, "[" + j + "]:" + e.getMessage());
					}
				}
				mApplication.logD(TAG, rowVal);
				cursor.moveToNext();
			}
			if (!cursor.isClosed()) {
				cursor.close();
			}
		} else {
			mApplication.logD(TAG, "No cursor found for the URI: " + uri);
		}
	}
}
