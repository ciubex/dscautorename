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
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
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
		OnSharedPreferenceChangeListener, RenameFileAsyncTask.Listener {
	private final String TAG = getClass().getName();
	private DSCApplication mApplication;
	private ListPreference mServiceTypeList;
	private EditTextPreference mOriginalFilePrefix;
	private EditTextPreference mFileNameFormat;
	private Preference mManuallyStartRename;
	private Preference mFileRenameCount;
	private Preference mBuildVersion;
	private Preference mLicensePref;
	private Preference mDonatePref;
	private ProgressDialog mProgressDialog;

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
	}

	/**
	 * Initialize preferences controls.
	 */
	private void initPreferences() {
		mServiceTypeList = (ListPreference) findPreference("serviceType");
		mOriginalFilePrefix = (EditTextPreference) findPreference("originalFilePrefix");
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

	/**
	 * Prepare all informations when the activity is resuming
	 */
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		prepareSummaries();
	}

	/**
	 * Unregister the preference changes when the activity is on pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
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
		if ("originalFilePrefix".equals(key)) {
			mApplication.validateOriginalFilePrefix();
		} else if ("serviceType".equals(key)) {
			mApplication.checkRegisteredServiceType(false);
		}
		prepareSummaries();
	}

	/**
	 * Prepare preferences summaries
	 */
	private void prepareSummaries() {
		Date now = new Date();
		String[] originalArr = mApplication.getOriginalFilePrefix();
		String newFileName = mApplication.getFileName(now);
		String txt1, txt2;
		if (originalArr.length > 1) {
			txt1 = mApplication.getString(R.string.original_file_prefix_desc_2,
					originalArr[0], originalArr[1]);
			txt2 = mApplication.getString(R.string.file_name_format_desc_2,
					mApplication.getFileNameFormat(), newFileName);
		} else {
			txt1 = mApplication.getString(R.string.original_file_prefix_desc,
					originalArr[0]);
			txt2 = mApplication.getString(R.string.file_name_format_desc,
					mApplication.getFileNameFormat(), newFileName);
		}
		mOriginalFilePrefix.setSummary(txt1);
		mFileNameFormat.setSummary(txt2);
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
	 * Start the rename service.
	 */
	protected void onManuallyStartRename() {
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
		showProgressDialog(
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
		if (mProgressDialog != null) {
			String message = mApplication.getString(
					position == 1 ? R.string.manually_file_rename_progress_1
							: R.string.manually_file_rename_progress_more,
					position, count);
			if (position == 0) {
				hideProgressDialog();
				showProgressDialog(message, count);
			} else {
				mProgressDialog.setMessage(message);
				mProgressDialog.setProgress(position);
			}
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
		AlertDialog.Builder dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name).setMessage(message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(R.string.ok, null);
		hideProgressDialog();
		dialog.show();
	}

	/**
	 * This will show a progress dialog using a context and the message to be
	 * showed on the progress dialog.
	 * 
	 * @param context
	 *            The context where should be displayed the progress dialog.
	 * @param message
	 *            The message displayed inside of progress dialog.
	 */
	private void showProgressDialog(String message, int max) {
		hideProgressDialog();
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(R.string.please_wait);
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mApplication.setRenameFileTaskCanceled(true);
					}
				});
		if (max > 0) {
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(max);
		}
		mProgressDialog.show();
	}

	/**
	 * Hide the progress dialog.
	 */
	private void hideProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
	}
}
