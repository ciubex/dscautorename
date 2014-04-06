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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * This is the application class for the DSC Auto Rename application.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class DSCApplication extends Application {
	private Locale mLocale;
	private SharedPreferences mSharedPreferences;
	private static boolean mRenameFileTaskCanceled;
	private static boolean mRenameFileTaskBusy;

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
		String newFileName = new SimpleDateFormat(getFileNameFormat(), mLocale)
				.format(date);
		return newFileName;
	}

	/**
	 * Check if the service is enabled.
	 * 
	 * @return True if the service is enabled.
	 */
	public boolean isAutoRenameEnabled() {
		return mSharedPreferences.getBoolean("enableAutoRename", true);
	}

	/**
	 * Obtain the original image name prefix.
	 * 
	 * @return The original image name prefix.
	 */
	public String getOriginalImagePrefix() {
		return mSharedPreferences.getString("originalImagePrefix",
				getString(R.string.original_image_prefix));
	}

	/**
	 * Check if the rename file async task is running.
	 * 
	 * @return True if the rename file async task is running.
	 */
	public boolean isRenameFileTaskBusy() {
		return mRenameFileTaskBusy;
	}

	/**
	 * Set the rename file async task running state.
	 * 
	 * @param flag
	 *            The rename file async task running state.
	 */
	public void setRenameFileTaskBusy(boolean flag) {
		DSCApplication.mRenameFileTaskBusy = flag;
	}

	/**
	 * Obtain the rename file task cancel boolean value.
	 * 
	 * @return The rename file task cancel boolean value.
	 */
	public boolean isRenameFileTaskCanceled() {
		return mRenameFileTaskCanceled;
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
}
