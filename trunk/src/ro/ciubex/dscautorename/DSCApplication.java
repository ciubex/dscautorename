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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ro.ciubex.dscautorename.receiver.MediaStorageObserverService;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This is the application class for the DSC Auto Rename application.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class DSCApplication extends Application {
	private final String TAG = getClass().getName();
	private Locale mLocale;
	private SharedPreferences mSharedPreferences;
	private static boolean mRenameFileRequested;
	private static boolean mRenameFileTaskCanceled;
	private static boolean mRenameFileTaskRunning;
	public static final int SERVICE_TYPE_DISABLED = 0;
	public static final int SERVICE_TYPE_CAMERA = 1;
	public static final int SERVICE_TYPE_CONTENT = 2;

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
		return mSharedPreferences.getString("fileNameFormat",
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
			saveStringValue("fileNameFormat", fileNameFormat);
			Log.e(TAG, "getFileName: " + date, e);
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
	 * Check if the video files should renamed too.
	 * 
	 * @return True if the video files should be renamed.
	 */
	public boolean isRenameVideoEnabled() {
		return mSharedPreferences.getBoolean("renameVideoEnabled", true);
	}

	/**
	 * Obtain the original file name prefix.
	 * 
	 * @return The original file name prefix.
	 */
	public String[] getOriginalFilePrefix() {
		String value = mSharedPreferences.getString("originalFilePrefix",
				getString(R.string.original_file_prefix));
		if (value.length() < 1) {
			value = getString(R.string.original_file_prefix);
			saveStringValue("originalFilePrefix", value);
		}
		return value.split(",");
	}

	/**
	 * Validate original file prefix.
	 */
	public void validateOriginalFilePrefix() {
		String value = mSharedPreferences.getString("originalFilePrefix",
				getString(R.string.original_file_prefix));
		String[] array = value.split(",");
		String temp;
		StringBuilder sBuilder = new StringBuilder("");
		for (int i = 0; i < array.length; i++) {
			temp = array[i].trim();
			if (temp.length() > 0) {
				if (i > 0) {
					sBuilder.append(',');
				}
				sBuilder.append(temp);
			}
		}
		String result = sBuilder.toString();
		if (result.length() < 1) {
			saveStringValue("originalFilePrefix",
					getString(R.string.original_file_prefix));
		} else if (!value.equals(result)) {
			saveStringValue("originalFilePrefix", result);
		}
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
		return mSharedPreferences.getInt("fileRenameCount", 0);
	}

	/**
	 * Increase the rename file counter.
	 * 
	 * @param value
	 *            The integer value to be increased the rename file counter.
	 */
	public void increaseFileRenameCount(int value) {
		if (value == -1) {
			removeSharedPreference("fileRenameCount");
		} else {
			int oldValue = getFileRenameCount();
			saveIntegerValue("fileRenameCount", oldValue + value);
		}
	}

	/**
	 * Obtain the delay for starting the rename service.
	 * 
	 * @return The delay for starting the rename service.
	 */
	public int getRenameServiceStartDelay() {
		return mSharedPreferences.getInt("renameServiceStartDelay", 3);
	}

	/**
	 * Obtain the selected service type.
	 * 
	 * @return Disabled = 0, camera = 1 or content = 2.
	 */
	public int getServiceType() {
		String strValue = mSharedPreferences.getString("serviceType", "1");
		int value = 1;
		try {
			value = Integer.parseInt(strValue);
		} catch (NumberFormatException e) {
			Log.e(TAG, "getServiceType: " + strValue, e);
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
		return mSharedPreferences.getInt("registeredServiceType", -1);
	}

	/**
	 * Save the registered service type value.
	 * 
	 * @param value
	 *            The registered service type value.
	 */
	public void setRegisteredServiceType(int value) {
		saveIntegerValue("registeredServiceType", value);
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
		saveIntegerValue("registeredServiceType", serviceType);
	}

	/**
	 * Method used to dynamically register a content observer service used to
	 * launch automatically rename service.
	 */
	private void registerMediaStorageContentObserver() {
		try {
			startService(new Intent(this, MediaStorageObserverService.class));
		} catch (Exception e) {
			Log.e(TAG,
					"registerMediaStorageContentObserver: " + e.getMessage(), e);
		}
	}

	/**
	 * Method used to unregister the content observer service.
	 */
	private void unregisterMediaStorageContentObserver() {
		try {
			stopService(new Intent(this, MediaStorageObserverService.class));
		} catch (Exception e) {
			Log.e(TAG,
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
}
