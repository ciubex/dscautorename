/**
 * This file is part of DSCAutoRename application.
 * 
 * Copyright (C) 2015 Claudiu Ciobotariu
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.dialog.SelectFoldersListDialog;
import ro.ciubex.dscautorename.dialog.SelectFileNamePatternDialog;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.provider.CachedFileProvider;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import ro.ciubex.dscautorename.util.Devices;
import ro.ciubex.dscautorename.util.Utilities;

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
	private Preference mDefineFileNamePatterns;
	private EditTextPreference mFileNameSuffixFormat;
	private CheckBoxPreference mEnabledFolderScanning;
	private Preference mFolderScanningPref;
	private Preference mToggleRenameShortcut;
	private CheckBoxPreference mHideRenameServiceStartConfirmation;
	private Preference mManuallyStartRename;
	private Preference mFileRenameCount;
	private Preference mBuildVersion;
	private Preference mSendDebugReport;
	private Preference mLicensePref;
	private Preference mDonatePref;
	private CheckBoxPreference mAppendOriginalName;
	private SelectFoldersListDialog selectFoldersListDialog;
	private static final int ID_CONFIRMATION_ALERT = -1;
	private static final int ID_CONFIRMATION_DONATION = 0;
	private static final int ID_CONFIRMATION_RESET_RENAME_COUNTER = 1;
	private static final int ID_CONFIRMATION_DEBUG_REPORT = 2;
	private static final int REQUEST_SEND_REPORT = 1;
	public static final int REQUEST_OPEN_DOCUMENT_TREE = 42;
	private static final int BUFFER = 1024;

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
		mDefineFileNamePatterns = (Preference) findPreference("definePatterns");
		mFileNameSuffixFormat = (EditTextPreference) findPreference("fileNameSuffixFormat");
		mEnabledFolderScanning = (CheckBoxPreference) findPreference("enabledFolderScanning");
		mFolderScanningPref = (Preference) findPreference("folderScanningPref");
		mToggleRenameShortcut = (Preference) findPreference("toggleRenameShortcut");
		mHideRenameServiceStartConfirmation = (CheckBoxPreference) findPreference("hideRenameServiceStartConfirmation");
		mAppendOriginalName = (CheckBoxPreference) findPreference("appendOriginalName");
		mManuallyStartRename = (Preference) findPreference("manuallyStartRename");
		mFileRenameCount = (Preference) findPreference("fileRenameCount");
		mBuildVersion = (Preference) findPreference("buildVersion");
		mSendDebugReport = (Preference) findPreference("sendDebugReport");
		mLicensePref = (Preference) findPreference("licensePref");
		mDonatePref = (Preference) findPreference("donatePref");
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
	 * @return The update message if is present on the resources.
	 */
	private String getUpdateMessage() {
		String message = null;
		int id = DSCApplication.getAppContext().getResources().getIdentifier("update_message",
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
	 * @param sharedPreferences
	 *            The shared preference
	 * @param key
	 *            Key of changed preference
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		boolean doPrepareSummaries = true;
		if (DSCApplication.KEY_SERVICE_TYPE.equals(key)) {
			mApplication.checkRegisteredServiceType(false);
		} else if (DSCApplication.KEY_ENABLED_FOLDER_SCANNING.equals(key)) {
			mApplication.updateFolderObserverList();
		} else if (DSCApplication.KEY_LANGUAGE_CODE.equals(key)) {
			doPrepareSummaries = false;
			restartActivity();
		}
		if (doPrepareSummaries) {
			prepareSummaries();
		}
	}

	/**
	 * Restart this activity.
	 */
	private void restartActivity() {
		mApplication.initLocale();
		Intent i = DSCApplication.getAppContext().getPackageManager()
				.getLaunchIntentForPackage( DSCApplication.getAppContext().getPackageName() );
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
		String summary = DSCApplication.getAppContext().getString(
				R.string.define_file_name_pattern_desc, originalArr[0].getDemoBefore());
		mDefineFileNamePatterns.setSummary(summary);

		summary = "" + newFileName;
		summary += "_" + mApplication.getFormattedFileNameSuffix(0);
		summary += "." + originalArr[0].getDemoExtension();
		summary += ", " + newFileName;
		summary += "_" + mApplication.getFormattedFileNameSuffix(1);
		summary += "." + originalArr[0].getDemoExtension();

		summary = DSCApplication.getAppContext().getString(R.string.file_name_suffix_format_desc, summary);
		mFileNameSuffixFormat.setSummary(summary);
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
		if (mApplication.getSdkInt() >= 21) {
			mEnabledFolderScanning.setSummary(R.string.enable_filter_folder_desc_v21);
		}
		updateSelectedFolders();
		// renameFileDateType
		String arr[] = mApplication.getResources().getStringArray(
				R.array.rename_file_using_labels);
		mRenameFileDateType
				.setSummary(arr[mApplication.getRenameFileDateType()]);
		summary = getString(R.string.append_original_name_desc, newFileName);
		mAppendOriginalName.setSummary(summary);
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
		new SelectFileNamePatternDialog(this, mApplication).show();
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
			startRenameServiceManually();
		}
	}

	/**
	 * Start the rename service.
	 */
	private void startRenameServiceManually() {
		mApplication.setRenameFileRequested(true);
		if (!mApplication.isRenameFileTaskRunning()) {
			new RenameFileAsyncTask(mApplication, this).execute();
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
		b.putString(InfoActivity.FILE_NAME, "LICENSE.TXT");
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
	 * @param message
	 *            Message of the confirmation dialog.
	 * @param messageContainLink
	 *            A boolean flag which mark if the text contain links.
	 * @param confirmationId
	 *            ID of the process to be executed if confirmed.
	 */
	private void showConfirmationDialog(String message,
			boolean messageContainLink, final int confirmationId) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
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
			alertDialog.setNeutralButton(R.string.ok, null);
		} else {
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
	 * @param confirmationId
	 *            Received confirmation ID.
	 */
	protected void onConfirmation(int confirmationId) {
		if (confirmationId == ID_CONFIRMATION_DONATION) {
			confirmedDonationPage();
		} else if (confirmationId == ID_CONFIRMATION_RESET_RENAME_COUNTER) {
			confirmedResetFileRenameCounter();
		} else if (confirmationId == ID_CONFIRMATION_DEBUG_REPORT) {
			confirmedSendReport(DSCApplication.getAppContext().getString(R.string.send_debug_email_title));
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
	 * @param position
	 *            Current number of renamed files.
	 * @param message
	 *            The message to be displayed on progress dialog.
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
	 * @param count
	 *            Number of renamed files.
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
		showAlertDialog(message);
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
				.setIcon(android.R.drawable.ic_dialog_info)
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
				DSCApplication.getAppContext().getString(R.string.rename_shortcut_name));
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
	 * User just confirmed to send a report.
	 */
	private void confirmedSendReport(String emailTitle) {
		mApplication.createProgressDialog(this, this,
				DSCApplication.getAppContext().getString(R.string.send_debug_title));
		mApplication.showProgressDialog();
		String message = DSCApplication.getAppContext().getString(R.string.report_body);
		File logsFolder = mApplication.getLogsFolder();
		File archive = getLogArchive(logsFolder);
		String[] TO = { "ciubex@yahoo.com" };
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
	 * @param logsFolder The logs folder.
	 * @return The archive file which should contain the logs.
	 */
	private File getLogArchive(File logsFolder) {
		File logFile = mApplication.getLogFile();
		File logcatFile = getLogcatFile(logsFolder);
		return getArchives(new File[]{logFile, logcatFile}, logsFolder, "DSC_logs.zip");
	}

	/**
	 * Method used to build a ZIP archive with log files.
	 * @param files The log files to be added.
	 * @param logsFolder The logs folder where should be added the archive name.
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
	 * @param cacheFolder
	 *            Cache directory where are the logs.
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
					" (" + mApplication.getVersionCode() + ")"  + LS);
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

	private void writePre(Writer writer) {

	}

	/**
	 * This method is invoked when a child activity is finished and this
	 * activity is showed again
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_SEND_REPORT) {
//			mApplication.deleteLogFile();
		} else if (resultCode == RESULT_OK && requestCode == REQUEST_OPEN_DOCUMENT_TREE) {
			if (mApplication.getSdkInt() >= 21) {
				processActionOpenDocumentTree(data);
			}
		}
	}

	/**
	 * Process resulted data from new API regarding selected tree.
	 * @param resultData Resulted data from selected folder.
	 */
	@TargetApi(21)
	private void processActionOpenDocumentTree(Intent resultData) {
		Uri uri = resultData.getData();
		int flags = resultData.getFlags();
		int index = -1;
		mApplication.logD(TAG, "Selected on OpenDocumentTree uri: " + uri);
		if (selectFoldersListDialog != null) {
			index = selectFoldersListDialog.getSelectedIndex();
		}
		SelectedFolderModel selectedFolder = new SelectedFolderModel();
		selectedFolder.fromUri(uri, flags);
		MountVolume volume = mApplication.getMountVolumeByUuid(selectedFolder.getUuid());
		if (volume != null && !Utilities.isEmpty(volume.getPath())) {
			selectedFolder.setRootPath(volume.getPath());
			mApplication.logD(TAG, "Selected from OpenDocumentTree: " + selectedFolder);
			mApplication.setFolderScanning(index, selectedFolder);
			if (selectFoldersListDialog != null) {
				selectFoldersListDialog.updateSelectedFolders();
				mApplication.updateFolderObserverList();
			}
		}
	}
}
