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
package ro.ciubex.dscautorename.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * This class define a dialog activity for the manually launched rename service.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class RenameDlgActivity extends Activity implements
		RenameFileAsyncTask.Listener {
	private DSCApplication mApplication;
	private TextView mRenameProgressMessage;
	private ProgressBar mRenameProgressBar;
	private Button mCancelButton;
	private boolean mValidRenameAction;
	private List<Uri> mFileUris;

	/**
	 * Method called when the activity is created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mApplication = (DSCApplication) getApplication();
		applyApplicationTheme();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rename_dialog_layout);
		initView();
		mValidRenameAction = false;
		mFileUris = null;
		initIntent();
	}

	/**
	 * Apply application theme.
	 */
	private void applyApplicationTheme() {
		this.setTheme(mApplication.getApplicationDialogTheme());
	}

	/**
	 * Initialize the view.
	 */
	private void initView() {
		mRenameProgressMessage = (TextView) findViewById(R.id.renameProgressMessage);
		mRenameProgressBar = (ProgressBar) findViewById(R.id.renameProgressBar);
		mCancelButton = (Button) findViewById(R.id.btnCancel);
		mCancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doStopRenameService();
			}
		});
	}

	/**
	 * Initialize the intent data.
	 */
	private void initIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		if (Intent.ACTION_MAIN.equals(action)) {
			mValidRenameAction = true;
		} else if (type != null && (type.startsWith("image/") || type.startsWith("video/"))) {
			if (Intent.ACTION_SEND.equals(action)) {
				handleActionSend(intent);
			} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
				handleActionSendMultiple(intent);
			}
		}
	}

	/**
	 * Handle ACTION_SEND for one file.
	 *
	 * @param intent The intent data to extract the file.
	 */
	private void handleActionSend(Intent intent) {
		Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (fileUri != null) {
			mValidRenameAction = true;
			mFileUris = new ArrayList<>(1);
			mFileUris.add(fileUri);
		}
	}

	/**
	 * Handle ACTION_SEND_MULTIPLE for more than one file.
	 *
	 * @param intent The intent data to extract the files.
	 */
	private void handleActionSendMultiple(Intent intent) {
		ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		if (fileUris != null) {
			mValidRenameAction = true;
			mFileUris = new ArrayList<>(fileUris);
		}
	}

	/**
	 * This method is invoked when the activity is started
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (mValidRenameAction) {
			if (!Utilities.isEmpty(mFileUris) && mApplication.getSdkInt() > 18) {
				showServiceStartConfirmationDialog();
			} else if (mApplication.hideRenameServiceStartConfirmation()) {
				doStartRenameService();
			} else {
				showServiceStartConfirmationDialog();
			}
		}
	}

	/**
	 * Invoked when the activity is put on pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
		doStopRenameService();
	}

	/**
	 * This method will show a confirmation popup.
	 */
	private void showServiceStartConfirmationDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setMessage(mApplication.getSdkInt() > 18 ?
						R.string.confirmation_rename_question_v19
						: R.string.confirmation_rename_question)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								doStartRenameService();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								doFinish();
							}
						}).show();
	}

	/**
	 * This method will start the rename service.
	 */
	private void doStartRenameService() {
		mApplication.setRenameFileRequested(true);
		if (!mApplication.isRenameFileTaskRunning()) {
			new RenameFileAsyncTask(mApplication, this, true, mFileUris).execute();
		}
	}

	/**
	 * Method invoked to stop the rename service.
	 */
	private void doStopRenameService() {
		if (mApplication.isRenameFileTaskRunning()) {
			mApplication.setRenameFileTaskCanceled(true);
		} else {
			doFinish();
		}
	}

	/**
	 * Method used to close this activity.
	 */
	private void doFinish() {
		finish();
	}

	/**
	 * Method invoked when the rename file async task is started.
	 */
	@Override
	public void onTaskStarted() {

	}

	/**
	 * Method invoked from the rename task when an update is required.
	 * 
	 * @param position
	 *            Current number of renamed files.
	 * @param count
	 *            The number of total files to be renamed.
	 * @param message
	 *            The message to be displayed on progress dialog.
	 */
	@Override
	public void onTaskUpdate(int position, int count, String message) {
		mRenameProgressMessage.setText(message);
		if (position == 0) {
			mRenameProgressBar.setIndeterminate(false);
			mRenameProgressBar.setMax(count);
		}
		mRenameProgressBar.setProgress(position);
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
			message = DSCApplication.getAppContext()
					.getString(R.string.manually_file_rename_count_0);
			break;
		case 1:
			message = DSCApplication.getAppContext()
					.getString(R.string.manually_file_rename_count_1);
			break;
		default:
			message = DSCApplication.getAppContext()
					.getString(R.string.manually_file_rename_count_more, count);
			break;
		}
		AlertDialog.Builder dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setMessage(message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(R.string.ok,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								doFinish();
							}
						});
		dialog.show();
	}

}
