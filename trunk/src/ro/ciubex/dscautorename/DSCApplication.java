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
package ro.ciubex.dscautorename;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ro.ciubex.dscautorename.activity.RenameShortcutUpdateListener;
import ro.ciubex.dscautorename.model.FilePrefix;
import ro.ciubex.dscautorename.model.FolderItem;
import ro.ciubex.dscautorename.receiver.MediaStorageObserverService;
import ro.ciubex.dscautorename.task.LogThread;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This is the application class for the DSC Auto Rename application.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class DSCApplication extends Application {
	private static final String TAG = DSCApplication.class.getName();
	private Locale mLocale;
	private ProgressDialog mProgressDialog;
	private SharedPreferences mSharedPreferences;
	private RenameShortcutUpdateListener mShortcutUpdateListener;
	private static int mVersionCode = -1;
	private static boolean mRenameFileRequested;
	private static boolean mRenameFileTaskCanceled;
	private static boolean mRenameFileTaskRunning;
	public static final int SERVICE_TYPE_DISABLED = 0;
	public static final int SERVICE_TYPE_CAMERA = 1;
	public static final int SERVICE_TYPE_CONTENT = 2;

	public static final String SUCCESS = "success";
	public static final String LOG_FILE_NAME = "DSC_logs.log";
	private static File logFile;
	private static LogThread logFileThread;
	private static SimpleDateFormat sFormatter;

	private static final String KEY_FOLDER_SCANNING = "folderScanning";
	private static final String KEY_ENABLED_FOLDER_SCANNING = "enabledFolderScanning";
	private static final String KEY_RENAME_SHORTCUT_CREATED = "renameShortcutCreated";
	private static final String KEY_RENAME_SERVICE_START_CONFIRMATION = "hideRenameServiceStartConfirmation";
	private static final String KEY_FILE_NAME_FORMAT = "fileNameFormat";
	private static final String KEY_RENAME_VIDEO_ENABLED = "renameVideoEnabled";
	private static final String KEY_ORIGINAL_FILE_PREFIX = "originalFilePrefix";
	private static final String KEY_FILE_RENAME_COUNT = "fileRenameCount";
	private static final String KEY_RENAME_SERVICE_START_DELAY = "renameServiceStartDelay";
	private static final String KEY_REGISTERED_SERVICE_TYPE = "registeredServiceType";
	private static final String KEY_RENAME_FILE_DATE_TYPE = "renameFileDateType";
	private static final String KEY_LAST_RENAME_FINISH_MESSAGE = "lastRenameFinishMessage";

	public interface ProgressCancelListener {
		public void onProgressCancel();
	}

	/**
	 * Called when the application is starting, before any activity, service, or
	 * receiver objects (excluding content providers) have been created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mLocale = Locale.getDefault();
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		checkRegisteredServiceType(true);
	}

	/**
	 * Check if is enabled folder scanning.
	 * 
	 * @return True if is enabled folder scanning.
	 */
	public boolean isEnabledFolderScanning() {
		return mSharedPreferences
				.getBoolean(KEY_ENABLED_FOLDER_SCANNING, false);
	}

	/**
	 * Check if the rename shortcut is created on home screen.
	 * 
	 * @return True if the shortcut for rename service is created on home
	 *         screen.
	 */
	public boolean isRenameShortcutCreated() {
		return mSharedPreferences
				.getBoolean(KEY_RENAME_SHORTCUT_CREATED, false);
	}

	/**
	 * Set the boolean value of created shortcut for rename service.
	 * 
	 * @param flag
	 *            True or False.
	 */
	public void setRenameShortcutCreated(boolean flag) {
		saveBooleanValue(KEY_RENAME_SHORTCUT_CREATED, flag);
	}

	/**
	 * Obtain the confirmation flag for the rename service start.
	 * 
	 * @return True if the confirmation of rename service start should be
	 *         hidden.
	 */
	public boolean hideRenameServiceStartConfirmation() {
		return mSharedPreferences.getBoolean(
				KEY_RENAME_SERVICE_START_CONFIRMATION, false);
	}

	/**
	 * Get the folder used for scanning files.
	 * 
	 * @return The folder user for scanning files.
	 */
	private String getFolderScanning() {
		String folders = mSharedPreferences.getString(KEY_FOLDER_SCANNING, "");
		if (folders.length() < 2) {
			folders = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/DCIM";
			setFoldersScanning(folders);
		}
		return folders;
	}

	/**
	 * Obtain all folders where the file should be renamed.
	 * 
	 * @return An array with folders restricted for renaming.
	 */
	public FolderItem[] getFoldersScanning() {
		String stored = getFolderScanning();
		String[] folders = stored.split(",");
		FolderItem[] items = new FolderItem[folders.length];
		for (int i = 0; i < folders.length; i++) {
			items[i] = new FolderItem(folders[i]);
		}
		return items;
	}

	/**
	 * Set folders used for scanning files.
	 * 
	 * @param folders
	 *            The folders names to save.
	 */
	public void setFoldersScanning(String folders) {
		saveStringValue(KEY_FOLDER_SCANNING, folders);
	}

	/**
	 * Set or add a folder used on renaming process.
	 * 
	 * @param index
	 *            Index of the folder on the list. If is -1 the the folder is
	 *            added.
	 * @param folder
	 *            Folder to be stored on the scanning folder
	 */
	public void setFolderScanning(int index, String folder) {
		FolderItem[] folders = getFoldersScanning();
		int i = 0, len = folders.length;
		StringBuilder buffer = new StringBuilder();
		while (i < len) {
			if (i > 0) {
				buffer.append(',');
			}
			if (i == index) {
				buffer.append(folder);
			} else {
				buffer.append(folders[i]);
			}
			i++;
		}
		if (index == -1) {
			buffer.append(',').append(folder);
		}
		saveStringValue(KEY_FOLDER_SCANNING, buffer.toString());
	}

	/**
	 * Remove a scanning folder.
	 * 
	 * @param index
	 *            Index of removed scanning folder.
	 */
	public void removeFolderScanning(int index) {
		FolderItem[] folders = getFoldersScanning();
		int i = 0, len = folders.length;
		StringBuilder buffer = new StringBuilder();
		while (i < len) {
			if (buffer.length() > 0) {
				buffer.append(',');
			}
			if (i != index) {
				buffer.append(folders[i]);
			}
			i++;
		}
		saveStringValue(KEY_FOLDER_SCANNING, buffer.toString());
	}

	/**
	 * Obtain the application locale.
	 * 
	 * @return The locale of the application.
	 */
	public Locale getLocale() {
		return mLocale;
	}

	/**
	 * Obtain the formatting template for the file name.
	 * 
	 * @return The file name format.
	 */
	public String getFileNameFormat() {
		return mSharedPreferences.getString(KEY_FILE_NAME_FORMAT,
				getString(R.string.file_name_format));
	}

	/**
	 * Obtain the formated file name based on a date.
	 * 
	 * @param date
	 *            Date used to generate file name.
	 * @return The formated file name.
	 */
	public String getFileName(Date date) {
		DateFormat df = null;
		String fileNameFormat = getFileNameFormat();
		try {
			df = new SimpleDateFormat(fileNameFormat, mLocale);
		} catch (Exception e) {
			fileNameFormat = getString(R.string.file_name_format);
			df = new SimpleDateFormat(fileNameFormat, mLocale);
			saveStringValue(KEY_FILE_NAME_FORMAT, fileNameFormat);
			logE(TAG, "getFileName: " + date, e);
		}
		String newFileName = df.format(date);
		return newFileName;
	}

	/**
	 * Obtain a demo extension based on provided file name prefix.
	 * 
	 * @param fileNamePrefix
	 *            Provided file name prefix.
	 * @return A demo extension.
	 */
	public String getDemoExtension(String fileNamePrefix) {
		String ext = ".JPG";
		if (fileNamePrefix != null && fileNamePrefix.indexOf("DSC") < 0) {
			ext = ".MP4";
		}
		return ext;
	}

	/**
	 * Check if the service is enabled.
	 * 
	 * @return True if the service is enabled.
	 */
	public boolean isAutoRenameEnabled() {
		return SERVICE_TYPE_DISABLED != getServiceType();
	}

	/**
	 * Check if the video files should renamed too.
	 * 
	 * @return True if the video files should be renamed.
	 */
	public boolean isRenameVideoEnabled() {
		return mSharedPreferences.getBoolean(KEY_RENAME_VIDEO_ENABLED, true);
	}

	/**
	 * Obtain the original file name prefix.
	 * 
	 * @return The original file name prefix.
	 */
	public FilePrefix[] getOriginalFilePrefix() {
		String value = mSharedPreferences.getString(KEY_ORIGINAL_FILE_PREFIX,
				getString(R.string.original_file_prefix));
		if (value.length() < 1) {
			value = getString(R.string.original_file_prefix);
			saveStringValue(KEY_ORIGINAL_FILE_PREFIX, value);
		}
		String[] arr = value.split(",");
		FilePrefix[] fp = new FilePrefix[arr.length];
		for (int i = 0; i < arr.length; i++) {
			fp[i] = new FilePrefix(arr[i]);
		}
		return fp;
	}

	/**
	 * Update a file prefix.
	 * 
	 * @param filePrefix
	 *            File prefix to be updated.
	 * @param position
	 *            Position of updated file prefix.
	 */
	public void saveFilePrefix(FilePrefix filePrefix, int position) {
		FilePrefix[] arr = getOriginalFilePrefix();
		FilePrefix fp;
		int index;
		int len = arr.length;
		StringBuilder sb = new StringBuilder();
		for (index = 0; index < len; index++) {
			if (position == index) {
				fp = filePrefix;
			} else {
				fp = arr[index];
			}
			if (index > 0) {
				sb.append(',');
			}
			sb.append(fp.toString());
		}
		if (position == -1) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(filePrefix.toString());
		}
		if (sb.length() < 1) {
			sb.append(getString(R.string.original_file_prefix));
		}
		saveFilePrefix(sb.toString());
	}

	/**
	 * Save the files prefixes on the shared preferences..
	 * 
	 * @param filePrefixes
	 *            The file prefixes to be saved.
	 */
	public void saveFilePrefix(String filePrefixes) {
		saveStringValue(KEY_ORIGINAL_FILE_PREFIX, filePrefixes);
	}

	/**
	 * Check if rename file is requested.
	 * 
	 * @return the renameFileRequested
	 */
	public boolean isRenameFileRequested() {
		return DSCApplication.mRenameFileRequested;
	}

	/**
	 * Set renameFileRequest flag.
	 * 
	 * @param renameFileRequested
	 *            the renameFileRequested to set
	 */
	public void setRenameFileRequested(boolean renameFileRequested) {
		DSCApplication.mRenameFileRequested = renameFileRequested;
	}

	/**
	 * Obtain the rename file task cancel boolean value.
	 * 
	 * @return The rename file task cancel boolean value.
	 */
	public boolean isRenameFileTaskCanceled() {
		return DSCApplication.mRenameFileTaskCanceled;
	}

	/**
	 * Set the rename file task cancel flag.
	 * 
	 * @param flag
	 *            The rename file task cancel boolean value.
	 */
	public void setRenameFileTaskCanceled(boolean flag) {
		DSCApplication.mRenameFileTaskCanceled = flag;
	}

	/**
	 * Check if rename file task is running.
	 * 
	 * @return the renameFileTaskRunning
	 */
	public boolean isRenameFileTaskRunning() {
		return DSCApplication.mRenameFileTaskRunning;
	}

	/**
	 * Set rename file task is running flag.
	 * 
	 * @param renameFileTaskRunning
	 *            the renameFileTaskRunning to set
	 */
	public void setRenameFileTaskRunning(boolean renameFileTaskRunning) {
		DSCApplication.mRenameFileTaskRunning = renameFileTaskRunning;
	}

	/**
	 * Obtain the number of renamed files.
	 * 
	 * @return Number of renamed files.
	 */
	public int getFileRenameCount() {
		return mSharedPreferences.getInt(KEY_FILE_RENAME_COUNT, 0);
	}

	/**
	 * Increase the rename file counter.
	 * 
	 * @param value
	 *            The integer value to be increased the rename file counter.
	 */
	public void increaseFileRenameCount(int value) {
		if (value == -1) {
			removeSharedPreference(KEY_FILE_RENAME_COUNT);
		} else {
			int oldValue = getFileRenameCount();
			saveIntegerValue(KEY_FILE_RENAME_COUNT, oldValue + value);
		}
	}

	/**
	 * Obtain the delay for starting the rename service.
	 * 
	 * @return The delay for starting the rename service.
	 */
	public int getRenameServiceStartDelay() {
		return mSharedPreferences.getInt(KEY_RENAME_SERVICE_START_DELAY, 3);
	}

	/**
	 * Obtain the selected service type.
	 * 
	 * @return Disabled = 0, camera = 1 or content = 2.
	 */
	public int getServiceType() {
		return getIntValue("serviceType", 1);
	}

	/**
	 * Obtain a valid integer value from shared preference.
	 * 
	 * @param key
	 *            The key.
	 * @param defaultValue
	 *            The default value.
	 * @return Value of the key.
	 */
	private int getIntValue(String key, int defaultValue) {
		String strValue = mSharedPreferences.getString(key, "" + defaultValue);
		int value = defaultValue;
		try {
			value = Integer.parseInt(strValue);
		} catch (NumberFormatException e) {
			logE(TAG, "getIntValue(" + key + "): " + strValue, e);
		}
		return value;
	}

	/**
	 * Obtain the registered service type.
	 * 
	 * @return The registered service type: disabled = 0, camera = 1 or content
	 *         = 2. -1 is returning for the first time.
	 */
	public int getRegisteredServiceType() {
		return mSharedPreferences.getInt(KEY_REGISTERED_SERVICE_TYPE, -1);
	}

	/**
	 * Save the registered service type value.
	 * 
	 * @param value
	 *            The registered service type value.
	 */
	public void setRegisteredServiceType(int value) {
		saveIntegerValue(KEY_REGISTERED_SERVICE_TYPE, value);
	}

	/**
	 * Check if the registered service type was changed.
	 * 
	 * @return True if the registered service type was changed.
	 */
	public boolean checkRegisteredServiceType(boolean force) {
		int serviceType = getServiceType();
		int regServiceType = getRegisteredServiceType();
		boolean changed = force || (serviceType != regServiceType);
		if (changed) {
			updateRegisteredServiceType(serviceType, regServiceType);
		}
		return changed;
	}

	/**
	 * Update registered service type according with specified service type.
	 * 
	 * @param serviceType
	 *            The specified service type to be registered.
	 * @param regServiceType
	 *            The current service type registered.
	 */
	private void updateRegisteredServiceType(int serviceType, int regServiceType) {
		if (SERVICE_TYPE_CONTENT == regServiceType) {
			unregisterMediaStorageContentObserver();
		}
		if (SERVICE_TYPE_CONTENT == serviceType) {
			registerMediaStorageContentObserver();
		}
		saveIntegerValue(KEY_REGISTERED_SERVICE_TYPE, serviceType);
	}

	/**
	 * Obtain which date should be used to rename
	 * 
	 * @return
	 */
	public int getRenameFileDateType() {
		return getIntValue(KEY_RENAME_FILE_DATE_TYPE, 1);
	}

	/**
	 * Method used to dynamically register a content observer service used to
	 * launch automatically rename service.
	 */
	private void registerMediaStorageContentObserver() {
		try {
			startService(new Intent(this, MediaStorageObserverService.class));
		} catch (Exception e) {
			logE(TAG, "registerMediaStorageContentObserver: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Method used to unregister the content observer service.
	 */
	private void unregisterMediaStorageContentObserver() {
		try {
			stopService(new Intent(this, MediaStorageObserverService.class));
		} catch (Exception e) {
			logE(TAG,
					"unregisterMediaStorageContentObserver: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Store a string value on the shared preferences.
	 * 
	 * @param key
	 *            The shared preference key.
	 * @param value
	 *            The string value to be saved.
	 */
	private void saveStringValue(String key, String value) {
		Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	/**
	 * Store a boolean value on the shared preferences.
	 * 
	 * @param key
	 *            The shared preference key.
	 * @param value
	 *            The boolean value to be saved.
	 */
	private void saveBooleanValue(String key, boolean value) {
		Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	/**
	 * Store an integer value on the shared preferences.
	 * 
	 * @param key
	 *            The shared preference key.
	 * @param value
	 *            The integer value to be saved.
	 */
	private void saveIntegerValue(String key, int value) {
		Editor editor = mSharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	/**
	 * Remove a shared preference.
	 * 
	 * @param key
	 *            The key of the shared preference to be removed.
	 */
	private void removeSharedPreference(String key) {
		Editor editor = mSharedPreferences.edit();
		editor.remove(key);
		editor.commit();
	}

	/**
	 * This will show a progress dialog using a context and the message to be
	 * showed on the progress dialog.
	 * 
	 * @param listener
	 *            The listener class which should listen for cancel.
	 * @param context
	 *            The context where should be displayed the progress dialog.
	 * @param message
	 *            The message displayed inside of progress dialog.
	 */
	public void showProgressDialog(final ProgressCancelListener listener,
			Context context, String message, int max) {
		hideProgressDialog();
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setTitle(R.string.please_wait);
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener != null) {
							listener.onProgressCancel();
						}
					}
				});
		if (max > 0) {
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(max);
		}
		if (!mProgressDialog.isShowing()) {
			mProgressDialog.show();
		}
	}

	/**
	 * Hide the progress dialog.
	 */
	public void hideProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
	}

	/**
	 * Set the message on the progress dialog.
	 * 
	 * @param message
	 *            The message to be set.
	 */
	public void setProgressDialogMessage(String message) {
		if (mProgressDialog != null) {
			mProgressDialog.setMessage(message);
		}
	}

	/**
	 * Set the progress position for the progress dialog.
	 * 
	 * @param position
	 *            The progress position.
	 */
	public void setProgressDialogProgress(int position) {
		if (mProgressDialog != null) {
			mProgressDialog.setProgress(position);
		}
	}

	/**
	 * Check for pro version.
	 * 
	 * @return True if pro version exist.
	 */
	public boolean isProPresent() {
		PackageManager pm = getPackageManager();
		boolean success = false;
		try {
			success = (PackageManager.SIGNATURE_MATCH == pm.checkSignatures(
					this.getPackageName(), "ro.ciubex.dscautorenamepro"));
			logD(TAG, "isProPresent: " + success);
		} catch (Exception e) {
			logE(TAG, "isProPresent: " + e.getMessage(), e);
		}
		return success;
	}

	/**
	 * Set the rename shortcut update listener.
	 * 
	 * @param listener
	 *            The rename shortcut update listener.
	 */
	public void updateShortcutUpdateListener(
			RenameShortcutUpdateListener listener) {
		this.mShortcutUpdateListener = listener;
	}

	/**
	 * Get the rename shortcut update listener.
	 * 
	 * @return The rename shortcut update listener.
	 */
	public RenameShortcutUpdateListener getShortcutUpdateListener() {
		return mShortcutUpdateListener;
	}

	/**
	 * Method invoked by the shortcut broadcast.
	 * 
	 * @param data
	 *            Intent data from the shortcut broadcast.
	 * @param type
	 *            Type of the event, uninstall or install.
	 */
	public void prepareShortcutPref(Intent data,
			RenameShortcutUpdateListener.TYPE type) {
		Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		if (intent != null && name != null && intent.getComponent() != null) {
			String cls = String.valueOf(intent.getComponent().getClassName());
			if (cls.indexOf("RenameDlgActivity") > 0) {
				updateShortcutPref(type);
			}
		}
	}

	/**
	 * Update the preferences related with the shortcut.
	 * 
	 * @param type
	 *            Type of the event, uninstall or install.
	 */
	private void updateShortcutPref(RenameShortcutUpdateListener.TYPE type) {
		RenameShortcutUpdateListener listener = getShortcutUpdateListener();
		boolean update = false;
		if (RenameShortcutUpdateListener.TYPE.INSTALL == type) {
			setRenameShortcutCreated(true);
			update = true;
		} else if (RenameShortcutUpdateListener.TYPE.UNINSTALL == type) {
			setRenameShortcutCreated(false);
			update = true;
		}
		if (listener != null && update) {
			listener.updateRenameShortcut();
		}
	}

	/**
	 * Save the rename process message.
	 * 
	 * @param message
	 *            Message to be saved.
	 */
	public void setLastRenameFinishMessage(String message) {
		saveStringValue(KEY_LAST_RENAME_FINISH_MESSAGE, message);
	}

	/**
	 * Get the last rename process message.
	 * 
	 * @return Last rename process message.
	 */
	public String getLastRenameFinishMessage() {
		return mSharedPreferences.getString(KEY_LAST_RENAME_FINISH_MESSAGE,
				SUCCESS);
	}

	/**
	 * Check if the Android version is KitKat or newer.
	 * 
	 * @return True if the system is KitKat or newer.
	 */
	public boolean isKitKatOrNewer() {
		// 18 is Android 4.3 - Jelly Bean
		if (android.os.Build.VERSION.SDK_INT > 18) {
			return true;
		}
		return false;
	}

	/**
	 * Send a {@link #ERROR} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public void logE(String tag, String msg) {
		Log.e(tag, msg);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg);
	}

	/**
	 * Send a {@link #ERROR} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 *            An exception to log
	 */
	public void logE(String tag, String msg, Throwable tr) {
		Log.e(tag, msg, tr);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg
				+ "\t" + Log.getStackTraceString(tr));
	}

	/**
	 * Send a {@link #DEBUG} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public void logD(String tag, String msg) {
		Log.d(tag, msg);
		writeLogFile(System.currentTimeMillis(), "DEBUG\t" + tag + "\t" + msg);
	}

	/**
	 * Write the log message to the app log file.
	 * 
	 * @param logmessage
	 *            The log message.
	 */
	private void writeLogFile(long milliseconds, String logmessage) {
		if (checkLogFileThread()) {
			logFileThread.addLog(sFormatter.format(new Date(milliseconds))
					+ "\t" + logmessage);
		}
	}

	/**
	 * Check if log file thread exist and create it if not.
	 */
	private boolean checkLogFileThread() {
		if (logFileThread == null) {
			try {
				logFile = new File(getCacheDir(),
						DSCApplication.LOG_FILE_NAME);
				logFileThread = new LogThread(logFile);
				sFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
						mLocale);
				sFormatter.setTimeZone(TimeZone.getDefault());
				new Thread(logFileThread).start();
			} catch (Exception e) {
				logE(TAG, "Exception: " + e.getMessage(), e);
			}
		}
		return logFileThread != null;
	}

	/**
	 * Obtain the log file.
	 * 
	 * @return The log file.
	 */
	public File getLogFile() {
		return logFile;
	}

	/**
	 * Remove log file from disk.
	 */
	public void deleteLogFile() {
		if (logFile != null && logFile.exists()) {
			try {
				logFileThread.close();
				while (!logFileThread.isClosed()) {
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				Log.e(TAG, "deleteLogFile: " + e.getMessage(), e);
			} catch (InterruptedException e) {
				Log.e(TAG, "deleteLogFile: " + e.getMessage(), e);
			}
			logFileThread = null;
			logFile.delete();
		}
	}
	
	/**
	 * Retrieve the application version code.
	 * 
	 * @return The application version code.
	 */
	public int getVersion() {
		if (mVersionCode == -1) {
			try {
				mVersionCode = getPackageManager().getPackageInfo(
						getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e) {
			}
		}
		return mVersionCode;
	}
}
