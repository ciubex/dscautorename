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
package ro.ciubex.dscautorename;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.multidex.MultiDex;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ro.ciubex.dscautorename.activity.RenameShortcutUpdateListener;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.receiver.FolderObserver;
import ro.ciubex.dscautorename.service.CameraRenameService;
import ro.ciubex.dscautorename.service.FileRenameService;
import ro.ciubex.dscautorename.service.FolderObserverService;
import ro.ciubex.dscautorename.service.MediaContentJobService;
import ro.ciubex.dscautorename.service.MediaStorageObserverService;
import ro.ciubex.dscautorename.task.LogThread;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * This is the application class for the DSC Auto Rename application.
 *
 * @author Claudiu Ciobotariu
 */
public class DSCApplication extends Application {
	private static final String TAG = DSCApplication.class.getName();
	private static Locale mLocale;
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
	public static final int SERVICE_TYPE_CAMERA_SERVICE = 4;

	public static final String LOG_FILE_NAME = "DSC_app_logs.log";
	private static LogThread logFileThread;

	public static final String KEY_SERVICE_TYPE = "serviceType";
	public static final String KEY_FOLDER_SCANNING = "folderScanning";
	public static final String KEY_ENABLED_FOLDER_SCANNING = "enabledFolderScanning";
	public static final String KEY_ENABLED_SCAN_FILES = "enableScanForFiles";
	private static final String KEY_RENAME_SHORTCUT_CREATED = "renameShortcutCreated";
	private static final String KEY_RENAME_SERVICE_START_CONFIRMATION = "hideRenameServiceStartConfirmation";
	private static final String KEY_FILE_NAME_FORMAT = "fileNameFormat";
	private static final String KEY_FILE_NAME_SUFFIX_FORMAT = "fileNameSuffixFormat";
	public static final String KEY_RENAME_VIDEO_ENABLED = "renameVideoEnabled";
	public static final String KEY_ORIGINAL_FILE_NAME_PATTERN = "originalFileNamePattern";
	private static final String KEY_FILE_RENAME_COUNT = "fileRenameCount";
	private static final String KEY_RENAME_SERVICE_START_DELAY = "renameServiceStartDelay";
	public static final String KEY_DELAY_UNIT = "delayUnit";
	private static final String KEY_RENAME_FILE_DELAY = "renameFileDelay";
	private static final String KEY_REGISTERED_SERVICE_TYPE = "registeredServiceType";
	private static final String KEY_RENAME_FILE_DATE_TYPE = "renameFileDateType";
	private static final String KEY_APPEND_ORIGINAL_NAME = "appendOriginalName";
	public static final String KEY_LANGUAGE_CODE = "languageCode";
	private static final String KEY_DISPLAY_NOT_GRANT_URI_PERMISSION = "displayNotGrantUriPermission";
	public static final String KEY_APP_THEME = "appTheme";
	private static final String KEY_CAMERA_SERVICE_INSTANCE_COUNT = "cameraServiceInstanceCount";

	private static final String FIRST_TIME = "firstTime";

	public static final String LOGS_FOLDER_NAME = "logs";
	private static int mSdkInt = 8;

	private Object mMountService;
	private List<MountVolume> mMountVolumes;
	private SelectedFolderModel[] mSelectedSelectedFolderModels;
	private String mDefaultFolderScanning;
	private Map<String, FolderObserver> mFolderObserverMap;
	private boolean mInitializedVolume;
	private boolean mUpdatedMountedVolumes;

	private static final String KEY_HAVE_PERMISSIONS_ASKED = "havePermissionsAsked";
	public static final String PERMISSION_FOR_CAMERA = "android.permission.CAMERA";
	public static final String PERMISSION_FOR_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
	public static final String PERMISSION_FOR_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
	public static final String PERMISSION_FOR_INSTALL_SHORTCUT = "com.android.launcher.permission.INSTALL_SHORTCUT";
	public static final String PERMISSION_FOR_UNINSTALL_SHORTCUT = "com.android.launcher.permission.UNINSTALL_SHORTCUT";
	public static final String PERMISSION_FOR_LOGS = "android.permission.READ_LOGS";
	public static final String ID_SHORTCUT_RENAME = "ro.ciubex.dscautorename.RENAME_SHORTCUT";

	public static final String SKIP_RENAME = "SKIP_RENAME";
	public static final String KEY_SEND_BROADCAST = "sendBroadcastEnabled";
	public static final String KEY_INVOKE_MEDIA_SCANNER = "invokeMediaScannerEnabled";
	public static final String NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
	public static final String NEW_VIDEO = "android.hardware.action.NEW_VIDEO";

	private Messenger mService = null;
	private boolean mBound;
	private Intent mCameraRenameService;
	private AssetManager mAssetManager;

	public static final List<String> FUNCTIONAL_PERMISSIONS = Arrays.asList(
			PERMISSION_FOR_CAMERA,
			PERMISSION_FOR_READ_EXTERNAL_STORAGE,
			PERMISSION_FOR_WRITE_EXTERNAL_STORAGE
	);

	public static final List<String> SHORTCUT_PERMISSIONS = Arrays.asList(
			PERMISSION_FOR_INSTALL_SHORTCUT,
			PERMISSION_FOR_UNINSTALL_SHORTCUT
	);

	public static final List<String> LOGS_PERMISSIONS = Collections.singletonList(PERMISSION_FOR_LOGS);

	public interface ProgressCancelListener {
		void onProgressCancel();
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mBound = false;
		}
	};

	/**
	 * Called when the application is starting, before any activity, service, or
	 * receiver objects (excluding content providers) have been created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		checkApplicationResources();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mBackupManager = new BackupManager(this);
		initLocale();
		mSdkInt = android.os.Build.VERSION.SDK_INT;
		int serviceType = getServiceType();
		if (DSCApplication.SERVICE_TYPE_DISABLED != serviceType) {
			checkRegisteredServiceType(false);
		}
		mFolderObserverMap = new HashMap<>();
		if (SERVICE_TYPE_FILE_OBSERVER == serviceType) {
			initVolumes();
			initFolderObserverList(false);
		}
	}

	/**
	 * Workaround to handle cases when the application do not have access to resources.
	 */
	private void checkApplicationResources() {
		if (getResources() == null) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	/**
	 * Method used to configure the MutiDex support.
	 *
	 * @param base The base context to be attached to this application.
	 */
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	/**
	 * Init application locale.
	 */
	public void initLocale() {
		mLocale = getLocaleSharedPreferences();
		Locale.setDefault(mLocale);
	}

	/**
	 * Init volumes.
	 */
	public void initVolumes() {
		if (!mInitializedVolume) {
			mInitializedVolume = true;
			updateMountedVolumes();
			updateSelectedFolders();
		}
	}

	public String getLanguageCode() {
		return mSharedPreferences.getString(KEY_LANGUAGE_CODE, "en");
	}

	/**
	 * Get the locale from the shared preference or device default locale.
	 *
	 * @return The locale which should be used on the application.
	 */
	private Locale getLocaleSharedPreferences() {
		Locale locale = Locale.getDefault();
		String language = getLanguageCode();
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

	/**
	 * Update mounted volumes.
	 */
	public void updateMountedVolumes() {
		if (!mUpdatedMountedVolumes) {
			mUpdatedMountedVolumes = true;
			mMountVolumes = Utilities.MountService.getVolumeList(getMountService(), getApplicationContext());
		}
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
		return isEnabledFolderScanning() && isEnabledFolderScanningForFiles();
	}

	/**
	 * Verify if the option "Enable folder scanning for files" is enabled.
	 * @return
	 */
	public boolean isEnabledFolderScanningForFiles() {
		return mSharedPreferences.getBoolean(KEY_ENABLED_SCAN_FILES, false);
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
				items[i].fromString(folders[i]);
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
	public static Locale getLocale() {
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
			fileNameFormat = getApplicationContext().getString(R.string.file_name_format);
			df = new SimpleDateFormat(fileNameFormat, mLocale);
			saveStringValue(KEY_FILE_NAME_FORMAT, fileNameFormat);
		}
		return df.format(date);
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
	public void launchAutoRenameTask(RenameFileAsyncTask.Listener listener, boolean noDelay, List<Uri> fileUris, boolean force) {
		if (force || isAutoRenameEnabled()) {
			setRenameFileRequested(true);
			if (!isRenameFileTaskRunning()) {
				logD(TAG, "launchAutoRenameTask");
				if (listener != null) {
					new RenameFileAsyncTask(this, listener, noDelay, fileUris).execute();
				} else {
					startFileRenameService();
				}
			}
		}
	}

	/**
	 * Start file rename service.
	 */
	private void startFileRenameService() {
		logD(TAG, "startFileRenameService");
		try {
			if (mSdkInt >= Build.VERSION_CODES.O) {
				startForegroundServiceAPI26();
			} else { // old service start
				startService(new Intent(this, FileRenameService.class));
			}
		} catch (Exception e) {
			logE(TAG, "startFileRenameService: " + e.getMessage(), e);
		}
	}

	/**
	 * Starting a foreground service is required from Android O.
	 */
	@TargetApi(Build.VERSION_CODES.O)
	private void startForegroundServiceAPI26() {
		startForegroundService(new Intent(this, FileRenameService.class));
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
				getApplicationContext().getString(R.string.original_file_name_pattern));
		if (value.length() < 1) {
			value = getApplicationContext().getString(R.string.original_file_name_pattern);
			saveStringValue(KEY_ORIGINAL_FILE_NAME_PATTERN, value);
		}
		String[] arr = value.split(",");
		FileNameModel[] fp = new FileNameModel[arr.length];
		for (int i = 0; i < arr.length; i++) {
			fp[i] = new FileNameModel(arr[i]);
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
			sb.append(getApplicationContext().getString(R.string.original_file_name_pattern));
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
		logD(TAG, "setRenameFileRequested(" + renameFileRequested + ")");
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
		return mSharedPreferences.getBoolean(KEY_DISPLAY_NOT_GRANT_URI_PERMISSION, true);
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
	 * Obtain the delay for each file rename process.
	 *
	 * @return The delay for each file rename process.
	 */
	public int getRenameFileDelay() {
		return mSharedPreferences.getInt(KEY_RENAME_FILE_DELAY, 0);
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
	 * Get the camera service instances counter.
	 *
	 * @return The camera service instances counter.
	 */
	public int getCameraServiceInstanceCount() {
		return mSharedPreferences.getInt(KEY_CAMERA_SERVICE_INSTANCE_COUNT, 0);
	}

	/**
	 * Reset the camera service instance counter.
	 */
	public void resetCameraServiceInstanceCount() {
		removeSharedPreference(KEY_CAMERA_SERVICE_INSTANCE_COUNT);
	}

	/**
	 * Increase the camera service instance counter.
	 */
	public void increaseCameraServiceInstanceCount() {
		int value = getCameraServiceInstanceCount();
		saveIntegerValue(KEY_CAMERA_SERVICE_INSTANCE_COUNT, value + 1);
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
		if (!changed && SERVICE_TYPE_CONTENT == serviceType && mSdkInt > Build.VERSION_CODES.N) {
			changed = !isMediaContentJobServiceRegistered(this);
		}
		logD(TAG, "checkRegisteredServiceType force: " + force + " changed: " + changed);
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
		switch (regServiceType) { // existing registered service
			case SERVICE_TYPE_CONTENT:
				unregisterMediaStorageContentObserver();
				break;
			case SERVICE_TYPE_FILE_OBSERVER:
				unregisterFolderObserver();
				break;
			case SERVICE_TYPE_CAMERA_SERVICE:
				unregisterCameraRenameService();
				break;
		}
		switch (serviceType) { // new registered service
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
			case SERVICE_TYPE_CAMERA_SERVICE:
				registerCameraRenameService();
				enableCameraRenameService(true);
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
		String defFormat = getApplicationContext().getString(R.string.file_name_suffix_format_value);
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
	 * Check if the application should send a broadcast message when files are renamed.
	 *
	 * @return True if should be send a broadcast message.
	 */
	public boolean isSendBroadcastEnabled() {
		return mSharedPreferences.getBoolean(KEY_SEND_BROADCAST, false);
	}

	/**
	 * Check if the application should invoked the media scanner when files are renamed.
	 *
	 * @return True if should be invoked the media scanner.
	 */
	public boolean isInvokeMediaScannerEnabled() {
		return mSharedPreferences.getBoolean(KEY_INVOKE_MEDIA_SCANNER, false);
	}

	/**
	 * Method used to dynamically register a content observer service used to
	 * launch automatically rename service.
	 */
	private void registerMediaStorageContentObserver() {
		try {
			if (mSdkInt > Build.VERSION_CODES.N) {
				registerMediaContentJobService(this);
			} else {
				startService(new Intent(this, MediaStorageObserverService.class));
			}
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
		logD(TAG, "unregisterMediaStorageContentObserver");
		try {
			if (mSdkInt > Build.VERSION_CODES.N) {
				cancelMediaContentJobService(this);
			} else {
				stopService(new Intent(this, MediaStorageObserverService.class));
			}
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
	public void saveBooleanValue(String key, boolean value) {
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
				getApplicationContext().getString(R.string.cancel),
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
	public void updateShortcutPref(RenameShortcutUpdateListener.TYPE type) {
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
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg, null);
	}

	/**
	 * Send a log message and log the exception.
	 *
	 * @param tag       Used to identify the source of a log message. It usually
	 *                  identifies the class or activity where the log call occurs.
	 * @param msg       The message you would like logged.
	 * @param throwable An exception to log
	 */
	public void logE(String tag, String msg, Throwable throwable) {
		Log.e(tag, msg, throwable);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg, throwable);
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
		writeLogFile(System.currentTimeMillis(), "DEBUG\t" + tag + "\t" + msg, null);
	}

	/**
	 * Write the log message to the app log file.
	 *	 *
	 * @param milliseconds Log timestamp.
	 * @param message      Log text.
	 * @param throwable    An exception to log
	 */
	private void writeLogFile(long milliseconds, String message, Throwable throwable) {
		if (checkLogFileThread()) {
			logFileThread.addLog(milliseconds, message, throwable);
		}
	}

	/**
	 * Check if log file thread exist and create it if not.
	 */
	private boolean checkLogFileThread() {
		if (logFileThread == null) {
			try {
				logFileThread = new LogThread(getLogsFolder());
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
		return logFileThread != null ? logFileThread.getLogFile() : null;
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
		String keyEntry;
		int count = 0;
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			keyEntry = entry.getKey();
			if (keyEntry.startsWith(FIRST_TIME)) {
				count++;
			}
		}
		return count == 1;
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
		if (mMountService == null && mSdkInt < Build.VERSION_CODES.N) {
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
				if (files != null) {
					for (File subfolder : files) {
						if (isValidFolder(subfolder)) {
							registerRecursivelyPath(subfolder, startWatching);
						}
					}
				}
			} catch (Exception e) {
				logE(TAG, "Exception on registerRecursivelyPath: " + path, e);
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
		boolean result = false;
		try {
			result = folder != null && folder.exists() && folder.isDirectory() && !isHidden(folder);
		} catch (Exception e) {
			logE(TAG, "isValidFolder: "  + String.valueOf(folder) + " : " + e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Check if a file is hidden.
	 *
	 * @param file File to check.
	 * @return True if the file is hidden
	 */
	private boolean isHidden(File file) {
		boolean result = false;
		try {
			result = file != null ? file.isHidden() || file.getName().charAt(0) == '.' : true;
		} catch (Exception e) {
			logE(TAG, "isHidden: "  + String.valueOf(file) + " : " + e.getMessage(), e);
		}
		return result;
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
		if (mFolderObserverMap != null && SERVICE_TYPE_FILE_OBSERVER == getServiceType()) {
			cleanupObservers();
			initFolderObserverList(true);
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

	/**
	 * Send a broadcast message with uri of renamed file.
	 *
	 * @param uri URI to broadcast.
	 */
	public void sendBroadcastMessage(Uri uri) {
		String action = uri.getPath().contains("images") ?
				DSCApplication.NEW_PICTURE : DSCApplication.NEW_VIDEO;
		Intent intent = new Intent(action, uri);
		Bundle b = new Bundle();
		b.putBoolean(DSCApplication.SKIP_RENAME, true);
		intent.putExtras(b);
		sendBroadcast(intent);
	}

	/**
	 * Method used to register the camera rename service.
	 */
	private void registerCameraRenameService() {
		logD(TAG, "registerCameraRenameService - startService");
		try {
			if (mCameraRenameService == null) {
				mCameraRenameService = new Intent(this, CameraRenameService.class);
			}
			startService(mCameraRenameService);
			enableCameraRenameService(true);
			this.bindService(mCameraRenameService, mConnection, Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			logE(TAG, "registerCameraRenameService: " + e.getMessage(), e);
		}
	}

	/**
	 * Method used to unregister the camera rename service.
	 */
	private void unregisterCameraRenameService() {
		logD(TAG, "unregisterCameraRenameService - stopService");
		try {
			if (mBound) {
				if (mConnection != null) {
					enableCameraRenameService(false);
					this.unbindService(mConnection);
				}
				mBound = false;
			}
		} catch (Exception e) {
			logE(TAG, "unregisterCameraRenameService: " + e.getMessage(), e);
		}
	}

	/**
	 * Enable or disable the camera broadcast receiver.
	 *
	 * @param flag True if the broadcast receiver should be enabled.
	 */
	public void enableCameraRenameService(boolean flag) {
		sendMessageToService(
				CameraRenameService.ENABLE_CAMERA_RENAME_SERVICE,
				String.valueOf(flag));
	}

	/**
	 * Send a message to service.
	 */
	public void sendMessageToService(String messageKey, String messageValue) {
		if (mBound) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putString(messageKey, messageValue);
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				logE(TAG,
						"sendMessageToService(" + messageKey + "," + messageValue + "): " +
								e.getMessage(), e);
			}
		}
	}


	/**
	 * Method used to verify if the job service is registered.
	 *
	 * @param context Application context.
	 * @return True if the service is registered.
	 */
	@TargetApi(Build.VERSION_CODES.N)
	public boolean isMediaContentJobServiceRegistered(Context context) {
		JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		List<JobInfo> jobs = js != null ? js.getAllPendingJobs() : null;
		if (jobs != null && !jobs.isEmpty()) {
			for (JobInfo jobInfo : jobs) {
				if (jobInfo.getId() == MediaContentJobService.JOB_ID) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method used to cancel this service.
	 *
	 * @param context The application context.
	 */
	@TargetApi(Build.VERSION_CODES.N)
	public void cancelMediaContentJobService(Context context) {
		JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		js.cancel(MediaContentJobService.JOB_ID);
	}

	/**
	 * Method used to register this service on the context.
	 *
	 * @param context The application context.
	 */
	@TargetApi(Build.VERSION_CODES.N)
	public void registerMediaContentJobService(Context context) {
		if (mSdkInt > Build.VERSION_CODES.N) {
			JobInfo.Builder builder = new JobInfo.Builder(MediaContentJobService.JOB_ID, new ComponentName(context, MediaContentJobService.class.getName()));
			builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Images.Media.INTERNAL_CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
			builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Video.Media.INTERNAL_CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
			builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
			builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
			builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
			builder.setTriggerContentMaxDelay(1000);
			builder.setTriggerContentUpdateDelay(1000);
			builder.setRequiresDeviceIdle(false);
			logD(TAG, "registerMediaContentJobService");
			scheduleMediaContentJobService(context, builder.build());
		}
	}

	/**
	 * Method used to schedule this job service.
	 *
	 * @param context The application context.
	 */
	@TargetApi(Build.VERSION_CODES.N)
	public void scheduleMediaContentJobService(Context context, JobInfo jobInfo) {
		JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		int result = scheduler.schedule(jobInfo);
		if (result == JobScheduler.RESULT_SUCCESS) {
			logD(TAG, "JobScheduler OK");
		} else {
			logD(TAG, "JobScheduler fails: " + result);
		}
	}

	/**
	 * Reschedule the media content job service.
	 */
	public void rescheduleMediaContentJobService() {
		if (mSdkInt > Build.VERSION_CODES.N && SERVICE_TYPE_CONTENT == getServiceType()) {
			cancelMediaContentJobService(this);
			registerMediaContentJobService(this);
		}
	}

	/**
	 * Get application assets.
	 *
	 * @return Application assets.
	 */
	public AssetManager getAppAssets() {
		if (mAssetManager == null) {
			mAssetManager = getApplicationContext().getAssets();
		}
		return mAssetManager;
	}
}
