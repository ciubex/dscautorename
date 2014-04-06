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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
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
		OnSharedPreferenceChangeListener {
	private final String TAG = getClass().getName();
	private DSCApplication mApplication;
	private EditTextPreference mOriginalImagePrefix;
	private EditTextPreference mFileNameFormat;
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
	}

	/**
	 * Initialize preferences controls.
	 */
	private void initPreferences() {
		mOriginalImagePrefix = (EditTextPreference) findPreference("originalImagePrefix");
		mFileNameFormat = (EditTextPreference) findPreference("fileNameFormat");
		mFileRenameCount = (Preference) findPreference("fileRenameCount");
		mBuildVersion = (Preference) findPreference("buildVersion");
		mLicensePref = (Preference) findPreference("licensePref");
		mDonatePref = (Preference) findPreference("donatePref");
	}

	/**
	 * Initialize the preference commands.
	 */
	private void initCommands() {
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
		prepareSummaries();
	}

	/**
	 * Prepare preferences summaries
	 */
	private void prepareSummaries() {
		mOriginalImagePrefix.setSummary(mApplication.getString(
				R.string.original_image_prefix_desc,
				mApplication.getOriginalImagePrefix()));
		mFileNameFormat.setSummary(mApplication.getString(
				R.string.file_name_format_desc,
				mApplication.getFileNameFormat(),
				mApplication.getFileName(new Date())));
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
}
