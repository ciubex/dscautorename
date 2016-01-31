/**
 * This file is part of DSCAutoRename application.
 * <p/>
 * Copyright (C) 2016 Claudiu Ciobotariu
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.FileRenameData;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.util.RenamePatternsUtilities;
import ro.ciubex.dscautorename.util.Utilities;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.ExifInterface;
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
	private boolean isGrantUriPermissionRequested;
	private static int mProgressPosition, mProgressLastPosition, mProgressMax;
	private static String mProgressMessage;
	private FileNameModel[] mFileNameModels;
	private List<SelectedFolderModel> mSelectedFolders;
	private int mPreviousFileModelId;
	private String mPreviousFileNameModel;
	private int mPreviousFileNameModelCount;
	private boolean mIsUriPermissionGranted;
	private RenamePatternsUtilities renamePatternsUtilities;
	private static final int BUFFER = 1024;
	private boolean mNoDelay;
	List<Uri> mMediaStoreURIs;

	public interface Listener {
		public void onTaskStarted();

		public void onTaskUpdate(int position, int max, String message);

		public void onTaskFinished(int count);

		public boolean isFinishing();
	}

	public RenameFileAsyncTask(DSCApplication application) {
		this(application, null, false);
	}

	public RenameFileAsyncTask(DSCApplication application, Listener listener, boolean noDelay) {
		this.mApplication = application;
		this.mListener = new WeakReference<Listener>(listener);
		mNoDelay = noDelay;
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
		mApplication.setRenameFileTaskRunning(true);
		mFileNameModels = mApplication.getOriginalFileNamePattern();
		renamePatternsUtilities = new RenamePatternsUtilities(mApplication);
		mMediaStoreURIs = new ArrayList<>();
		mMediaStoreURIs.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		mMediaStoreURIs.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		if (mApplication.isRenameVideoEnabled()) {
			mMediaStoreURIs.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
			mMediaStoreURIs.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
		}
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
			while (mApplication.isRenameFileRequested()) {
				mApplication.setRenameFileRequested(false);
				if (!mNoDelay) {
					executeDelay();
				}
				doGrantUriPermission();
				renamePatternsUtilities.buildPatterns();
				populateAllListFiles();
				if (!mListFiles.isEmpty()
						&& !mApplication.isRenameFileTaskCanceled()) {
					mPreviousFileNameModelCount = 0;
					mPreviousFileModelId = -1;
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
			}
		}
		mApplication.setRenameFileTaskRunning(false);
		return mProgressPosition;
	}

	/**
	 * Request grant URI permissions.
	 */
	private void doGrantUriPermission() {
		if (!isGrantUriPermissionRequested) {
			isGrantUriPermissionRequested = true;
			if (mApplication.getSdkInt() >= 21) {
				prepareSelectedFolders();
				List<String> list = mApplication.doGrantUriPermission(mContentResolver, mSelectedFolders);
				mIsUriPermissionGranted = list.isEmpty(); // no rejected folder.
			}
		}
	}

	/**
	 * Prepare the list of selected folders used to grant URI permissions.
	 */
	private void prepareSelectedFolders() {
		SelectedFolderModel folderMove;
		if (mSelectedFolders == null) {
			mSelectedFolders = new ArrayList<>();
		} else {
			mSelectedFolders.clear();
		}
		for (SelectedFolderModel folder : mApplication.getSelectedFolders()) {
			if (!mSelectedFolders.contains(folder)) {
				mSelectedFolders.add(folder);
			}
		}
		for (FileNameModel fileNameModel : mFileNameModels) {
			folderMove = fileNameModel.getSelectedFolder();
			if (Utilities.isMoveFiles(folderMove)) {
				if (!mSelectedFolders.contains(folderMove)) {
					mSelectedFolders.add(folderMove);
				}
			}
		}
	}

	/**
	 * Rename current file.
	 *
	 * @param currentFileData Current file data used by the rename process.
	 */
	private void renameCurrentFile(FileRenameData currentFileData) {
		String currentFileName = currentFileData.getData();
		boolean skipFile;
		if (currentFileName != null) {
			File currentFile = getFile(currentFileName);
			if (currentFile != null) {
				skipFile = false;
				if (mFoldersScanning != null && mFoldersScanning.length > 0) {
					skipFile = !checkScanningFolders(currentFile);
				}
				if (!skipFile) {
					if (canRenameFile(currentFile)) {
						invokeRenameFile(currentFileData, currentFile, currentFileName);
					} else {
						mApplication.logE(TAG, "File can not be renamed: " + currentFileName);
					}
				} else {
					mApplication.logD(TAG, "Skip rename file: " + currentFileName);
				}
			} else {
				mApplication.logE(TAG, "The file:" + currentFileName + " does not exist.");
			}
		} else {
			mApplication.logE(TAG, "oldFileName is null.");
		}
		publishProgress();
	}

	/**
	 * Check if the file can be renamed.
	 *
	 * @param file The file to be checked.
	 * @return
	 */
	private boolean canRenameFile(File file) {
		if (file.exists()) {
			if (mApplication.getSdkInt() > 20) { // will be used new API
				return true;
			}
			return file.renameTo(file); // rename to itself
		}
		return false;
	}

	/**
	 * Try to rename the old file with provided new name.
	 *
	 * @param data        Original data information.
	 * @param file        The old file to be renamed.
	 * @param oldFileName The old file name.
	 */
	private boolean invokeRenameFile(FileRenameData data, final File file, String oldFileName) {
		boolean result = mainRenameFile(data, file, oldFileName);
		if (result) {
			mProgressPosition++;
		}
		return result;
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
		int unit = mApplication.getDelayUnit();
		long delayMillis = sec * unit * 1000;
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
			if (!Utilities.isEmpty(data.getMoveToFolderPath())) {
				parentFolder = new File(data.getMoveToFolderPath());
			} else {
				parentFolder = oldFile.getParentFile();
			}
			newFile = new File(parentFolder, newFileName);
			exist = newFile.exists();
		} while (exist && mPreviousFileNameModelCount < 1000);
		if (!exist) {
			int id = data.getId();
			data.setFullPath(newFile.getAbsolutePath());
			data.setFileName(newFile.getName());
			success = renameFileUseApiLevel(data, oldFile, newFile);
			if (success) {
				updateFileRecord(data.getUri(), id,
						data.getFullPath(), oldFileName, data.getFileTitle(),
						data.getFileName(), data.getSize());
				data.setParentFolder(newFile.getParentFile());
				renameZeroFile(data);
			} else {
				mApplication.logE(TAG, "Unable to rename: " + data);
			}
		} else {
			mApplication.logE(TAG, "The file cannot be renamed: " + oldFile.getAbsolutePath()
					+ " to " + newFile.getAbsolutePath());
		}
		return success;
	}

	/**
	 * Rename the zero file if the counter is needed.
	 *
	 * @param data All rename data.
	 */
	private void renameZeroFile(FileRenameData data) {
		File newFile, zeroFile;
		if (mPreviousFileNameModelCount == 1) {
			zeroFile = new File(data.getParentFolder(), data.getPreviousFileName());
			newFile = new File(data.getParentFolder(), data.getFileNameZero());
			if (renameFileUseApiLevel(data, zeroFile, newFile)) {
				mApplication.logD(TAG, "ZERO File renamed from "
						+ zeroFile.getName() + " to " + newFile.getName());
				updateFileRecord(data.getUri(),
						mPreviousFileModelId,
						newFile.getAbsolutePath(),
						zeroFile.getAbsolutePath(),
						data.getFileTitleZero(),
						data.getFileNameZero(),
						data.getSize());
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
		if (mustMoveFile(oldFile, newFile)) {
			if (!newFile.getParentFile().exists()) {
				newFile.getParentFile().mkdirs();
			}
		}
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
			return renameFileApiLevelPriorKitKat(data, oldFile, newFile);
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
				if (mustMoveFile(oldFile, newFile)) {
					result = doMoveFilesNewAPI(data, oldFile, newFile);
				} else {
					result = doRenameFilesNewAPI(data, oldFile, newFile);
				}
				if (!result) {
					mApplication.logD(TAG, "Can not be renamed using new API, rename using old Java File API: " + fullFilePath);
					result = renameFileApiLevelPriorKitKat(data, oldFile, newFile);
				}
			} else {
				mApplication.logD(TAG, "Uri permission not granted, rename using old Java File API: " + fullFilePath);
				result = renameFileApiLevelPriorKitKat(data, oldFile, newFile);
			}
		} catch (Exception e) {
			mApplication.logE(TAG, "Unable to rename file using Lollipop method: " + data, e);
		}
		return result;
	}

	/**
	 * Check if the file should be moved.
	 *
	 * @param oldFile The old file object.
	 * @param newFile The new file object.
	 * @return True if the new file should be moved. (parents folders are different)
	 */
	private boolean mustMoveFile(File oldFile, File newFile) {
		File parent1 = oldFile.getParentFile();
		File parent2 = newFile.getParentFile();
		String path1 = parent1.getAbsolutePath();
		String path2 = parent2.getAbsolutePath();
		boolean result = !path2.equals(path1);
		mApplication.logD(TAG, "Check mustMoveFile(" + oldFile + ", " + newFile + "): " + result);
		return result;
	}

	/**
	 * Move a file using the new API methods.
	 *
	 * @param data    File rename data info.
	 * @param oldFile Old file reference.
	 * @param newFile New file reference.
	 * @return True if the file was moved.
	 */
	@TargetApi(21)
	private boolean doRenameFilesNewAPI(FileRenameData data, File oldFile, File newFile) {
		Uri oldUri = mApplication.getDocumentUri(mSelectedFolders, oldFile.getAbsolutePath());
		Uri newUri = null;
		if (oldUri != null) {
			newUri = DocumentsContract.renameDocument(mContentResolver, oldUri, newFile.getName());
		}
		return newUri != null;
	}

	/**
	 * Move a file using the new API methods.
	 *
	 * @param data    File rename data info.
	 * @param oldFile Old file reference.
	 * @param newFile New file reference.
	 * @return True if the file was moved.
	 */
	@TargetApi(21)
	private boolean doMoveFilesNewAPI(FileRenameData data, File oldFile, File newFile) {
		Uri oldUri = mApplication.getDocumentUri(mSelectedFolders, oldFile.getAbsolutePath());
		File newParent = newFile.getParentFile();
		Uri newParentUri = mApplication.getDocumentUri(mSelectedFolders, newParent.getAbsolutePath());
		Uri newUri = DocumentsContract.createDocument(mContentResolver, newParentUri,
				data.getMimeType(), newFile.getName());
		if (newUri != null) {
			String newExtension = getFileExtension(newUri.getPath());
			String expectedExtension = getFileExtension(newFile.getName());
			if (!newExtension.equals(expectedExtension)) { // rename the file with the right extension
				newUri = DocumentsContract.renameDocument(mContentResolver, newUri, newFile.getName());
			}
		}
		boolean result = false;
		int size = 0;
		try {
			if (oldUri != null && newUri != null) {
				size = copyFileUseStream(mContentResolver.openInputStream(oldUri),
						mContentResolver.openOutputStream(newUri));

			}
			result = (size > 0);
		} catch (FileNotFoundException e) {
			mApplication.logE(TAG, "doMoveFilesNewAPI " + oldFile + " to " + newFile, e);
		}
		if (result) {
			result = DocumentsContract.deleteDocument(mContentResolver, oldUri);
		}
		return result;
	}

	/**
	 * Method used to copy a file content using stream buffering.
	 *
	 * @param inStream  The input stream.
	 * @param outStream The output stream.
	 * @return The copy size.
	 */
	private int copyFileUseStream(InputStream inStream, OutputStream outStream) {
		int size = 0;
		byte[] buffer = new byte[BUFFER];
		int length;
		try {
			while ((length = inStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, length);
				size += length;
			}
			if (size > 0) {
				outStream.flush();
			}
		} catch (IOException e) {
			size = 0;
		} finally {
			Utilities.doClose(outStream);
			Utilities.doClose(inStream);
		}
		return size;
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
		String extension = getFileExtension(oldFileName);
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
			data.setPreviousFileName(newFileName + extension);
			mPreviousFileNameModelCount++;
		} else {
			mPreviousFileNameModel = newFileName;
			mPreviousFileNameModelCount = 0;
			mPreviousFileModelId = data.getId();
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
	 * Extract the file extension from the file name.
	 *
	 * @param fileName The file name only.
	 * @return The file name extension.
	 */
	private String getFileExtension(String fileName) {
		int idx = fileName.lastIndexOf(".");
		String extension = fileName.substring(idx);
		return extension;
	}

	/**
	 * Compute the file mime type based on the file extension.
	 *
	 * @param fileName The file name only.
	 * @return The mime type based on the file extension.
	 */
	private String getFileMimeType(String fileName) {
		String extension = String.valueOf(getFileExtension(fileName)).toLowerCase();
		if (extension.startsWith("jp") || extension.startsWith("png")) {
			return "image/" + extension;
		}
		return "video/" + extension;
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
			mListFiles = new ArrayList<>();
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
									getFileMimeType(fileName),
									file.lastModified(),
									file.length());
							originalData.setFileNamePatternBefore(fileNameModel.getBefore());
							originalData.setFileNamePatternAfter(fileNameModel.getAfter());
							if (Utilities.isMoveFiles(fileNameModel.getSelectedFolder())) {
								originalData.setMoveToFolderPath(fileNameModel.getSelectedFolder().getFullPath());
							}
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
		for (Uri uri : mMediaStoreURIs) {
			populateListFiles(uri);
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
		String[] columns = new String[]{
				MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DATA,
				MediaStore.MediaColumns.TITLE,
				MediaStore.MediaColumns.DISPLAY_NAME,
				MediaStore.MediaColumns.MIME_TYPE,
				MediaStore.MediaColumns.DATE_ADDED,
				MediaStore.MediaColumns.SIZE
		};
		try {
			// doQuery(mContentResolver, uri);
			cursor = mContentResolver.query(uri, columns, null, null, null);
			if (cursor != null) {
				int index, id;
				long dateAdded;
				long size;
				String data, title, displayName, mimeType, fileName;
				FileRenameData originalData;
				FileNameModel fileNameModel;
				while (cursor.moveToNext()) {
					data = cursor.getString(cursor
							.getColumnIndex(MediaStore.MediaColumns.DATA));
					data = getValidFullFileName(data);
					if (data != null) {
						fileName = getFileName(data);
						index = renamePatternsUtilities.matchFileNameBefore(fileName);
						if (index > -1) {
							fileNameModel = mFileNameModels[index];
							id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
							title = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE));
							displayName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
							mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
							dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
							size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
							originalData = new FileRenameData(id, uri, data, title, displayName, mimeType, dateAdded, size);
							originalData.setFileNamePatternBefore(fileNameModel.getBefore());
							originalData.setFileNamePatternAfter(fileNameModel.getAfter());
							if (Utilities.isMoveFiles(fileNameModel.getSelectedFolder())) {
								originalData.setMoveToFolderPath(fileNameModel.getSelectedFolder().getFullPath());
							}
							mListFiles.add(originalData);
						}
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
	 * Validate an return full path for the provided file name.
	 *
	 * @param fullFileName Original full file name.
	 * @return Return validated file name or null.
	 */
	private String getValidFullFileName(String fullFileName) {
		File file;
		String path, wrongPath;
		if (fullFileName.contains("public:")) {
			for (MountVolume volume : mApplication.getMountedVolumes()) {
				wrongPath = volume.getWrongPath();
				if (wrongPath != null && fullFileName.contains(wrongPath)) {
					path = fullFileName.replaceAll(wrongPath, volume.getPath());
					file = new File(path);
					if (file.exists()) {
						return path;
					}
				}
			}
		} else {
			file = new File(fullFileName);
			if (file.exists()) {
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * This is an utility method used to show columns and values from a table.
	 *
	 * @param cr  The application ContentResolver
	 * @param uri The database URI path.
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

	/**
	 * Update the media store database with data file details.
	 *
	 * @param uri         The file URI.
	 * @param id          The file ID.
	 * @param data        The file data, normally this is the file path.
	 * @param title       The file title, usually is the file name without path and
	 *                    extension.
	 * @param displayName The file display name, usually is the file name without the
	 *                    path.
	 * @param size        The file size.
	 * @return True if the media store was updated.
	 */
	private boolean updateFileRecord(Uri uri, int id, String data, String oldData,
									 String title, String displayName,
									 long size) {
		String whereClause;
		String[] whereParam = new String[1];
		if (id != -1) {
			whereClause = MediaStore.MediaColumns._ID + "=?";
			whereParam[0] = "" + id;
		} else {
			whereClause = MediaStore.MediaColumns.DATA + "=?";
			whereParam[0] = oldData;
		}
		if (uri == null) {
			return updateMediaStoreData(data, title, displayName, whereClause, whereParam);
		}
		return updateMediaStoreData(uri, data, title, displayName, whereClause, whereParam);
	}

	/**
	 * Update the media store database with data file details.
	 *
	 * @param data        The file data, normally this is the file path.
	 * @param title       The file title, usually is the file name without path and
	 *                    extension.
	 * @param displayName The file display name, usually is the file name without the
	 *                    path.
	 * @param whereClause An SQL WHERE clause.
	 * @param whereParam  The SQL WHERE parameter.
	 * @return True if the media store was updated.
	 */
	private boolean updateMediaStoreData(String data, String title, String displayName,
										 String whereClause, String[] whereParam) {
		for (Uri uri : mMediaStoreURIs) {
			if (updateMediaStoreData(uri, data, title, displayName, whereClause, whereParam)) {
				return true;
			}
		}
		return false;
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
										 String whereClause, String[] whereParam) {
		boolean result = false;
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DATA, data);
		contentValues.put(MediaStore.MediaColumns.TITLE, title);
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
		try {
			int count = mContentResolver.update(uri, contentValues,
					whereClause, whereParam);
			result = (count == 1);
			mApplication.logD(TAG, "Media store update where: " + whereParam[0] + " data: " + data + " result:" + result);
		} catch (Exception ex) {
			mApplication.logE(TAG, "Cannot be updated the content resolver: "
					+ uri.toString() + " where: " + whereParam[0] + " data: " + data +
					" Exception: " + ex.getMessage(), ex);
			if (ex instanceof android.database.sqlite.SQLiteConstraintException) {
				deleteWrongRecordMediaStoreData(uri, whereClause, whereParam);
			}
		}
		return result;
	}

	/**
	 * Method used to remove wrong media store record.
	 *
	 * @param uri         Wrong media store URI root.
	 * @param whereClause An SQL WHERE clause.
	 * @param whereParam  The SQL WHERE parameter.
	 */
	private void deleteWrongRecordMediaStoreData(Uri uri, String whereClause, String[] whereParam) {
		try {
			int count = mContentResolver.delete(uri, whereClause, whereParam);
			mApplication.logD(TAG, "Media store delete where: " + whereParam[0] + " count:" + count);
		} catch (Exception ex) {
			mApplication.logE(TAG, "Cannot be deleted the wrong record: "
					+ whereParam[0] + " Exception: " + ex.getMessage(), ex);
		}
	}
}
