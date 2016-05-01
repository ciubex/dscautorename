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
package ro.ciubex.dscautorename;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import ro.ciubex.dscautorename.activity.RenameShortcutUpdateListener;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.receiver.FolderObserver;
import ro.ciubex.dscautorename.receiver.FolderObserverService;
import ro.ciubex.dscautorename.receiver.MediaStorageObserverService;
import ro.ciubex.dscautorename.task.LogThread;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import ro.ciubex.dscautorename.util.Utilities;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;

/**
 * This is the application class for the DSC Auto Rename application.
 *
 * @author Claudiu Ciobotariu
 */
public class DSCApplication extends Application {
	private static final String TAG = DSCApplication.class.getName();
	private static Context mContext;
	private Locale mLocale;
	private ProgressDialog mProgressDialog;
	private SharedPreferences mSharedPreferences;
	private BackupManager mBackupManager;
	private RenameShortcutUpdateListener mShortcutUpdateListener;
	private File mLogsFolder;
	private static int mVersionCode = -1;
	private static String mVersionName = null;
	private static boolean mRenameFileRequested;
	private static boolean mRenameFileTaskCanceled;
	private static boolean mRenameFileTaskRunning;
	public static final int SERVICE_TYPE_DISABLED = 0;
	public static final int SERVICE_TYPE_CAMERA = 1;
	public static final int SERVICE_TYPE_CONTENT = 2;
	public static final int SERVICE_TYPE_FILE_OBSERVER = 3;

	public static final String LOG_FILE_NAME = "DSC_app_logs.log";
	private static File logFile;
	private static LogThread logFileThread;
	private static SimpleDateFormat sFormatter;

	public static final String KEY_SERVICE_TYPE = "serviceType";
	private static final String KEY_FOLDER_SCANNING = "folderScanning";
	public static final String KEY_ENABLED_FOLDER_SCANNING = "enabledFolderScanning";
	private static final String KEY_ENABLED_SCAN_FILES = "enableScanForFiles";
	private static final String KEY_RENAME_SHORTCUT_CREATED = "renameShortcutCreated";
	private static final String KEY_RENAME_SERVICE_START_CONFIRMATION = "hideRenameServiceStartConfirmation";
	private static final String KEY_FILE_NAME_FORMAT = "fileNameFormat";
	private static final String KEY_FILE_NAME_SUFFIX_FORMAT = "fileNameSuffixFormat";
	private static final String KEY_RENAME_VIDEO_ENABLED = "renameVideoEnabled";
	private static final String KEY_ORIGINAL_FILE_NAME_PATTERN = "originalFileNamePattern";
	private static final String KEY_FILE_RENAME_COUNT = "fileRenameCount";
	private static final String KEY_RENAME_SERVICE_START_DELAY = "renameServiceStartDelay";
	public static final String KEY_DELAY_UNIT = "delayUnit";
	private static final String KEY_REGISTERED_SERVICE_TYPE = "registeredServiceType";
	private static final String KEY_RENAME_FILE_DATE_TYPE = "renameFileDateType";
	private static final String KEY_APPEND_ORIGINAL_NAME = "appendOriginalName";
	public static final String KEY_LANGUAGE_CODE = "languageCode";
	private static final String KEY_DISPLAY_NOT_GRANT_URI_PERMISSION = "displayNotGrantUriPermission";
	public static final String KEY_APP_THEME = "appTheme";

	private static final String FIRST_TIME = "firstTime";

	public static final String LOGS_FOLDER_NAME = "logs";
	private static int mSdkInt = 8;

	private Object mMountService;
	private List<MountVolume> mMountVolumes;
	private SelectedFolderModel[] mSelectedSelectedFolderModels;
	private String mDefaultFolderScanning;
	private Map<String, FolderObserver> mFolderObserverMap;

	private static final String KEY_HAVE_PERMISSIONS_ASKED = "havePermissionsAsked";
	public static final String PERMISSION_FOR_CAMERA = "android.permission.CAMERA";
	public static final String PERMISSION_FOR_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
	public static final String PERMISSION_FOR_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
	public static final String PERMISSION_FOR_INSTALL_SHORTCUT = "com.android.launcher.permission.INSTALL_SHORTCUT";
	public static final String PERMISSION_FOR_UNINSTALL_SHORTCUT = "com.android.launcher.permission.UNINSTALL_SHORTCUT";
	public static final String PERMISSION_FOR_LOGS = "android.permission.READ_LOGS";

	public static final List<String> FUNCTIONAL_PERMISSIONS = Arrays.asList(
			PERMISSION_FOR_CAMERA,
			PERMISSION_FOR_READ_EXTERNAL_STORAGE,
			PERMISSION_FOR_WRITE_EXTERNAL_STORAGE
	);

	public static final List<String> SHORTCUT_PERMISSIONS = Arrays.asList(
			PERMISSION_FOR_INSTALL_SHORTCUT,
			PERMISSION_FOR_UNINSTALL_SHORTCUT
	);

	public static final List<String> LOGS_PERMISSIONS = Arrays.asList(PERMISSION_FOR_LOGS);

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
		DSCApplication.mContext = getApplicationContext();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mBackupManager = new BackupManager(DSCApplication.mContext);
		initLocale();
		mSdkInt = android.os.Build.VERSION.SDK_INT;
		checkRegisteredServiceType(true);
		updateMountedVolumes();
		updateSelectedFolders();
		mFolderObserverMap = new HashMap<>();
		if (SERVICE_TYPE_FILE_OBSERVER == getServiceType()) {
			initFolderObserverList(false);
		}
	}

	/**
	 * Init application locale.
	 */
	public void initLocale() {
		mLocale = getLocaleSharedPreferences();
		Locale.setDefault(mLocale);
		android.content.res.Configuration config = new android.content.res.Configuration();
		config.locale = mLocale;
		DSCApplication.mContext.getResources().updateConfiguration(config, DSCApplication.mContext.getResources().getDisplayMetrics());
	}

	/**
	 * Get the locale from the shared preference or device default locale.
	 *
	 * @return The locale which should be used on the application.
	 */
	private Locale getLocaleSharedPreferences() {
		Locale locale = Locale.getDefault();
		String language = mSharedPreferences.getString(KEY_LANGUAGE_CODE, "en");
		if (!Utilities.isEmpty(language)) {
			String[] arr = language.split("_");
			try {
				switch (arr.length) {
					case 1:
						locale = new Locale(arr[0]);
						break;
					case 2:
						locale = new Locale(arr[0], arr[1]);
						break;
					case 3:
						locale = new Locale(arr[0], arr[1], arr[2]);
						break;
				}
			} catch (Exception e) {
				Log.e(TAG, "getLocaleSharedPreferences: " + language, e);
			}
		}
		return locale;
	}

	public static Context getAppContext() {
		return DSCApplication.mContext;
	}

	/**
	 * Update mounted volumes.
	 */
	public void updateMountedVolumes() {
		mMountVolumes = Utilities.MountService.getVolumeList(getMountService(), DSCApplication.mContext);
	}

	/**
	 * Get the list of mounted volumes.
	 *
	 * @return List of mounted volumes.
	 */
	public List<MountVolume> getMountedVolumes() {
		return mMountVolumes;
	}

	/**
	 * Update selected folders for scanning
	 */
	public void updateSelectedFolders() {
		mSelectedSelectedFolderModels = getFoldersScanning();
		if (mSelectedSelectedFolderModels.length > 0) {
			for (SelectedFolderModel model : mSelectedSelectedFolderModels) {
				updateSelectedFolderModel(model);
			}
		}
	}

	public SelectedFolderModel[] getSelectedFolders() {
		return mSelectedSelectedFolderModels;
	}

	/**
	 * Update the selected folder model with the volume ID and root path.
	 *
	 * @param model The model to be updated.
	 */
	public void updateSelectedFolderModel(SelectedFolderModel model) {
		MountVolume volume = getMountVolumeByUuid(model.getUuid());
		if (volume != null) {
			model.setRootPath(volume.getPath());
		}
	}

	/**
	 * Obtain mount volume based on the path.
	 *
	 * @param path Path to check.
	 * @return Mount volume of checked path.
	 */
	public MountVolume getMountVolumeByPath(String path) {
		for (MountVolume volume : mMountVolumes) {
			if (Utilities.contained(volume.getPath(), path)) {
				return volume;
			}
		}
		return null;
	}

	/**
	 * Obtain mount volume based on the volume UUID.
	 *
	 * @param uuid The volume UUID to find.
	 * @return Mount volume with requested UUID.
	 */
	public MountVolume getMountVolumeByUuid(String uuid) {
		if (uuid != null && !"null".equalsIgnoreCase(uuid)) {
			for (MountVolume volume : mMountVolumes) {
				if (volume.getUuid() != null) {
					if (volume.getUuid().equals(uuid)
							|| ("primary".equalsIgnoreCase(uuid) && volume.isPrimary())) {
						return volume;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Notify the backup manager when SharedPreferences is changed.
	 */
	public void sharedPreferencesDataChanged() {
		mBackupManager.dataChanged();
	}

	/**
	 * Check if is enabled folder scanning.
	 *
	 * @return True if is enabled folder scanning.
	 */
	public boolean isEnabledFolderScanning() {
		return mSharedPreferences.getBoolean(KEY_ENABLED_FOLDER_SCANNING, false);
	}

	/**
	 * Check if is enabled file scanning on selected folders.
	 *
	 * @return True if is enabled file scanning on selected folders.
	 */
	public boolean isEnabledScanForFiles() {
		return isEnabledFolderScanning() && mSharedPreferences.getBoolean(KEY_ENABLED_SCAN_FILES, false);
	}

	/**
	 * Check if the rename shortcut is created on home screen.
	 *
	 * @return True if the shortcut for rename service is created on home
	 * screen.
	 */
	public boolean isRenameShortcutCreated() {
		return mSharedPreferences.getBoolean(KEY_RENAME_SHORTCUT_CREATED, false);
	}

	/**
	 * Set the boolean value of created shortcut for rename service.
	 *
	 * @param flag True or False.
	 */
	public void setRenameShortcutCreated(boolean flag) {
		saveBooleanValue(KEY_RENAME_SHORTCUT_CREATED, flag);
	}

	/**
	 * Obtain the confirmation flag for the rename service start.
	 *
	 * @return True if the confirmation of rename service start should be
	 * hidden.
	 */
	public boolean hideRenameServiceStartConfirmation() {
		return mSharedPreferences.getBoolean(KEY_RENAME_SERVICE_START_CONFIRMATION, false);
	}

	/**
	 * Get the folder used for scanning files.
	 *
	 * @return The folder user for scanning files.
	 */
	private String getFolderScanning() {
		String folders = mSharedPreferences.getString(KEY_FOLDER_SCANNING, "");
		return folders;
	}

	/**
	 * Obtain default folder scanning, normally should be /storage/sdcard/DCIM, where
	 * the /storage/sdcard/ is the primary volume.
	 *
	 * @return Default path for camera storage folder.
	 */
	public String getDefaultFolderScanning() {
		if (mDefaultFolderScanning == null) {
			String path = getPrimaryVolumePath();
			File dcim = null;
			if (path.length() > 0) {
				dcim = new File(path, "DCIM");
				if (dcim.exists() && dcim.isDirectory()) {
					path = dcim.getAbsolutePath();
				}
			} else {
				dcim = new File("DCIM");
				if (dcim.exists() && dcim.isDirectory()) {
					path = "DCIM";
				} else {
					path = "/";
				}
			}
			dcim = null;
			mDefaultFolderScanning = path;
		}
		return mDefaultFolderScanning;
	}

	/**
	 * Obtain all folders where the file should be renamed.
	 *
	 * @return An array with folders restricted for renaming.
	 */
	private SelectedFolderModel[] getFoldersScanning() {
		String stored = getFolderScanning();
		SelectedFolderModel[] items;
		if (Utilities.isEmpty(stored)) {
			items = new SelectedFolderModel[0];
		} else {
			String[] folders = stored.split(",");
			items = new SelectedFolderModel[folders.length];
			for (int i = 0; i < folders.length; i++) {
				items[i] = new SelectedFolderModel();
				items[i].fromString(this, folders[i]);
			}
		}
		return items;
	}

	/**
	 * Set or add a folder used on renaming process.
	 *
	 * @param index  Index of the folder on the list. If is -1 the the folder is
	 *               added.
	 * @param folder Folder to be stored on the scanning folder
	 */
	public void setFolderScanning(int index, SelectedFolderModel folder) {
		SelectedFolderModel[] folders = mSelectedSelectedFolderModels;
		List<SelectedFolderModel> folderList = new ArrayList<SelectedFolderModel>();
		int i = 0, len = folders.length;
		while (i < len) {
			if (i == index) {
				if (folder != null && !folderList.contains(folder)) {
					folderList.add(folder);
				}
			} else {
				if (folder != null) {
					if (Utilities.contained(folder.getFullPath(), folders[i].getFullPath())) {
						if (!folderList.contains(folder)) {
							folderList.add(folder);
						}
						index = i;
					} else {
						if (!folderList.contains(folders[i])) {
							folderList.add(folders[i]);
						}
					}
				}
			}
			i++;
		}
		if (index == -1) {
			if (!folderList.contains(folder)) {
				folderList.add(folder);
			}
		}
		persistFolderList(folderList);
	}

	/**
	 * Set folders used for scanning files.
	 *
	 * @param folderList The folders names to save.
	 */
	public void persistFolderList(List<SelectedFolderModel> folderList) {
		StringBuilder buffer = new StringBuilder();
		for (SelectedFolderModel item : folderList) {
			if (buffer.length() > 0) {
				buffer.append(',');
			}
			buffer.append(item);
		}
		saveStringValue(KEY_FOLDER_SCANNING, buffer.toString());
		updateSelectedFolders();
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
	 * Obtain the formatted file name based on a date.
	 *
	 * @param fileNameFormat The file name pattern format.
	 * @param date           Date used to generate file name.
	 * @return The formatted file name.
	 */
	public String getFileNameFormatted(String fileNameFormat, Date date) {
		DateFormat df = null;
		try {
			df = new SimpleDateFormat(fileNameFormat, mLocale);
		} catch (Exception e) {
			fileNameFormat = DSCApplication.getAppContext().getString(R.string.file_name_format);
			df = new SimpleDateFormat(fileNameFormat, mLocale);
			saveStringValue(KEY_FILE_NAME_FORMAT, fileNameFormat);
		}
		String newFileName = df.format(date);
		return newFileName;
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
	 * Launch the auto rename task.
	 */
	public void launchAutoRenameTask() {
		if (isAutoRenameEnabled()) {
			setRenameFileRequested(true);
			if (!isRenameFileTaskRunning()) {
				new RenameFileAsyncTask(this).execute();
			}
		}
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
	 * Obtain the original file name pattern.
	 *
	 * @return The original file name pattern.
	 */
	public FileNameModel[] getOriginalFileNamePattern() {
		String value = mSharedPreferences.getString(KEY_ORIGINAL_FILE_NAME_PATTERN,
				DSCApplication.getAppContext().getString(R.string.original_file_name_pattern));
		if (value.length() < 1) {
			value = DSCApplication.getAppContext().getString(R.string.original_file_name_pattern);
			saveStringValue(KEY_ORIGINAL_FILE_NAME_PATTERN, value);
		}
		String[] arr = value.split(",");
		FileNameModel[] fp = new FileNameModel[arr.length];
		for (int i = 0; i < arr.length; i++) {
			fp[i] = new FileNameModel(this, arr[i]);
			updateSelectedFolderModel(fp[i].getSelectedFolder());
		}
		return fp;
	}

	/**
	 * Update a file name pattern.
	 *
	 * @param fileNameModel File name model to be updated.
	 * @param position      Position of updated file name.
	 */
	public void saveFileNamePattern(FileNameModel fileNameModel, int position) {
		FileNameModel[] arr = getOriginalFileNamePattern();
		FileNameModel fp;
		int index;
		int len = arr.length;
		StringBuilder sb = new StringBuilder();
		for (index = 0; index < len; index++) {
			if (position == index) {
				fp = fileNameModel;
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
			sb.append(fileNameModel.toString());
		}
		if (sb.length() < 1) {
			sb.append(DSCApplication.getAppContext().getString(R.string.original_file_name_pattern));
		}
		saveFileNamePattern(sb.toString());
	}

	/**
	 * Save the files name model on the shared preferences..
	 *
	 * @param fileNamePattern The file name to be saved.
	 */
	public void saveFileNamePattern(String fileNamePattern) {
		saveStringValue(KEY_ORIGINAL_FILE_NAME_PATTERN, fileNamePattern);
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
	 * @param renameFileRequested the renameFileRequested to set
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
	 * @param flag The rename file task cancel boolean value.
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
	 * @param renameFileTaskRunning the renameFileTaskRunning to set
	 */
	public void setRenameFileTaskRunning(boolean renameFileTaskRunning) {
		DSCApplication.mRenameFileTaskRunning = renameFileTaskRunning;
	}

	/**
	 * Check if should be displayed the alert message when the application do not have permission to selected
	 * folders.
	 *
	 * @return True if the alert message should be displayed.
	 */
	public boolean isDisplayNotGrantUriPermission() {
		return false;//mSharedPreferences.getBoolean(KEY_DISPLAY_NOT_GRANT_URI_PERMISSION, true);
	}

	/**
	 * Set to show or not the alert message when the application do not have permission to selected
	 * folders.
	 *
	 * @param flag A boolean flag to be saved.
	 */
	public void setDisplayNotGrantUriPermission(boolean flag) {
		saveBooleanValue(KEY_DISPLAY_NOT_GRANT_URI_PERMISSION, flag);
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
	 * @param value The integer value to be increased the rename file counter.
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
	 * Obtain the delay unit used to starting the rename service.
	 *
	 * @return The delay unit used to starting the rename service.
	 */
	public int getDelayUnit() {
		return getIntValue(KEY_DELAY_UNIT, 1);
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
	 * @param key          The key.
	 * @param defaultValue The default value.
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
	 * = 2. -1 is returning for the first time.
	 */
	public int getRegisteredServiceType() {
		return mSharedPreferences.getInt(KEY_REGISTERED_SERVICE_TYPE, -1);
	}

	/**
	 * Save the registered service type value.
	 *
	 * @param value The registered service type value.
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
	 * @param serviceType    The specified service type to be registered.
	 * @param regServiceType The current service type registered.
	 */
	private void updateRegisteredServiceType(int serviceType, int regServiceType) {
		switch (regServiceType) {
			case SERVICE_TYPE_CONTENT:
				unregisterMediaStorageContentObserver();
				break;
			case SERVICE_TYPE_FILE_OBSERVER:
				unregisterFolderObserver();
				break;
		}
		switch (serviceType) {
			case SERVICE_TYPE_CONTENT:
				registerMediaStorageContentObserver();
				break;
			case SERVICE_TYPE_FILE_OBSERVER:
				if (isEnabledFolderScanning()) {
					registerFolderObserver();
				} else {
					serviceType = regServiceType;
					if (serviceType == SERVICE_TYPE_CONTENT) {
						registerMediaStorageContentObserver();
					}
				}
				break;
		}
		saveIntegerValue(KEY_REGISTERED_SERVICE_TYPE, serviceType);
	}

	/**
	 * Obtain which date should be used to rename
	 *
	 * @return
	 */
	public int getRenameFileDateType() {
		return getIntValue(KEY_RENAME_FILE_DATE_TYPE, 2);
	}

	/**
	 * Obtain the formatted file name suffix.
	 *
	 * @param value Value to be formatted.
	 * @return Formatted value.
	 */
	public String getFormattedFileNameSuffix(int value) {
		String defFormat = DSCApplication.getAppContext().getString(R.string.file_name_suffix_format_value);
		String format = mSharedPreferences.getString(KEY_FILE_NAME_SUFFIX_FORMAT, defFormat);
		String result;
		try {
			result = String.format(mLocale, format, value);
		} catch (IllegalFormatException e) {
			result = String.format(mLocale, defFormat, value);
			saveStringValue(KEY_FILE_NAME_SUFFIX_FORMAT, defFormat);
		}
		return result;
	}

	/**
	 * Check if the original name should be appended to the new file name.
	 *
	 * @return True, if the original name should be preserved.
	 */
	public boolean isAppendOriginalNameEnabled() {
		return mSharedPreferences.getBoolean(KEY_APPEND_ORIGINAL_NAME, false);
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
	 * Method used to dynamically register a folder observer service used to
	 * launch automatically rename service.
	 */
	private void registerFolderObserver() {
		try {
			startService(new Intent(this, FolderObserverService.class));
		} catch (Exception e) {
			logE(TAG, "registerFolderObserver: " + e.getMessage(),
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
	 * Method used to unregister the folder observer service.
	 */
	private void unregisterFolderObserver() {
		try {
			stopService(new Intent(this, FolderObserverService.class));
		} catch (Exception e) {
			logE(TAG,
					"unregisterFolderObserver: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Store a string value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The string value to be saved.
	 */
	private void saveStringValue(String key, String value) {
		Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
		sharedPreferencesDataChanged();
	}

	/**
	 * Store a boolean value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The boolean value to be saved.
	 */
	private void saveBooleanValue(String key, boolean value) {
		Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
		sharedPreferencesDataChanged();
	}

	/**
	 * Store an integer value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The integer value to be saved.
	 */
	private void saveIntegerValue(String key, int value) {
		Editor editor = mSharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
		sharedPreferencesDataChanged();
	}

	/**
	 * Remove a shared preference.
	 *
	 * @param key The key of the shared preference to be removed.
	 */
	private void removeSharedPreference(String key) {
		Editor editor = mSharedPreferences.edit();
		editor.remove(key);
		editor.commit();
		sharedPreferencesDataChanged();
	}

	/**
	 * This will show a progress dialog using a context and the message to be
	 * showed on the progress dialog.
	 *
	 * @param listener The listener class which should listen for cancel.
	 * @param context  The context where should be displayed the progress dialog.
	 * @param message  The message displayed inside of progress dialog.
	 */
	public void createProgressDialog(final ProgressCancelListener listener, Context context, String message) {
		createProgressDialog(listener, context, message, 0);
	}

	/**
	 * This will show a progress dialog using a context and the message to be
	 * showed on the progress dialog.
	 *
	 * @param listener    The listener class which should listen for cancel.
	 * @param context     The context where should be displayed the progress dialog.
	 * @param message     The message displayed inside of progress dialog.
	 * @param progressMax The progress dialog bar max steps.
	 */
	public void createProgressDialog(final ProgressCancelListener listener, Context context, String message, int progressMax) {
		hideProgressDialog();
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setTitle(R.string.please_wait);
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				DSCApplication.getAppContext().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener != null) {
							listener.onProgressCancel();
						}
					}
				});
		if (progressMax > 0) {
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(progressMax);
		}
	}

	/**
	 * Show the progress dialog.
	 */
	public void showProgressDialog() {
		if (mProgressDialog != null) {
			try {
				if (!mProgressDialog.isShowing()) {
					mProgressDialog.show();
				}
			} catch (Exception e) {
				logE(TAG, "showProgressDialog: " + e.getMessage(), e);
				hideProgressDialog();
			}
		}
	}

	/**
	 * Hide the progress dialog.
	 */
	public void hideProgressDialog() {
		if (mProgressDialog != null) {
			try {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
			} catch (Exception e) {
				logE(TAG, "hideProgressDialog: " + e.getMessage(), e);
			} finally {
				mProgressDialog = null;
			}
		}
	}

	/**
	 * Set the message on the progress dialog.
	 *
	 * @param message The message to be set.
	 */
	public void setProgressDialogMessage(String message) {
		if (mProgressDialog != null) {
			mProgressDialog.setMessage(message);
		}
	}

	/**
	 * Set the progress position for the progress dialog.
	 *
	 * @param position The progress position.
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
	 * @param listener The rename shortcut update listener.
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
	 * @param data Intent data from the shortcut broadcast.
	 * @param type Type of the event, uninstall or install.
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
	 * @param type Type of the event, uninstall or install.
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
	 * Send a log message and log the exception.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public void logE(String tag, String msg) {
		Log.e(tag, msg);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg);
	}

	/**
	 * Send a log message and log the exception.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr  An exception to log
	 */
	public void logE(String tag, String msg, Throwable tr) {
		Log.e(tag, msg, tr);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg
				+ "\t" + Log.getStackTraceString(tr));
	}

	/**
	 * Send a log message.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public void logD(String tag, String msg) {
		Log.d(tag, msg);
		writeLogFile(System.currentTimeMillis(), "DEBUG\t" + tag + "\t" + msg);
	}

	/**
	 * Write the log message to the app log file.
	 *
	 * @param logmessage The log message.
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
				logFile = new File(getLogsFolder(), DSCApplication.LOG_FILE_NAME);
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
	 * Check if the application is launched for the first time.
	 *
	 * @return True if is the first time when the application is launched.
	 */
	public boolean isFirstTime() {
		String key = FIRST_TIME + getVersionCode();
		boolean result = mSharedPreferences.getBoolean(key, true);
		if (result) {
			saveBooleanValue(key, false);
		}
		return result;
	}

	/**
	 * Check if is the first time when the application was installed.
	 *
	 * @return True if is the first time when the application was installed.
	 */
	public boolean isFirstInstallation() {
		Map<String, ?> allEntries = mSharedPreferences.getAll();
		String key = FIRST_TIME + getVersionCode();
		String keyEntry;
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			keyEntry = entry.getKey();
			if (keyEntry.startsWith(FIRST_TIME)) {
				return keyEntry.equals(key);
			}
		}
		return false;
	}

	/**
	 * Retrieve the application version code.
	 *
	 * @return The application version code.
	 */
	public int getVersionCode() {
		if (mVersionCode == -1) {
			try {
				mVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e) {
			}
		}
		return mVersionCode;
	}

	/**
	 * Retrieve the application version name.
	 *
	 * @return The application version name.
	 */
	public String getVersionName() {
		if (mVersionName == null) {
			try {
				mVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
			}
		}
		return mVersionName;
	}

	/**
	 * Get logs folder. If is not defined then is initialized and created.
	 *
	 * @return Logs folder.
	 */
	public File getLogsFolder() {
		if (mLogsFolder == null) {
			mLogsFolder = new File(getCacheDir() + File.separator + DSCApplication.LOGS_FOLDER_NAME);
			if (!mLogsFolder.exists()) {
				mLogsFolder.mkdirs();
			}
		}
		return mLogsFolder;
	}

	/**
	 * Apply URI permission to selected folders.
	 *
	 * @return A list of not granted folders.
	 */
	public List<String> doGrantUriPermission(ContentResolver resolver, List<SelectedFolderModel> folders) {
		List<String> list = new ArrayList<>();
		for (SelectedFolderModel folder : folders) {
			if (!doGrantUriPermission(resolver, folder.getUri(), folder.getFlags())) {
				list.add(folder.getFullPath());
			}
		}
		return list; // list of folders which do not received access permissions
	}

	/**
	 * Obtain the Mount Service object.
	 *
	 * @return The Mount Service.
	 */
	public Object getMountService() {
		if (mMountService == null) {
			mMountService = Utilities.MountService.getService();
		}
		return mMountService;
	}

	/**
	 * Get primary mounted volume path.
	 *
	 * @return Empty string or primary mounted volume path.
	 */
	private String getPrimaryVolumePath() {
		for (MountVolume volume : mMountVolumes) {
			if (volume.isPrimary()) {
				return volume.getPath();
			}
		}
		if (mMountVolumes.size() > 0) {
			for (MountVolume volume : mMountVolumes) {
				if (volume.isMounted()) {
					return volume.getPath();
				}
			}
		}
		return "";
	}

	/**
	 * The user-visible SDK version of the framework.
	 *
	 * @return The user-visible SDK version of the framework
	 */
	public int getSdkInt() {
		return mSdkInt;
	}

	/**
	 * Take URI permission
	 */
	@TargetApi(21)
	public boolean doGrantUriPermission(ContentResolver resolver, Uri uri, int flags) {
		boolean result = false;
		try {
			this.getApplicationContext().grantUriPermission(this.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
			final int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
					| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			resolver.takePersistableUriPermission(uri, takeFlags);
			result = true;
			this.logD(TAG, "Successfully doGrantUriPermission(" + String.valueOf(uri) + ")");
		} catch (Exception e) {
			this.logE(TAG, "doGrantUriPermission(" + String.valueOf(uri) + ")", e);
		}
		return result;
	}

	/**
	 * For API >= 21 the file path should be translated
	 * from original full path: /storage/sdcard/DCIM/Camera/20150325_193246.jpg
	 * to a valid API Uri: content://com.android.externalstorage.documents/tree/17EA-1C19:DCIM/Camera/20150325_193246.jpg
	 *
	 * @param fullFilePath Original full path.
	 * @return New URI compatible file path string.
	 */
	@TargetApi(21)
	public Uri getDocumentUri(List<SelectedFolderModel> selectedFolders, String fullFilePath) {
		if (!Utilities.isEmpty(selectedFolders)) {
			String currentFullPath, documentId = null, previousFullPath = "";
			Uri selectedUri = null;
			for (SelectedFolderModel selectedFolder : selectedFolders) {
				currentFullPath = selectedFolder.getFullPath();
				if (folderMatching(previousFullPath, currentFullPath, fullFilePath)) {
					previousFullPath = currentFullPath;
					documentId = fullFilePath.replace(selectedFolder.getRootPath() + "/", selectedFolder.getUuid() + ":");
					selectedUri = selectedFolder.getUri();
				}
			}
			if (selectedUri != null && documentId != null) {
				return DocumentsContract.buildDocumentUriUsingTree(selectedUri, documentId);
			}
		}
		return null;
	}

	private boolean folderMatching(String previousMatch, String actualMatch, String testMatch) {
		if (testMatch.startsWith(actualMatch)) {
			return previousMatch.length() < actualMatch.length();
		}
		return false;
	}

	/**
	 * Initialise list of observers.
	 */
	public void initFolderObserverList(boolean startWatching) {
		if (isEnabledFolderScanning()) {
			SelectedFolderModel[] folders = getSelectedFolders();
			if (folders != null && folders.length > 0) {
				File file;
				for (SelectedFolderModel folder : folders) {
					file = new File(folder.getFullPath());
					registerRecursivelyPath(file, startWatching);
				}
			}
		}
	}

	/**
	 * Method used to recursive register file observers on selected path and its subfolders.
	 *
	 * @param file          Path to be registered and subfolders.
	 * @param startWatching If true the created observer will start watching.
	 */
	private void registerRecursivelyPath(File file, boolean startWatching) {
		if (isValidFolder(file)) {
			String path = file.getAbsolutePath();
			FolderObserver observer = mFolderObserverMap.get(path);
			if (observer == null) {
				logD(TAG, "registerRecursivelyPath: " + path);
				observer = new FolderObserver(this, path);
				if (startWatching) {
					observer.startWatching();
				}
				mFolderObserverMap.put(path, observer);
			}
			// check for subfolders
			File[] files = null;
			try {
				files = file.listFiles();
			} catch (Exception e) {
				logE(TAG, "Exception on registerRecursivelyPath: " + path, e);
			}
			if (files != null) {
				for (File subfolder : files) {
					if (isValidFolder(subfolder)) {
						registerRecursivelyPath(subfolder, startWatching);
					}
				}
			}
		}
	}

	/**
	 * Check if the folder is ok to be watched.
	 *
	 * @param folder Folder to validate.
	 * @return True if the folder can be watched.
	 */
	private boolean isValidFolder(File folder) {
		return folder != null && folder.exists() && folder.isDirectory() && !isHidden(folder);
	}

	/**
	 * Check if a file is hidden.
	 *
	 * @param file File to check.
	 * @return True if the file is hidden
	 */
	private boolean isHidden(File file) {
		return file != null ? file.isHidden() || file.getName().charAt(0) == '.' : true;
	}

	/**
	 * Clean up observers list.
	 */
	public void cleanupObservers() {
		if (mFolderObserverMap != null && !mFolderObserverMap.isEmpty()) {
			logD(TAG, "cleanupObservers");
			for (FolderObserver observer : mFolderObserverMap.values()) {
				observer.stopWatching();
			}
			mFolderObserverMap.clear();
		}
	}

	/**
	 * Start registered observers.
	 */
	public void startWatchingObservers() {
		if (mFolderObserverMap != null && !mFolderObserverMap.isEmpty()) {
			for (FolderObserver observer : mFolderObserverMap.values()) {
				observer.startWatching();
			}
		}
	}

	/**
	 * Method used to update the observer list.
	 */
	public void updateFolderObserverList() {
		if (mFolderObserverMap != null) {
			cleanupObservers();
			if (SERVICE_TYPE_FILE_OBSERVER == getServiceType()) {
				initFolderObserverList(true);
			}
		}
	}

	/**
	 * Write the shared preferences to provided writer.
	 *
	 * @param writer The writer used to write the shared preferences.
	 */
	public void writeSharedPreferences(Writer writer) {
		Map<String, ?> allEntries = mSharedPreferences.getAll();
		try {
			for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
				writer.write(entry.getKey());
				writer.write(": \"");
				writer.write(String.valueOf(entry.getValue()));
				writer.write("\"");
				writer.write('\n');
			}
		} catch (IOException e) {
			logE(TAG, "writeSharedPreferences: " + e.getMessage(), e);
		}
	}

	/**
	 * Check if should be asked for permissions.
	 *
	 * @return True if should be asked for permissions.
	 */
	public boolean shouldAskPermissions() {
		boolean result = mSdkInt > 22;
		return result;
	}

	/**
	 * Check if the permissions were asked.
	 *
	 * @return True if the permissions were asked.
	 */
	public boolean havePermissionsAsked() {
		return mSharedPreferences.getBoolean(KEY_HAVE_PERMISSIONS_ASKED, false);
	}

	/**
	 * Set the permission asked flag to true.
	 */
	public void markPermissionsAsked() {
		saveBooleanValue(KEY_HAVE_PERMISSIONS_ASKED, true);
	}

	/**
	 * Check if a permission was asked.
	 *
	 * @param permission The permission to be asked.
	 * @return True if the permission was asked before.
	 */
	public boolean isPermissionAsked(String permission) {
		return mSharedPreferences.getBoolean(permission, false);
	}

	/**
	 * Mark a permission as asked.
	 *
	 * @param permission Permission to be marked as asked.
	 */
	public void markPermissionAsked(String permission) {
		saveBooleanValue(permission, true);
	}

	/**
	 * Remove the permission asked flag.
	 *
	 * @param permission The permission for which will be removed the asked flag.
	 */
	public void removePermissionAskedMark(String permission) {
		removeSharedPreference(permission);
	}

	/**
	 * Check if a permission is allowed.
	 *
	 * @param permission The permission to be checked.
	 * @return True if the permission is allowed.
	 */
	public boolean hasPermission(String permission) {
		if (shouldAskPermissions()) {
			return hasPermission23(permission);
		}
		return true;
	}

	/**
	 * Check if a permission is allowed. (API 23)
	 *
	 * @param permission The permission to be checked.
	 * @return True if the permission is allowed.
	 */
	@TargetApi(23)
	private boolean hasPermission23(String permission) {
		return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
	}

	/**
	 * Check if the application have functional permissions.
	 *
	 * @return True if all functional permissions are allowed.
	 */
	public boolean haveFunctionalPermissions() {
		for (String permission : DSCApplication.FUNCTIONAL_PERMISSIONS) {
			if (!hasPermission(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the application have shortcut permissions.
	 *
	 * @return True if all shortcut permissions are allowed.
	 */
	public boolean haveShortcutPermissions() {
		for (String permission : DSCApplication.SHORTCUT_PERMISSIONS) {
			if (!hasPermission(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the application have logs permissions.
	 *
	 * @return True if all logs permissions are allowed.
	 */
	public boolean haveLogsPermissions() {
		for (String permission : DSCApplication.LOGS_PERMISSIONS) {
			if (!hasPermission(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get all not granted permissions.
	 */
	public String[] getNotGrantedPermissions() {
		List<String> permissions = new ArrayList<>();
		buildRequiredPermissions(permissions, DSCApplication.FUNCTIONAL_PERMISSIONS, true);
		buildRequiredPermissions(permissions, DSCApplication.SHORTCUT_PERMISSIONS, true);
//		buildRequiredPermissions(permissions, DSCApplication.LOGS_PERMISSIONS, true);
		String[] array = null;
		if (!permissions.isEmpty()) {
			array = new String[permissions.size()];
			array = permissions.toArray(array);
		}
		return array;
	}

	/**
	 * Get an array with all required permissions.
	 *
	 * @return Array with permissions to be requested.
	 */
	public String[] getAllRequiredPermissions() {
		List<String> permissions = new ArrayList<>();
		buildRequiredPermissions(permissions, DSCApplication.FUNCTIONAL_PERMISSIONS, false);
		buildRequiredPermissions(permissions, DSCApplication.SHORTCUT_PERMISSIONS, false);
//		buildRequiredPermissions(permissions, DSCApplication.LOGS_PERMISSIONS, false);
		String[] array = null;
		if (!permissions.isEmpty()) {
			array = new String[permissions.size()];
			array = permissions.toArray(array);
		}
		return array;
	}

	/**
	 * Put on the permissions all required permissions which is missing and was not asked.
	 *
	 * @param permissions         List of permissions to be requested.
	 * @param requiredPermissions List with all required permissions to be checked.
	 */
	private void buildRequiredPermissions(List<String> permissions, List<String> requiredPermissions, boolean force) {
		for (String permission : requiredPermissions) {
			if ((force && !hasPermission(permission)) ||
					(!isPermissionAsked(permission) && !hasPermission(permission))) {
				permissions.add(permission);
			}
		}
	}

	/**
	 * Invalidate stored paths.
	 */
	public void invalidatePaths() {
		saveBooleanValue(KEY_ENABLED_FOLDER_SCANNING, false);
		removeSharedPreference(KEY_FOLDER_SCANNING);
		String value = mSharedPreferences.getString(KEY_ORIGINAL_FILE_NAME_PATTERN, null);
		if (value != null) { //  remove paths from file rename prefix
			StringBuilder sb = new StringBuilder();
			String[] items = value.split(",");
			String[] arr;
			for (String item : items) {
				arr = item.split("\\|");
				if (arr.length > 0) {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(arr[0]);
				}
			}
			if (sb.length() > 0) {
				saveStringValue(KEY_ORIGINAL_FILE_NAME_PATTERN, sb.toString());
			}
		}
	}

	/**
	 * Get the application theme.
	 *
	 * @return The application theme.
	 */
	public int getApplicationTheme() {
		String theme = mSharedPreferences.getString(KEY_APP_THEME, "light");
		if ("dark".equals(theme)) {
			return R.style.AppThemeDark;
		}
		return R.style.AppThemeLight;
	}

	/**
	 * Get the application dialog window theme.
	 *
	 * @return The application dialog window theme.
	 */
	public int getApplicationDialogTheme() {
		String theme = mSharedPreferences.getString(KEY_APP_THEME, "light");
		if ("dark".equals(theme)) {
			return R.style.DialogThemeDark;
		}
		return R.style.DialogThemeLight;
	}
}
