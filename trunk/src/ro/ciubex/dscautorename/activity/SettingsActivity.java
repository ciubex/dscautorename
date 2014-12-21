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
package ro.ciubex.dscautorename.activity;

import java.util.Date;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.dialog.SelectFoldersListDialog;
import ro.ciubex.dscautorename.dialog.SelectPrefixDialog;
import ro.ciubex.dscautorename.model.FilePrefix;
import ro.ciubex.dscautorename.model.FolderItem;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * This is main activity class, actually is a preference activity.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, RenameFileAsyncTask.Listener,
		DSCApplication.ProgressCancelListener, RenameShortcutUpdateListener {
	private static final String TAG = SettingsActivity.class.getName();
	private DSCApplication mApplication;
	private ListPreference mServiceTypeList;
	private ListPreference mRenameFileDateType;
	private Preference mDefinePrefixes;
	private EditTextPreference mFileNameFormat;
	private Preference mFolderScanningPref;
	private Preference mToggleRenameShortcut;
	private CheckBoxPreference mHideRenameServiceStartConfirmation;
	private Preference mManuallyStartRename;
	private Preference mFileRenameCount;
	private Preference mBuildVersion;
	private Preference mLicensePref;
	private Preference mDonatePref;

	/**
	 * Method called when the activity is created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_activity);
		mApplication = (DSCApplication) getApplication();
		initPreferences();
		initCommands();
		checkProVersion();
		updateShortcutFields();
	}

	/**
	 * Initialize preferences controls.
	 */
	private void initPreferences() {
		mServiceTypeList = (ListPreference) findPreference("serviceType");
		mRenameFileDateType = (ListPreference) findPreference("renameFileDateType");
		mDefinePrefixes = (Preference) findPreference("definePrefixes");
		mFolderScanningPref = (Preference) findPreference("folderScanningPref");
		mToggleRenameShortcut = (Preference) findPreference("toggleRenameShortcut");
		mHideRenameServiceStartConfirmation = (CheckBoxPreference) findPreference("hideRenameServiceStartConfirmation");
		mFileNameFormat = (EditTextPreference) findPreference("fileNameFormat");
		mManuallyStartRename = (Preference) findPreference("manuallyStartRename");
		mFileRenameCount = (Preference) findPreference("fileRenameCount");
		mBuildVersion = (Preference) findPreference("buildVersion");
		mLicensePref = (Preference) findPreference("licensePref");
		mDonatePref = (Preference) findPreference("donatePref");
	}

	/**
	 * Initialize the preference commands.
	 */
	private void initCommands() {
		mDefinePrefixes
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onDefinePrefixes();
						return true;
					}
				});
		mFolderScanningPref
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onFolderScanningPref();
						return true;
					}
				});
		mToggleRenameShortcut
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onToggleRenameShortcut();
						return true;
					}
				});
		mManuallyStartRename
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onManuallyStartRename();
						return true;
					}
				});
		mFileRenameCount
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onResetFileRenameCounter();
						return true;
					}
				});
		mBuildVersion
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onBuildVersion();
						return true;
					}
				});
		mLicensePref
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onLicensePref();
						return true;
					}
				});
		mDonatePref
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onDonatePref();
						return true;
					}
				});
	}

	private void checkProVersion() {
		if (mApplication.isProPresent()) {
			mDonatePref.setEnabled(false);
			mDonatePref.setTitle(R.string.thank_you_title);
			mDonatePref.setSummary(R.string.thank_you_desc);
		}
	}

	/**
	 * Prepare all informations when the activity is resuming
	 */
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		mApplication.updateShortcutUpdateListener(this);
		prepareSummaries();
		checkLastRenameMessage();
	}

	/**
	 * Unregister the preference changes when the activity is on pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mApplication.updateShortcutUpdateListener(null);
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * This method is invoked when a preference is changed
	 * 
	 * @param sp
	 *            The shared preference
	 * @param key
	 *            Key of changed preference
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if ("serviceType".equals(key)) {
			mApplication.checkRegisteredServiceType(false);
		}
		prepareSummaries();
	}

	/**
	 * Prepare preferences summaries
	 */
	private void prepareSummaries() {
		Date now = new Date();
		String temp;
		FilePrefix[] originalArr = mApplication.getOriginalFilePrefix();
		String newFileName = mApplication.getFileName(now);
		String summary = mApplication.getString(
				R.string.define_file_prefix_desc, originalArr[0].getBefore());
		mDefinePrefixes.setSummary(summary);
		summary = mApplication.getString(R.string.file_name_format_desc,
				mApplication.getFileNameFormat(), originalArr[0].getAfter()
						+ newFileName);
		mFileNameFormat.setSummary(summary);
		switch (mApplication.getServiceType()) {
		case DSCApplication.SERVICE_TYPE_CAMERA:
			mServiceTypeList.setSummary(R.string.service_choice_1);
			break;
		case DSCApplication.SERVICE_TYPE_CONTENT:
			mServiceTypeList.setSummary(R.string.service_choice_2);
			break;
		default:
			mServiceTypeList.setSummary(R.string.service_choice_0);
			break;
		}
		FolderItem[] folders = mApplication.getFoldersScanning();
		summary = folders[0].toString();
		if (folders.length > 1) {
			summary += ", ";
			temp = folders[1].toString();
			summary += temp.substring(0,
					10 < temp.length() ? 10 : temp.length());
			summary += "...";
		}
		// renameFileDateType
		String arr[] = mApplication.getResources().getStringArray(
				R.array.rename_file_using_labels);
		mRenameFileDateType
				.setSummary(arr[mApplication.getRenameFileDateType()]);
		mFolderScanningPref.setSummary(summary);
		mFileRenameCount.setTitle(mApplication.getString(
				R.string.file_rename_count_title,
				mApplication.getFileRenameCount()));
		mBuildVersion.setSummary(getApplicationVersion());
	}

	/**
	 * Obtain the application version.
	 * 
	 * @return The application version.
	 */
	private String getApplicationVersion() {
		String version = "1.0";
		try {
			version = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException ex) {
			Log.e(TAG, "getApplicationVersion Exception: " + ex.getMessage(),
					ex);
		}
		return version;
	}

	/**
	 * When the user click on define prefixes preferences.
	 */
	private void onDefinePrefixes() {
		new SelectPrefixDialog(this, mApplication).show();
	}

	/**
	 * Start to select a folder.
	 */
	private void onFolderScanningPref() {
		new SelectFoldersListDialog(this, mApplication).show();
	}

	/**
	 * Invoked when the user click on the "Create rename service shortcut"
	 * preference.
	 */
	private void onToggleRenameShortcut() {
		boolean isCreated = mApplication.isRenameShortcutCreated();
		boolean mustCreate = isCreated ? false : true;
		createOrRemoveRenameShortcut(mustCreate);
	}

	/**
	 * Start the rename service.
	 */
	private void onManuallyStartRename() {
		mApplication.setRenameFileRequested(true);
		if (!mApplication.isRenameFileTaskRunning()) {
			new RenameFileAsyncTask(mApplication, this).execute();
		}
	}

	/**
	 * Method invoked when was pressed the fileRenameCount preference.
	 */
	private void onResetFileRenameCounter() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setMessage(R.string.file_rename_count_confirmation)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								mApplication.increaseFileRenameCount(-1);
							}
						}).setNegativeButton(R.string.no, null).show();
	}

	/**
	 * Show about pop up dialog message.
	 */
	private void onBuildVersion() {
		Intent intent = new Intent(getBaseContext(), InfoActivity.class);
		Bundle b = new Bundle();
		b.putInt(InfoActivity.TITLE, R.string.about_section);
		b.putInt(InfoActivity.MESSAGE, R.string.about_text);
		b.putBoolean(InfoActivity.HTML_MESSAGE, true);
		intent.putExtras(b);
		startActivity(intent);
	}

	/**
	 * Show license info.
	 */
	private void onLicensePref() {
		Intent intent = new Intent(getBaseContext(), InfoActivity.class);
		Bundle b = new Bundle();
		b.putInt(InfoActivity.TITLE, R.string.license_title);
		b.putString(InfoActivity.FILE_NAME, "LICENSE.TXT");
		intent.putExtras(b);
		startActivity(intent);
	}

	/**
	 * Method invoked when was pressed the donatePref preference.
	 */
	private void onDonatePref() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setMessage(R.string.donate_confirmation)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								openDonationPage();
							}
						}).setNegativeButton(R.string.no, null).show();
	}

	/**
	 * Access the browser to open the donation page.
	 */
	private void openDonationPage() {
		String url = mApplication.getString(R.string.donate_url);
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException ex) {
			Log.e(TAG, "openDonationPage Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Method invoked when the rename file async task is started.
	 */
	@Override
	public void onTaskStarted() {
		mApplication.showProgressDialog(this, this,
				mApplication.getString(R.string.manually_service_running), 0);
	}

	/**
	 * Method invoked from the rename task when an update is required.
	 * 
	 * @param position
	 *            Current number of renamed files.
	 * @param count
	 *            The number of total files to be renamed.
	 */
	@Override
	public void onTaskUpdate(int position, int count) {
		String message = mApplication.getString(
				position == 1 ? R.string.manually_file_rename_progress_1
						: R.string.manually_file_rename_progress_more,
				position, count);
		if (position == 0) {
			mApplication.hideProgressDialog();
			mApplication.showProgressDialog(this, this, message, count);
		} else {
			mApplication.setProgressDialogMessage(message);
			mApplication.setProgressDialogProgress(position);
		}
	}

	/**
	 * Method invoked at the end of rename file async task.
	 * 
	 * @param count
	 *            Number of renamed files.
	 */
	@Override
	public void onTaskFinished(int count) {
		String message;
		switch (count) {
		case 0:
			message = mApplication
					.getString(R.string.manually_file_rename_count_0);
			break;
		case 1:
			message = mApplication
					.getString(R.string.manually_file_rename_count_1);
			break;
		default:
			message = mApplication.getString(
					R.string.manually_file_rename_count_more, count);
			break;
		}
		mApplication.hideProgressDialog();
		if (checkLastRenameMessage()) {
			showAlertDialog(message);
		}
	}

	/**
	 * Display an alert dialog with a custom message.
	 * 
	 * @param message
	 *            Message to be displayed on an alert dialog.
	 */
	private void showAlertDialog(String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name).setMessage(message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(R.string.ok, null);
		dialog.show();
	}

	@Override
	public void onProgressCancel() {
		mApplication.setRenameFileTaskCanceled(true);
	}

	/**
	 * Create or remove rename shortcut from the home screen.
	 * 
	 * @param create
	 *            True if the shortcut should be created.
	 */
	private void createOrRemoveRenameShortcut(boolean create) {
		String action = create ? RenameShortcutUpdateListener.INSTALL_SHORTCUT
				: RenameShortcutUpdateListener.UNINSTALL_SHORTCUT;

		Intent shortcutIntent = new Intent();
		shortcutIntent.setAction(action);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
				getActivityIntent());
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.rename_shortcut_name));
		shortcutIntent.putExtra("duplicate", false);
		if (create) {
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(
							getApplicationContext(),
							R.drawable.ic_manual_rename));
		}
		getApplicationContext().sendBroadcast(shortcutIntent);
	}

	/**
	 * Create the manually rename service shortcut intent.
	 * 
	 * @return The manually rename service shortcut intent.
	 */
	private Intent getActivityIntent() {
		Intent activityIntent = new Intent(getApplicationContext(),
				RenameDlgActivity.class);
		activityIntent.setAction(Intent.ACTION_MAIN);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		return activityIntent;
	}

	/**
	 * Update Rename service shortcut fields, descriptions and enabled/disabled
	 * properties.
	 */
	private void updateShortcutFields() {
		if (mApplication.isRenameShortcutCreated()) {
			mToggleRenameShortcut.setTitle(R.string.remove_rename_shortcut);
			mToggleRenameShortcut
					.setSummary(R.string.remove_rename_shortcut_desc);
			mHideRenameServiceStartConfirmation.setEnabled(true);
		} else {
			mToggleRenameShortcut.setTitle(R.string.create_rename_shortcut);
			mToggleRenameShortcut
					.setSummary(R.string.create_rename_shortcut_desc);
			mHideRenameServiceStartConfirmation.setEnabled(false);
		}
	}

	/**
	 * Method invoked by the rename shortcut broadcast.
	 */
	@Override
	public void updateRenameShortcut() {
		updateShortcutFields();
	}

	/**
	 * Check last rename message.
	 * 
	 * @return FALSE if an error occurred on rename process;
	 */
	private boolean checkLastRenameMessage() {
		String message = mApplication.getLastRenameFinishMessage();
		boolean result = true;
		if (message != null && message.length() > 0
				&& !DSCApplication.SUCCESS.equals(message)) {
			if (mApplication.isKitKatOrNewer()) {
				message = mApplication.getString(
						R.string.last_error_message_android_newer, message);
			} else {
				message = mApplication.getString(R.string.last_error_message,
						message);
			}
			mApplication.setLastRenameFinishMessage(DSCApplication.SUCCESS);
			showAlertDialog(message);
			result = false;
		}
		return result;
	}
}
