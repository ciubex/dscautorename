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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FilePrefix;
import ro.ciubex.dscautorename.model.FileRenameData;
import ro.ciubex.dscautorename.model.FolderItem;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

/**
 * An AsyncTask used to rename a file.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class RenameFileAsyncTask extends AsyncTask<Void, Void, Integer> {
	private final static String TAG = RenameFileAsyncTask.class.getName();
	private DSCApplication mApplication;
	private ContentResolver mContentResolver;
	private final WeakReference<Listener> mListener;
	private List<FileRenameData> mListFiles;
	private FolderItem[] mFoldersScanning;
	private int mPosition, mCount;
	private Locale mLocale;
	private FilePrefix[] mFilesPrefixes;
	private Pattern[] mPatterns;
	private static SimpleDateFormat sFormatter;
	private static ParsePosition position = new ParsePosition(0);
	private String mFinishedMessage;

	public interface Listener {
		public void onTaskStarted();

		public void onTaskUpdate(int position, int count);

		public void onTaskFinished(int count);

		public boolean isFinishing();
	}

	public RenameFileAsyncTask(DSCApplication application) {
		this(application, null);
	}

	public RenameFileAsyncTask(DSCApplication application, Listener listener) {
		this.mApplication = application;
		this.mListener = new WeakReference<Listener>(listener);
		mLocale = mApplication.getLocale();
		sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", mLocale);
		sFormatter.setTimeZone(TimeZone.getDefault());
		mApplication.setRenameFileTaskRunning(true);
		mFilesPrefixes = mApplication.getOriginalFilePrefix();
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
		mPosition = 0;
		boolean enableFilter;
		if (mContentResolver != null) {
			enableFilter = mApplication.isEnabledFolderScanning();
			if (enableFilter) {
				mFoldersScanning = mApplication.getFoldersScanning();
			}
			while (mApplication.isRenameFileRequested()) {
				mApplication.setRenameFileRequested(false);
				mFinishedMessage = DSCApplication.SUCCESS;
				executeDelay();
				buildPatterns();
				populateAllListFiles();
				if (!mListFiles.isEmpty()
						&& !mApplication.isRenameFileTaskCanceled()) {
					mPosition = 0;
					publishProgress();
					for (FileRenameData data : mListFiles) {
						renameCurrentFile(data);
					}
					if (mPosition > 0) {
						mApplication.increaseFileRenameCount(mPosition);
					}
					populateAllListFiles();
				}
				mApplication.setLastRenameFinishMessage(mFinishedMessage);
			}
		}
		mApplication.setRenameFileTaskRunning(false);
		return mPosition;
	}

	/**
	 * Rename current file.
	 * 
	 * @param currentFile
	 *            Current file data used by the rename process.
	 */
	private void renameCurrentFile(FileRenameData currentFile) {
		String oldFileName = currentFile.getData();
		boolean skipFile;
		if (oldFileName != null) {
			File oldFile = getFile(oldFileName);
			if (oldFile != null) {
				skipFile = false;
				if (mFoldersScanning != null) {
					skipFile = !checkScanningFolders(oldFile);
				}
				if (!skipFile) {
					if (oldFile.canRead() && oldFile.canWrite()) {
						if (renameFile(currentFile, oldFile, oldFileName)) {
							mPosition++;
						}
					} else {
						mFinishedMessage = mApplication
								.getString(R.string.error_rename_file_no_read_write);
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
	 * @param fileToCheck
	 *            File to be verified if is on a scanning folder.
	 * @return TRUE if the file is on a scanning folder.
	 */
	private boolean checkScanningFolders(File fileToCheck) {
		for (FolderItem folder : mFoldersScanning) {
			if (fileToCheck.getAbsolutePath().startsWith(folder.toString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Prepare prefix patterns.
	 */
	private void buildPatterns() {
		int i, len = mFilesPrefixes.length, lst;
		mPatterns = new Pattern[len];
		Pattern pattern;
		FilePrefix filePrefix;
		String before;
		for (i = 0; i < len; i++) {
			filePrefix = mFilesPrefixes[i];
			before = filePrefix.getBefore().toLowerCase(mLocale);
			lst = before.length();
			if (lst == 0) {
				before = "*";
			} else if (lst > 0) {
				if (before.charAt(lst - 1) != '*') {
					before += "*";
				}
			}
			pattern = Pattern.compile(wildcardToRegex(before));
			mPatterns[i] = pattern;
		}
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
	 * @param values
	 *            Not used.
	 */
	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		if (mListener.get() != null) {
			Listener listener = mListener.get();
			if (listener != null && !listener.isFinishing()) {
				listener.onTaskUpdate(mPosition, mCount);
			}
		}
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...). The specified
	 * result is the value returned by doInBackground(Params...).
	 * 
	 * @param count
	 *            The result of the operation computed by
	 *            doInBackground(Params...).
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
	 * @param data
	 *            Original data information.
	 * @param oldFile
	 *            The old file to be renamed.
	 * @param oldFileName
	 *            The old file name.
	 */
	private boolean renameFile(FileRenameData data, File oldFile,
			String oldFileName) {
		boolean success = false;
		int index = 0;
		String newFileName;
		File newFile;
		boolean exist = false;
		do {
			newFileName = getNewFileName(data, oldFile, index);
			newFile = new File(oldFile.getParentFile(), newFileName);
			index++;
			exist = newFile.exists();
		} while (exist && index < 1000);
		if (!exist) {
			int id = data.getId();
			success = oldFile.renameTo(newFile);
			if (success) {
				success = setNewFileToMediaStoreData(id, data.getUri(), newFile);
				mApplication.logD(TAG, "File renamed: " + id + ", "
						+ oldFileName + ", " + newFileName
						+ ", media updated: " + success);
			} else {
				mFinishedMessage = mApplication.getString(
						R.string.error_rename_file, oldFileName);
				mApplication.logE(TAG, "The file " + oldFileName + " (id:" + id
						+ ") cannot be renamed!");
			}
		} else {
			mApplication.logE(TAG, "The file cannot be renamed: " + oldFileName
					+ " to " + newFileName);
		}
		return success;
	}

	/**
	 * Update the Media Store data with the new file name.
	 * 
	 * @param id
	 *            The ID of the file to be updated
	 * @param oldFileName
	 *            The old file name.
	 * @param newFileName
	 *            The new file name.
	 * @return True if the file name was updated.
	 */
	private boolean setNewFileToMediaStoreData(int id, Uri uri, File newFile) {
		boolean result = updateMediaStoreData(id, uri,
				newFile.getAbsolutePath(),
				removeExtensionFileName(newFile.getName()), newFile.getName());
		return result;
	}

	/**
	 * Update media store files with following data details.
	 * 
	 * @param id
	 *            The file ID.
	 * @param data
	 *            The file data, normally this is the file path.
	 * @param title
	 *            The file title, usually is the file name without path and
	 *            extension.
	 * @param displayName
	 *            The file display name, usually is the file name without the
	 *            path.
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
					new String[] { "" + id });
			result = (count == 1);
		} catch (Exception ex) {
			mApplication.logE(TAG, "Cannot be updated the content resolver: "
					+ uri.toString() + " Exception: " + ex.getMessage(), ex);
		}
		return result;
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
	 * @param data
	 *            Original data information.
	 * @param file
	 *            The file to be renamed.
	 * @param index
	 *            The index used to generate a unique file name if the file is
	 *            already exist.
	 */
	private String getNewFileName(FileRenameData data, File file, int index) {
		String oldFileName = file.getName();
		String prefix = data.getPrefixAfter();
		String sufix;
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
		String newFileName = prefix
				+ mApplication.getFileName(new Date(milliseconds));
		if (index > 0) {
			sufix = String.format(mLocale, "%05d", index);
			newFileName += "_" + sufix;
		}
		newFileName += getFileExtension(oldFileName);
		return newFileName;
	}

	/**
	 * Obtain and calculate in milliseconds the date and time from EXIF meta
	 * data.
	 * 
	 * @param data
	 *            Original data.
	 * @param file
	 *            The file object.
	 * @return The date and time from EXIF meta data.
	 */
	private long getDateFromExif(FileRenameData data, File file) {
		String dateTimeString = null;
		long milliseconds = -1;
		String fileName = file.getAbsolutePath();
		try {
			ExifInterface exifInterface = new ExifInterface(fileName);
			dateTimeString = exifInterface
					.getAttribute(ExifInterface.TAG_DATETIME);
			if (dateTimeString != null) {
				Date datetime = sFormatter.parse(dateTimeString, position);
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
	 * @param data
	 *            Original data.
	 * @param file
	 *            The file object.
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
	 * Populate the list files accordingly with user choice.
	 */
	private void populateAllListFiles() {
		if (mListFiles == null) {
			mListFiles = new ArrayList<FileRenameData>();
		} else {
			mListFiles.clear();
		}
		populateListFiles(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		populateListFiles(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		if (mApplication.isRenameVideoEnabled()) {
			populateListFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
			populateListFiles(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
		}
		mCount = mListFiles.size();
	}

	/**
	 * Obtain a list with all files ready to be renamed.
	 * 
	 * @param uri
	 *            The URI, using the content:// scheme, for the content to
	 *            retrieve.
	 */
	private void populateListFiles(Uri uri) {
		Cursor cursor = null;
		String[] columns = new String[] { MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE,
				MediaStore.MediaColumns.DISPLAY_NAME,
				MediaStore.MediaColumns.DATE_ADDED };
		try {
			// doQuery(mContentResolver, uri);
			cursor = mContentResolver.query(uri, columns, null, null, null);
			if (cursor != null) {
				int index, id;
				long dateAdded;
				String data, title, displayName, fileName;
				FileRenameData originalData;
				FilePrefix filePrefix;
				while (cursor.moveToNext()) {
					data = cursor.getString(cursor
							.getColumnIndex(MediaStore.MediaColumns.DATA));
					fileName = getFileName(data);
					index = matchFileNamePrefix(fileName);
					if (index > -1) {
						filePrefix = mFilesPrefixes[index];
						id = cursor.getInt(cursor
								.getColumnIndex(MediaStore.MediaColumns._ID));
						title = cursor.getString(cursor
								.getColumnIndex(MediaStore.MediaColumns.TITLE));
						displayName = cursor
								.getString(cursor
										.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
						dateAdded = cursor
								.getLong(cursor
										.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
						originalData = new FileRenameData(id, uri, data, title,
								displayName, dateAdded);
						originalData.setPrefixBefore(filePrefix.getBefore());
						originalData.setPrefixAfter(filePrefix.getAfter());
						mListFiles.add(originalData);
					}
				}
			}
		} catch (Exception ex) {
			mApplication.logE(TAG, "getImageList Exception: " + ex.getMessage(), ex);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	/**
	 * Extract file name based on the full file path.
	 * 
	 * @param fullPath
	 *            Full file path.
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
	 * Look on the path and check with existing file name matches.
	 * 
	 * @param fileName
	 *            Path to be checked.
	 * @return -1 if the path is not matching with a prefix otherwise is
	 *         returned prefix index.
	 */
	private int matchFileNamePrefix(String fileName) {
		String lower = fileName.toLowerCase(mLocale);
		int i, len = mPatterns.length;
		Pattern pattern;
		for (i = 0; i < len; i++) {
			pattern = mPatterns[i];
			if (pattern.matcher(lower).matches()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Convert wildcard to a regex expression.
	 * 
	 * @param wildcard
	 *            Wildcard expression to convert.
	 * @return Converted expression.
	 */
	private String wildcardToRegex(String wildcard) {
		StringBuffer s = new StringBuffer(wildcard.length());
		s.append('^');
		for (int i = 0, is = wildcard.length(); i < is; i++) {
			char c = wildcard.charAt(i);
			switch (c) {
			case '*':
				s.append(".*");
				break;
			case '?':
				s.append(".");
				break;
			// escape special regexp-characters
			case '(':
			case ')':
			case '[':
			case ']':
			case '$':
			case '^':
			case '.':
			case '{':
			case '}':
			case '|':
			case '\\':
				s.append("\\");
				s.append(c);
				break;
			default:
				s.append(c);
				break;
			}
		}
		s.append('$');
		return (s.toString());
	}
}
