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
package ro.ciubex.dscautorename.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.dialog.SelectFoldersListDialog;
import ro.ciubex.dscautorename.dialog.SelectFileNamePatternDialog;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.preference.SeekBarPreference;
import ro.ciubex.dscautorename.provider.CachedFileProvider;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import ro.ciubex.dscautorename.util.Devices;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * This is main activity class, actually is a preference activity.
 *
 * @author Claudiu Ciobotariu
 */
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, RenameFileAsyncTask.Listener,
		DSCApplication.ProgressCancelListener, RenameShortcutUpdateListener {
	private static final String TAG = SettingsActivity.class.getName();
	private DSCApplication mApplication;
	private Preference mAppTheme;
	private ListPreference mServiceTypeList;
	private Preference mRenameVideoEnabled;
	private SeekBarPreference mRenameServiceStartDelay;
	private ListPreference mDelayUnit;
	private ListPreference mRenameFileDateType;
	private Preference mDefineFileNamePatterns;
	private EditTextPreference mFileNameSuffixFormat;
	private Preference mEnabledFolderScanning;
	private Preference mFolderScanningPref;
	private Preference mEnableScanForFiles;
	private Preference mToggleRenameShortcut;
	private Preference mHideRenameServiceStartConfirmation;
	private Preference mManuallyStartRename;
	private Preference mFileRenameCount;
	private Preference mRequestPermissions;
	private Preference mBuildVersion;
	private Preference mSendDebugReport;
	private Preference mLicensePref;
	private Preference mDonatePref;
	private Preference mAppendOriginalName;
	private SelectFoldersListDialog selectFoldersListDialog;
	private SelectFileNamePatternDialog selectFileNamePatternDialog;
	private PreferenceCategory mOtherSettings;
	private static final int ID_CONFIRMATION_INFO = -2;
	private static final int ID_CONFIRMATION_ALERT = -1;
	private static final int ID_CONFIRMATION_DONATION = 0;
	private static final int ID_CONFIRMATION_RESET_RENAME_COUNTER = 1;
	private static final int ID_CONFIRMATION_DEBUG_REPORT = 2;
	private static final int ID_CONFIRMATION_REQUEST_PERMISSIONS = 3;
	private static final int ID_CONFIRMATION_MANUAL_RENAME = 4;
	private static final int REQUEST_SEND_REPORT = 1;
	public static final int REQUEST_OPEN_DOCUMENT_TREE = 42;
	public static final int REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER = 43;
	private static final int PERMISSIONS_REQUEST_CODE = 44;
	private static final int BUFFER = 1024;

	private final static int DO_NOT_SHOW_IGNORED = 0;
	private final static int DO_NOT_SHOW_GRANT_URI_PERMISSION = 1;

	private boolean doNotDisplayNotGrantUriPermission;

	/**
	 * Method called when the activity is created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mApplication = (DSCApplication) getApplication();
		applyApplicationTheme();
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		initPreferences();
		initCommands();
		initPreferencesByPermissions();
		checkProVersion();
		updateShortcutFields();
		doNotDisplayNotGrantUriPermission = (mApplication.getSdkInt() < 21 || // do not check for old API
				mApplication.isDisplayNotGrantUriPermission());
	}

	/**
	 * Apply application theme.
	 */
	private void applyApplicationTheme() {
		this.setTheme(mApplication.getApplicationTheme());
	}

	/**
	 * Initialize preferences controls.
	 */
	private void initPreferences() {
		mAppTheme = findPreference("appTheme");
		mServiceTypeList = (ListPreference) findPreference("serviceType");
		mRenameVideoEnabled = findPreference("renameVideoEnabled");
		mRenameServiceStartDelay = (SeekBarPreference) findPreference("renameServiceStartDelay");
		mDelayUnit = (ListPreference) findPreference("delayUnit");
		mRenameFileDateType = (ListPreference) findPreference("renameFileDateType");
		mDefineFileNamePatterns = findPreference("definePatterns");
		mFileNameSuffixFormat = (EditTextPreference) findPreference("fileNameSuffixFormat");
		mEnabledFolderScanning = findPreference("enabledFolderScanning");
		mFolderScanningPref = findPreference("folderScanningPref");
		mEnableScanForFiles = findPreference("enableScanForFiles");
		mToggleRenameShortcut = findPreference("toggleRenameShortcut");
		mHideRenameServiceStartConfirmation = findPreference("hideRenameServiceStartConfirmation");
		mAppendOriginalName = findPreference("appendOriginalName");
		mManuallyStartRename = findPreference("manuallyStartRename");
		mFileRenameCount = findPreference("fileRenameCount");
		mRequestPermissions = findPreference("requestPermissions");
		mBuildVersion = findPreference("buildVersion");
		mSendDebugReport = findPreference("sendDebugReport");
		mLicensePref = findPreference("licensePref");
		mDonatePref = findPreference("donatePref");
		mOtherSettings = (PreferenceCategory) findPreference("otherSettings");
	}

	/**
	 * Initialize the preference commands.
	 */
	private void initCommands() {
		mDefineFileNamePatterns
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onDefineFileNamePatterns();
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
		mRequestPermissions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onRequestPermissions();
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
		mSendDebugReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onSendDebugReport();
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
	 * Remove the permission request preference if should not be asked for permissions.
	 */
	private void initPreferencesByPermissions() {
		if (!mApplication.shouldAskPermissions()) {
			mOtherSettings.removePreference(mRequestPermissions);
		}
	}

	/**
	 * Method used to request for application required permissions.
	 */
	@TargetApi(23)
	private void requestForPermissions(String[] permissions) {
		if (!Utilities.isEmpty(permissions)) {
			requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
		}
	}

	/**
	 * Check if the pro version is present to update the donation preference item.
	 */
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
		checkAndroidVersion();
		checkForPermissions();
		setColorPreferencesSummary(mEnableScanForFiles, Color.RED);
		checkAllSelectedFolders();
	}

	/**
	 * Method used to check for application permissions.
	 */
	@TargetApi(23)
	private void checkForPermissions() {
		if (mApplication.shouldAskPermissions()) {
			updateSettingsOptionsByPermissions();
			if (!mApplication.havePermissionsAsked()) {
				requestForPermissions(mApplication.getAllRequiredPermissions());
			}
		}
	}

	/**
	 * Check if is first time when the used open this application
	 */
	private void checkAndroidVersion() {
		if (mApplication.isFirstTime()) {
			String message;
			boolean messageContainLink = false;
			if (mApplication.isFirstInstallation()) {
				message = DSCApplication.getAppContext().getString(R.string.first_time_alert);
				if (mApplication.getSdkInt() > 18 && mApplication.getSdkInt() < 21) { // KitKat
					message += "\n" + DSCApplication.getAppContext().getString(R.string.enable_filter_alert_v19);
					messageContainLink = true;
				} else if (mApplication.getSdkInt() > 20) { // Lollipop
					message += "\n" + DSCApplication.getAppContext().getString(R.string.enable_filter_alert_v21);
				}
			} else { // check update message
				message = getUpdateMessage();
			}
			if (message != null) { // show the message only if is necessary
				showConfirmationDialog(message, messageContainLink, ID_CONFIRMATION_ALERT);
			}
		}
	}

	/**
	 * Get the update message from the resources.
	 *
	 * @return The update message if is present on the resources.
	 */
	private String getUpdateMessage() {
		String message = null;
		String key = "update_message_v" + mApplication.getVersionCode();
		int id = DSCApplication.getAppContext().getResources().getIdentifier(key,
				"string", mApplication.getPackageName());
		if (id > 0) {
			message = DSCApplication.getAppContext().getString(id);
		}
		return message;
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
	 * @param sharedPreferences The shared preference
	 * @param key               Key of changed preference
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
										  String key) {
		boolean doPrepareSummaries = true;
		if (DSCApplication.KEY_SERVICE_TYPE.equals(key)) {
			mApplication.checkRegisteredServiceType(false);
		} else if (DSCApplication.KEY_ENABLED_FOLDER_SCANNING.equals(key)) {
			mApplication.updateFolderObserverList();
			mApplication.sharedPreferencesDataChanged();
		} else if (DSCApplication.KEY_LANGUAGE_CODE.equals(key) ||
				DSCApplication.KEY_APP_THEME.equals(key)) {
			doPrepareSummaries = false;
			restartActivity();
		} else {
			mApplication.sharedPreferencesDataChanged();
		}
		if (doPrepareSummaries) {
			prepareSummaries();
		}
	}

	/**
	 * Get selected units string: seconds or minutes.
	 *
	 * @return Selected units string.
	 */
	private String getSelectedUnits() {
		if (mApplication.getDelayUnit() == 60) {
			return DSCApplication.getAppContext().getString(R.string.minutes_unit);
		}
		return DSCApplication.getAppContext().getString(R.string.seconds_unit);
	}

	/**
	 * Get the application theme label.
	 * @return The application theme label.
	 */
	private String getSelectedThemeLabel() {
		String[] labels = DSCApplication.getAppContext().getResources().
				getStringArray(R.array.app_theme_labels);
		int themeId = mApplication.getApplicationTheme();
		if (R.style.AppThemeDark == themeId) {
			return labels[1];
		}
		return labels[0];
	}

	/**
	 * Restart this activity.
	 */
	private void restartActivity() {
		mApplication.initLocale();
		Intent i = DSCApplication.getAppContext().getPackageManager()
				.getLaunchIntentForPackage(DSCApplication.getAppContext().getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	/**
	 * Prepare preferences summaries
	 */
	private void prepareSummaries() {
		Date now = new Date();
		FileNameModel[] originalArr = mApplication.getOriginalFileNamePattern();
		String newFileName = mApplication.getFileNameFormatted(originalArr[0].getAfter(), now);
		String label = DSCApplication.getAppContext().getString(
				R.string.define_file_name_pattern_desc, originalArr[0].getDemoBefore());
		mDefineFileNamePatterns.setSummary(label);

		label = "" + newFileName;
		label += mApplication.getFormattedFileNameSuffix(0);
		label += "." + originalArr[0].getDemoExtension();
		label += ", " + newFileName;
		label += mApplication.getFormattedFileNameSuffix(1);
		label += "." + originalArr[0].getDemoExtension();

		label = DSCApplication.getAppContext().getString(R.string.file_name_suffix_format_desc, label);
		mFileNameSuffixFormat.setSummary(label);
		switch (mApplication.getServiceType()) {
			case DSCApplication.SERVICE_TYPE_CAMERA:
				mServiceTypeList.setSummary(R.string.service_choice_1);
				break;
			case DSCApplication.SERVICE_TYPE_CONTENT:
				mServiceTypeList.setSummary(R.string.service_choice_2);
				break;
			case DSCApplication.SERVICE_TYPE_FILE_OBSERVER:
				mServiceTypeList.setSummary(R.string.service_choice_3);
				break;
			default:
				mServiceTypeList.setSummary(R.string.service_choice_0);
				break;
		}
		label = DSCApplication.getAppContext().getString(R.string.app_theme_title_param,
				getSelectedThemeLabel());
		mAppTheme.setTitle(label);

		label = getSelectedUnits();
		mRenameServiceStartDelay.setUnits(label);
		mDelayUnit.setTitle(
				DSCApplication.getAppContext().getString(R.string.choose_units_title_param,
						label));
		if (mApplication.getSdkInt() >= 21) {
			mEnabledFolderScanning.setSummary(R.string.enable_filter_folder_desc_v21);
		}
		updateSelectedFolders();
		// renameFileDateType
		String arr[] = mApplication.getResources().getStringArray(
				R.array.rename_file_using_labels);
		mRenameFileDateType
				.setSummary(arr[mApplication.getRenameFileDateType()]);
		label = getString(R.string.append_original_name_desc, newFileName);
		mAppendOriginalName.setSummary(label);
		mFileRenameCount.setTitle(mApplication.getString(
				R.string.file_rename_count_title,
				mApplication.getFileRenameCount()));
		mBuildVersion.setSummary(mApplication.getVersionName());
	}

	/**
	 * Update selected folders.
	 */
	public void updateSelectedFolders() {
		String summary, temp;
		SelectedFolderModel[] folders = mApplication.getSelectedFolders();
		if (folders.length > 0) {
			summary = folders[0].getFullPath();
			if (folders.length > 1) {
				summary += ", ";
				temp = folders[1].getFullPath();
				summary += temp.substring(0,
						10 < temp.length() ? 10 : temp.length());
				summary += "...";
			}
			mFolderScanningPref.setSummary(summary);
		}
	}

	/**
	 * When the user click on define file name patterns preferences.
	 */
	private void onDefineFileNamePatterns() {
		if (selectFileNamePatternDialog == null) {
			selectFileNamePatternDialog = new SelectFileNamePatternDialog(this, mApplication, this);
		}
		selectFileNamePatternDialog.show();
	}

	/**
	 * Start to select a folder.
	 */
	private void onFolderScanningPref() {
		if (selectFoldersListDialog == null) {
			selectFoldersListDialog = new SelectFoldersListDialog(this, mApplication, this);
		} else {
			selectFoldersListDialog.updateSelectedFolders();
		}
		selectFoldersListDialog.show();
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
	 * Method invoked by the user to start rename service.
	 */
	private void onManuallyStartRename() {
		if (mApplication.getSdkInt() >= 21 && (!mApplication.isEnabledFolderScanning()
				|| mApplication.getSelectedFolders().length == 0)) {
			showConfirmationDialog(DSCApplication.getAppContext().getString(R.string.enable_filter_alert_v21), false,
					ID_CONFIRMATION_ALERT);
		} else {
			showConfirmationDialog(DSCApplication.getAppContext().getString(R.string.confirmation_rename_question), false,
					ID_CONFIRMATION_MANUAL_RENAME);
		}
	}

	/**
	 * Start the rename service.
	 */
	private void startRenameServiceManually() {
		mApplication.setRenameFileRequested(true);
		mApplication.logD(TAG, "startRenameServiceManually");
		if (!mApplication.isRenameFileTaskRunning()) {
			new RenameFileAsyncTask(mApplication, this, true, null).execute();
		}
	}

	/**
	 * Method invoked when was pressed the fileRenameCount preference.
	 */
	private void onResetFileRenameCounter() {
		showConfirmationDialog(DSCApplication.getAppContext().getString(R.string.file_rename_count_confirmation), false,
				ID_CONFIRMATION_RESET_RENAME_COUNTER);
	}

	/**
	 * Method invoked when was pressed the request permission preference.
	 */
	private void onRequestPermissions() {
		showConfirmationDialog(DSCApplication.getAppContext().getString(R.string.request_permissions_confirmation), false,
				ID_CONFIRMATION_REQUEST_PERMISSIONS);
	}

	/**
	 * Confirmed reset file rename counter.
	 */
	private void confirmedResetFileRenameCounter() {
		mApplication.increaseFileRenameCount(-1);
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
	 * Method invoked when the user chose to send a debug.
	 */
	private void onSendDebugReport() {
		showConfirmationDialog(DSCApplication.getAppContext().getString(R.string.send_debug_confirmation), false,
				ID_CONFIRMATION_DEBUG_REPORT);
	}

	/**
	 * Show license info.
	 */
	private void onLicensePref() {
		Intent intent = new Intent(getBaseContext(), InfoActivity.class);
		Bundle b = new Bundle();
		b.putInt(InfoActivity.TITLE, R.string.license_title);
		b.putString(InfoActivity.FILE_NAME, "gpl-3.0-standalone.html");
		b.putBoolean(InfoActivity.HTML_MESSAGE, true);
		intent.putExtras(b);
		startActivity(intent);
	}

	/**
	 * Method invoked when was pressed the donatePref preference.
	 */
	private void onDonatePref() {
		showConfirmationDialog(DSCApplication.getAppContext().getString(R.string.donate_confirmation), false,
				ID_CONFIRMATION_DONATION);
	}

	/**
	 * Show a confirmation popup dialog.
	 *
	 * @param message            Message of the confirmation dialog.
	 * @param messageContainLink A boolean flag which mark if the text contain links.
	 * @param confirmationId     ID of the process to be executed if confirmed.
	 */
	private void showConfirmationDialog(String message,
										boolean messageContainLink, final int confirmationId) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle(R.string.app_name);
		if (messageContainLink) {
			ScrollView scrollView = new ScrollView(this);
			SpannableString spanText = new SpannableString(message);
			Linkify.addLinks(spanText, Linkify.ALL);

			TextView textView = new TextView(this);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setText(spanText);

			scrollView.setPadding(14, 2, 10, 12);
			scrollView.addView(textView);
			alertDialog.setView(scrollView);
		} else {
			alertDialog.setMessage(message);
		}
		alertDialog.setCancelable(false);
		if (confirmationId == ID_CONFIRMATION_ALERT) {
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.setNeutralButton(R.string.ok, null);
		} else if (confirmationId == ID_CONFIRMATION_INFO) {
			alertDialog.setIcon(android.R.drawable.ic_dialog_info);
			alertDialog.setNeutralButton(R.string.ok, null);
		} else {
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int whichButton) {
							onConfirmation(confirmationId);
						}
					});
			alertDialog.setNegativeButton(R.string.no, null);
		}
		AlertDialog alert = alertDialog.create();
		alert.show();
	}

	/**
	 * Execute proper confirmation process based on received confirmation ID.
	 *
	 * @param confirmationId Received confirmation ID.
	 */
	protected void onConfirmation(int confirmationId) {
		if (confirmationId == ID_CONFIRMATION_DONATION) {
			confirmedDonationPage();
		} else if (confirmationId == ID_CONFIRMATION_RESET_RENAME_COUNTER) {
			confirmedResetFileRenameCounter();
		} else if (confirmationId == ID_CONFIRMATION_DEBUG_REPORT) {
			confirmedSendReport(DSCApplication.getAppContext().getString(R.string.send_debug_email_title));
		} else if (confirmationId == ID_CONFIRMATION_REQUEST_PERMISSIONS) {
			String[] permissions = mApplication.getNotGrantedPermissions();
			if (Utilities.isEmpty(permissions)) {
				showConfirmationDialog(
						DSCApplication.getAppContext().getString(R.string.request_permissions_ok),
						false,
						ID_CONFIRMATION_ALERT);
			} else {
				requestForPermissions(mApplication.getNotGrantedPermissions());
			}
		} else if (confirmationId == ID_CONFIRMATION_MANUAL_RENAME) {
			startRenameServiceManually();
		}
	}

	/**
	 * Access the browser to open the donation page.
	 */
	private void confirmedDonationPage() {
		String url = DSCApplication.getAppContext().getString(R.string.donate_url);
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException ex) {
			mApplication.logE(TAG,
					"confirmedDonationPage Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Method invoked when the rename file async task is started.
	 */
	@Override
	public void onTaskStarted() {
		mApplication.createProgressDialog(this, this,
				DSCApplication.getAppContext().getString(R.string.manually_service_running));
		mApplication.showProgressDialog();
	}

	/**
	 * Method invoked from the rename task when an update is required.
	 *
	 * @param position Current number of renamed files.
	 * @param message  The message to be displayed on progress dialog.
	 */
	@Override
	public void onTaskUpdate(int position, int max, String message) {
		if (position == 0 && max > 0) {
			mApplication.createProgressDialog(this, this, message, max);
			mApplication.showProgressDialog();
		} else {
			mApplication.setProgressDialogMessage(message);
			mApplication.setProgressDialogProgress(position);
		}
	}

	/**
	 * Method invoked at the end of rename file async task.
	 *
	 * @param count Number of renamed files.
	 */
	@Override
	public void onTaskFinished(int count) {
		String message;
		switch (count) {
			case 0:
				message = DSCApplication.getAppContext().getString(R.string.manually_file_rename_count_0);
				break;
			case 1:
				message = DSCApplication.getAppContext().getString(R.string.manually_file_rename_count_1);
				break;
			default:
				message = DSCApplication.getAppContext().getString(R.string.manually_file_rename_count_more, count);
				break;
		}
		mApplication.hideProgressDialog();
		showAlertDialog(android.R.drawable.ic_dialog_info, message, DO_NOT_SHOW_IGNORED);
	}

	/**
	 * Display an alert dialog with a custom message.
	 *
	 * @param iconId           The resource ID for the dialog icon.
	 * @param message          Message to be displayed on an alert dialog.
	 * @param doNotShowAgainId ID for cases when the user chose to not show again this message.
	 */
	private void showAlertDialog(int iconId, String message, final int doNotShowAgainId) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final LayoutInflater layoutInflater = LayoutInflater.from(this);
		final View view = layoutInflater.inflate(R.layout.do_not_show_again, null);

		if (doNotShowAgainId != DO_NOT_SHOW_IGNORED) {
			dialog.setView(view);
		}
		dialog.setTitle(R.string.app_name)
				.setMessage(message)
				.setIcon(iconId)
				.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog,
										int whichButton) {
						boolean flag = false;
						View dlgView = view.findViewById(R.id.skip);
						if (dlgView instanceof CheckBox) {
//							flag = ((CheckBox)dlgView).isChecked();
						}
						doShowAlertDialog(doNotShowAgainId, flag);
					}
				});
		dialog.show();
	}

	/**
	 * Invoked when the user close the Alert Dialog.
	 *
	 * @param noShowAgainId ID for cases when the user chose to not show again this message.
	 * @param checked       Boolean flag which indicate if the do not show checkbox was checked.
	 */
	private void doShowAlertDialog(int noShowAgainId, boolean checked) {
		if (DO_NOT_SHOW_GRANT_URI_PERMISSION == noShowAgainId && checked) {
			mApplication.setDisplayNotGrantUriPermission(false);
		}
	}

	@Override
	public void onProgressCancel() {
		mApplication.setRenameFileTaskCanceled(true);
	}

	/**
	 * Create or remove rename shortcut from the home screen.
	 *
	 * @param create True if the shortcut should be created.
	 */
	private void createOrRemoveRenameShortcut(boolean create) {
		String action = create ? RenameShortcutUpdateListener.INSTALL_SHORTCUT
				: RenameShortcutUpdateListener.UNINSTALL_SHORTCUT;

		Intent shortcutIntent = new Intent();
		shortcutIntent.setAction(action);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
				getActivityIntent());
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				DSCApplication.getAppContext().getString(R.string.rename_shortcut_name));
		shortcutIntent.putExtra("duplicate", false);
		if (create) {
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(
							DSCApplication.getAppContext(),
							R.drawable.ic_manual_rename));
		}
		DSCApplication.getAppContext().sendBroadcast(shortcutIntent);
	}

	/**
	 * Create the manually rename service shortcut intent.
	 *
	 * @return The manually rename service shortcut intent.
	 */
	private Intent getActivityIntent() {
		Intent activityIntent = new Intent(DSCApplication.getAppContext(),
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
	 * User just confirmed to send a report.
	 */
	private void confirmedSendReport(String emailTitle) {
		mApplication.createProgressDialog(this, this,
				DSCApplication.getAppContext().getString(R.string.send_debug_title));
		mApplication.showProgressDialog();
		String message = DSCApplication.getAppContext().getString(R.string.report_body);
		File logsFolder = mApplication.getLogsFolder();
		File archive = getLogArchive(logsFolder);
		String[] TO = {"ciubex@yahoo.com"};

		Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle);
		emailIntent.putExtra(Intent.EXTRA_TEXT, message);

		ArrayList<Uri> uris = new ArrayList<Uri>();
		if (archive != null && archive.exists() && archive.length() > 0) {
			uris.add(Uri.parse("content://" + CachedFileProvider.AUTHORITY
					+ "/" + archive.getName()));
		}
		if (!uris.isEmpty()) {
			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		mApplication.hideProgressDialog();
		try {
			startActivityForResult(Intent.createChooser(emailIntent,
					DSCApplication.getAppContext().getString(R.string.send_report)), REQUEST_SEND_REPORT);
		} catch (ActivityNotFoundException ex) {
			mApplication.logE(TAG,
					"confirmedSendReport Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Build the logs and call the archive creator.
	 *
	 * @param logsFolder The logs folder.
	 * @return The archive file which should contain the logs.
	 */
	private File getLogArchive(File logsFolder) {
		File logFile = mApplication.getLogFile();
		File logcatFile = getLogcatFile(logsFolder);
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String fileName = "DSC_logs_" + format.format(now) + ".zip";
		return getArchives(new File[]{logFile, logcatFile}, logsFolder, fileName);
	}

	/**
	 * Method used to build a ZIP archive with log files.
	 *
	 * @param files       The log files to be added.
	 * @param logsFolder  The logs folder where should be added the archive name.
	 * @param archiveName The archive file name.
	 * @return The archive file.
	 */
	private File getArchives(File[] files, File logsFolder, String archiveName) {
		File archive = new File(logsFolder, archiveName);
		try {
			BufferedInputStream origin;
			FileOutputStream dest = new FileOutputStream(archive);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			byte data[] = new byte[BUFFER];
			File file;
			FileInputStream fi;
			ZipEntry entry;
			int count;
			for (int i = 0; i < files.length; i++) {
				file = files[i];
				if (file.exists() && file.length() > 0) {
					mApplication.logD(TAG, "Adding to archive: " + file.getName());
					fi = new FileInputStream(file);
					origin = new BufferedInputStream(fi, BUFFER);
					entry = new ZipEntry(file.getName());
					out.putNextEntry(entry);
					while ((count = origin.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, count);
					}
					Utilities.doClose(entry);
					Utilities.doClose(origin);
				}
			}
			Utilities.doClose(out);
		} catch (FileNotFoundException e) {
			mApplication.logE(TAG, "getArchives failed: FileNotFoundException", e);
		} catch (IOException e) {
			mApplication.logE(TAG, "getArchives failed: IOException", e);
		}
		return archive;
	}

	/**
	 * Generate logs file on cache directory.
	 *
	 * @param cacheFolder Cache directory where are the logs.
	 * @return File with the logs.
	 */
	private File getLogcatFile(File cacheFolder) {
		File logFile = new File(cacheFolder, "DSC_logcat.log");
		Process shell = null;
		InputStreamReader reader = null;
		FileWriter writer = null;
		char LS = '\n';
		char[] buffer = new char[BUFFER];
		String model = Build.MODEL;
		if (!model.startsWith(Build.MANUFACTURER)) {
			model = Build.MANUFACTURER + " " + model;
		}
		mApplication.logD(TAG, "Prepare Logs to be send via e-mail.");
		String oldCmd = "logcat -d -v threadtime ro.ciubex.dscautorename:v dalvikvm:v System.err:v *:s";
		String newCmd = "logcat -d -v threadtime";
		String command = newCmd;
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			if (mApplication.getSdkInt() <= 15) {
				command = oldCmd;
			}
			shell = Runtime.getRuntime().exec(command);
			reader = new InputStreamReader(shell.getInputStream());
			writer = new FileWriter(logFile);
			writer.write("Android version: " + Build.VERSION.SDK_INT +
					" (" + Build.VERSION.CODENAME + ")" + LS);
			writer.write("Device: " + model + LS);
			writer.write("Device name: " + Devices.getDeviceName() + LS);
			writer.write("App version: " + mApplication.getVersionName() +
					" (" + mApplication.getVersionCode() + ")" + LS);
			mApplication.writeSharedPreferences(writer);
			int n;
			do {
				n = reader.read(buffer, 0, BUFFER);
				if (n == -1) {
					break;
				}
				writer.write(buffer, 0, n);
			} while (true);
			shell.waitFor();
		} catch (IOException e) {
			mApplication.logE(TAG, "getLogcatFile failed: IOException", e);
		} catch (InterruptedException e) {
			mApplication.logE(TAG, "getLogcatFile failed: InterruptedException", e);
		} catch (Exception e) {
			mApplication.logE(TAG, "getLogcatFile failed: Exception", e);
		} finally {
			Utilities.doClose(writer);
			Utilities.doClose(reader);
			if (shell != null) {
				shell.destroy();
			}
		}
		return logFile;
	}

	/**
	 * This method is invoked when a child activity is finished and this
	 * activity is showed again
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		doNotDisplayNotGrantUriPermission = true;
		if (requestCode == REQUEST_SEND_REPORT) {
//			mApplication.deleteLogFile();
		} else if (resultCode == RESULT_OK && (
				requestCode == REQUEST_OPEN_DOCUMENT_TREE ||
						requestCode == REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER)
				) {
			if (mApplication.getSdkInt() >= 21) {
				processActionOpenDocumentTree(requestCode, data);
			}
		}
	}

	/**
	 * Process resulted data from new API regarding selected tree.
	 *
	 * @param resultData Resulted data from selected folder.
	 */
	@TargetApi(21)
	private void processActionOpenDocumentTree(int requestCode, Intent resultData) {
		Uri uri = resultData.getData();
		int flags = resultData.getFlags();
		mApplication.logD(TAG, "Selected on OpenDocumentTree uri: " + uri);
		SelectedFolderModel selectedFolder = new SelectedFolderModel();
		selectedFolder.fromUri(uri, flags);
		MountVolume volume = mApplication.getMountVolumeByUuid(selectedFolder.getUuid());
		if (volume != null && !Utilities.isEmpty(volume.getPath())) {
			selectedFolder.setRootPath(volume.getPath());
			mApplication.logD(TAG, "Selected from OpenDocumentTree: " + selectedFolder);
			if (requestCode == REQUEST_OPEN_DOCUMENT_TREE) {
				updateSelectFoldersListDialog(selectedFolder);
			} else if (requestCode == REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER) {
				updateSelectedFolder(selectedFolder);
			}
		}
	}

	/**
	 * Update select folder list dialog.
	 *
	 * @param selectedFolder Selected folder model.
	 */
	private void updateSelectFoldersListDialog(SelectedFolderModel selectedFolder) {
		int index = -1;
		if (selectFoldersListDialog != null) {
			index = selectFoldersListDialog.getSelectedIndex();
		}
		mApplication.setFolderScanning(index, selectedFolder);
		if (selectFoldersListDialog != null) {
			selectFoldersListDialog.updateSelectedFolders();
			mApplication.updateFolderObserverList();
		}
	}

	/**
	 * Update the pattern dialog with selected folder.
	 *
	 * @param selectedFolder The selected folder.
	 */
	private void updateSelectedFolder(SelectedFolderModel selectedFolder) {
		if (selectFileNamePatternDialog != null) {
			selectFileNamePatternDialog.updateSelectedFolder(selectedFolder);
		}
	}

	/**
	 * Callback for the result from requesting permissions.
	 *
	 * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
	 * @param permissions  The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (PERMISSIONS_REQUEST_CODE == requestCode) {
			mApplication.markPermissionsAsked();
			for (String permission : permissions) {
				mApplication.markPermissionAsked(permission);
			}
			updateSettingsOptionsByPermissions();
		}
	}

	/**
	 * Update settings options based on the allowed permissions.
	 */
	private void updateSettingsOptionsByPermissions() {
		boolean allowed;
		if (mApplication.shouldAskPermissions()) {
			// functionality
			allowed = mApplication.haveFunctionalPermissions();
			mServiceTypeList.setEnabled(allowed);
			mRenameVideoEnabled.setEnabled(allowed);
			mRenameServiceStartDelay.setEnabled(allowed);
			mDelayUnit.setEnabled(allowed);
			mEnabledFolderScanning.setEnabled(allowed);
			mFolderScanningPref.setEnabled(allowed);
			mEnableScanForFiles.setEnabled(allowed);
			mDefineFileNamePatterns.setEnabled(allowed);
			mFileNameSuffixFormat.setEnabled(allowed);
			mRenameFileDateType.setEnabled(allowed);
			mAppendOriginalName.setEnabled(allowed);
			mManuallyStartRename.setEnabled(allowed);
			// shortcut
			allowed = allowed && mApplication.haveShortcutPermissions();
			mToggleRenameShortcut.setEnabled(allowed);
			mHideRenameServiceStartConfirmation.setEnabled(allowed);
			// logs
			allowed = true;// mApplication.haveLogsPermissions();
			mSendDebugReport.setEnabled(allowed);
		}
	}

	/**
	 * Set the text color for the preference summary.
	 *
	 * @param preference The preference item.
	 * @param color      The color to set.
	 */
	private void setColorPreferencesSummary(Preference preference, int color) {
		CharSequence cs = preference.getSummary();
		String plainTitle = cs.subSequence(0, cs.length()).toString();
		Spannable coloredTitle = new SpannableString(plainTitle);
		coloredTitle.setSpan(new ForegroundColorSpan(color), 0, coloredTitle.length(), 0);
		preference.setSummary(coloredTitle);
	}

	/**
	 * Check for all selected folders used if have grant URI permissions.
	 */
	private void checkAllSelectedFolders() {
		if (doNotDisplayNotGrantUriPermission) {
			return;
		}
		SelectedFolderModel folderMove;
		List<SelectedFolderModel> selectedFolders = new ArrayList<>();
		for (SelectedFolderModel folder : mApplication.getSelectedFolders()) {
			if (!selectedFolders.contains(folder)) {
				selectedFolders.add(folder);
			}
		}
		for (FileNameModel fileNameModel : mApplication.getOriginalFileNamePattern()) {
			folderMove = fileNameModel.getSelectedFolder();
			if (Utilities.isMoveFiles(folderMove)) {
				if (!selectedFolders.contains(folderMove)) {
					selectedFolders.add(folderMove);
				}
			}
		}
		if (!selectedFolders.isEmpty()) {
			List<String> list = mApplication.doGrantUriPermission(mApplication.getContentResolver(), selectedFolders);
			if (!list.isEmpty()) {
				displayNotGrantUriPermissionAlertFor(list);
			}
		}
	}

	/**
	 * Display in error message for all folders which do not have granted URI permission.
	 *
	 * @param folderList List of folders for which the application do not have access permission.
	 */
	private void displayNotGrantUriPermissionAlertFor(List<String> folderList) {
		String message;
		if (folderList.size() == 1) {
			message = DSCApplication.getAppContext().
					getString(R.string.folder_list_no_grant_permission_1, folderList.get(0));
		} else {
			StringBuilder sb = new StringBuilder();
			for (String folder : folderList) {
				if (sb.length() > 0) {
					sb.append('\n');
				}
				sb.append(folder);
			}
			message = DSCApplication.getAppContext().
					getString(R.string.folder_list_no_grant_permission_2, sb.toString());
		}
		showAlertDialog(android.R.drawable.ic_dialog_alert, message, DO_NOT_SHOW_GRANT_URI_PERMISSION);
	}
}
