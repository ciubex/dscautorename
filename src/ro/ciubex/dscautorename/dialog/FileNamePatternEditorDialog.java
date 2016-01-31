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
package ro.ciubex.dscautorename.dialog;

import java.util.Date;
import java.util.Locale;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.activity.SettingsActivity;
import ro.ciubex.dscautorename.adpater.FileNamePatternListAdapter;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.util.RenamePatternsUtilities;
import ro.ciubex.dscautorename.util.Utilities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Define a dialog editor for a file name patterns.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileNamePatternEditorDialog extends BaseDialog implements
		SelectFolderDialog.SelectFolderListener {
	private static final String TAG = FileNamePatternEditorDialog.class.getName();
	private FileNamePatternListAdapter mAdapter;
	private Activity mParentActivity;
	private EditText mEditFileNamePatternFrom, mEditFileNamePatternTo;
	private TextView mFileNamePatternInfo, mSelectedFolderField;
	private int mPosition;
	private Date mNow;
	private FileNameModel[] mFileNameModels;
	private FileNameModel mDefaultFileName;
	private SelectedFolderModel mDefaultSelectedFolder;
	private RenamePatternsUtilities renamePatternsUtilities;
	private CheckBox mEnableMoveFiles;

	private static final int CONFIRMATION_USE_INTERNAL_SELECT_FOLDER = 2;

	public FileNamePatternEditorDialog(Context context, DSCApplication application,
									   Activity parentActivity,
									   FileNamePatternListAdapter adapter) {
		super(context, application);
		mParentActivity = parentActivity;
		setContentView(R.layout.file_name_pattern_editor_dialog_layout);
		mAdapter = adapter;
		renamePatternsUtilities = new RenamePatternsUtilities(mApplication);
	}

	/**
	 * Set the position of the renamed pattern.
	 * @param position The position of renamed pattern.
	 */
	public void setPosition(int position) {
		mPosition = position;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDialog(BUTTON_OK | BUTTON_CANCEL);
		initControls();
		initControlCommands();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onStart()
	 */
	@Override
	protected void onStart() {
		mNow = new Date();
		mDefaultFileName = new FileNameModel(mApplication, DSCApplication.getAppContext().getString(R.string.default_file_name_pattern));
		mFileNameModels = mApplication.getOriginalFileNamePattern();
		updateDialogTitle();
		initValues();
		updateMoveFilesFields();
		updateFileNamePatternInfo();
	}

	/**
	 * Update the title text for this dialog's window.
	 */
	private void updateDialogTitle() {
		setTitle(mPosition == -1 ? R.string.file_name_pattern_dialog_title_add
				: R.string.file_name_pattern_dialog_title_editor);
	}

	/**
	 * Initialize dialog controls.
	 */
	private void initControls() {
		mEditFileNamePatternFrom = (EditText) findViewById(R.id.editFileNamePatternFrom);
		mEditFileNamePatternTo = (EditText) findViewById(R.id.editFileNamePatternTo);
		mFileNamePatternInfo = (TextView) findViewById(R.id.fileNamePatternInfo);
		mEnableMoveFiles = (CheckBox) findViewById(R.id.enableMoveFiles);
		mSelectedFolderField = (TextView) findViewById(R.id.selectedFolderField);
		prepareEditTextField(mEditFileNamePatternFrom);
		prepareEditTextField(mEditFileNamePatternTo);
	}

	/**
	 * Initialize the control commands.
	 */
	private void initControlCommands() {
		mEnableMoveFiles.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mDefaultFileName.setSelectedFolder(mDefaultSelectedFolder);
				} else {
					mDefaultFileName.setSelectedFolder(null);
				}
				updateMoveFilesFields();
			}
		});
		mSelectedFolderField.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSelectFolder();
			}
		});
	}

	/**
	 * Initialize the fields with proper values.
	 */
	private void initValues() {
		boolean isMoveFiles = false;
		if (mPosition > -1) {
			FileNameModel fileNameModel = mApplication.getOriginalFileNamePattern()[mPosition];
			mEditFileNamePatternFrom.setText(fileNameModel.getBefore());
			mEditFileNamePatternTo.setText(fileNameModel.getAfter());
			isMoveFiles = Utilities.isMoveFiles(fileNameModel.getSelectedFolder());
			if (isMoveFiles) {
				mDefaultSelectedFolder = fileNameModel.getSelectedFolder();
				mDefaultFileName.setSelectedFolder(mDefaultSelectedFolder);
			} else {
				mDefaultSelectedFolder = new SelectedFolderModel();
				mDefaultSelectedFolder.setPath(mApplication.getDefaultFolderScanning());
			}
		} else {
			mEditFileNamePatternFrom.setText("");
			mEditFileNamePatternTo.setText("");
			mDefaultSelectedFolder = new SelectedFolderModel();
			mDefaultSelectedFolder.setPath(mApplication.getDefaultFolderScanning());
		}
		mEnableMoveFiles.setChecked(isMoveFiles);
	}

	/**
	 * Update move files fields: the checkbox and textview.
	 */
	private void updateMoveFilesFields() {
		boolean isMoveFiles = Utilities.isMoveFiles(mDefaultFileName.getSelectedFolder());
		if (isMoveFiles) {
			mSelectedFolderField.setVisibility(View.VISIBLE);
			String path = mDefaultFileName.getSelectedFolder().getFullPath();
			String text;
			if (Utilities.isEmpty(path)) {
				text = DSCApplication.getAppContext().getString(R.string.move_file_text_no_folder);
			} else {
				text = DSCApplication.getAppContext().getString(R.string.move_file_text_selected_folder,
						path);
			}
			mSelectedFolderField.setText(text);
		} else {
			mSelectedFolderField.setVisibility(View.GONE);
		}
	}

	/**
	 * Prepare an edit text field by add an change listener.
	 * 
	 * @param editText
	 *            The edit text field.
	 */
	private void prepareEditTextField(EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
									  int count) {
				updateFileNamePatternInfo();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
										  int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
	}

	/**
	 * Method used to update the file name pattern info view.
	 */
	private void updateFileNamePatternInfo() {
		String prefBefore = mEditFileNamePatternFrom.getEditableText().toString();
		prefBefore = prefBefore.trim();
		String prefAfter = mEditFileNamePatternTo.getEditableText().toString().trim();
		String text = validateFileNamePatterns(prefBefore, prefAfter);
		btnOk.setEnabled(text == null);
		if (text == null) {
			mDefaultFileName.setBefore(prefBefore);
			if (prefAfter.length() < 1) {
				prefAfter = mDefaultFileName.getAfter();
			} else {
				mDefaultFileName.setAfter(prefAfter);
			}

			text = DSCApplication.getAppContext().getString(R.string.file_name_pattern_dialog_bottom,
					mDefaultFileName.getDemoBefore(), getDemoFileName(prefAfter, mDefaultFileName.getDemoExtension()));
		}
		mFileNamePatternInfo.setText(text);
	}

	/**
	 * Obtain a demo file name based on a file name format.
	 * @param fileNameFormat The file name format.
	 * @return A demo file name.
	 */
	private String getDemoFileName(String fileNameFormat, String fileExtension) {
		String newFileName = mApplication.getFileNameFormatted(fileNameFormat, mNow);
		newFileName += "." + fileExtension;
		return newFileName;
	}

	/**
	 * Extract the file name extension from the provided file name string.
	 * @param fileName The file name string.
	 * @return The extension of the file name or default file name.
	 */
	private String getFileNameExtension(String fileName) {
		int index = fileName.lastIndexOf('.') + 1;
		return index > 0 ? fileName.substring(index) : mDefaultFileName.getDemoExtension();
	}

	/**
	 * Validate the file name patterns.
	 * 
	 * @param before
	 *            The original file name pattern.
	 * @param after
	 *            The new file name pattern.
	 * @return Null if both patterns are valid, otherwise a validation error
	 *         message.
	 */
	private String validateFileNamePatterns(String before, String after) {
		Locale locale = mApplication.getLocale();
		String str1, str11 = before.toLowerCase(locale);
		String fileNameExt;
		String newFileName;
		int index, length = mFileNameModels.length;
		FileNameModel fileNameModel;
		if (str11.length() < 1) {
			return DSCApplication.getAppContext()
					.getString(R.string.file_name_pattern_validation_error_old_empty);
		} else if (after.length() < 1) {
			return DSCApplication.getAppContext()
					.getString(R.string.file_name_pattern_validation_error_new_empty);
		} else {
			if (before.startsWith("*.")) {
				return DSCApplication.getAppContext().getString(R.string.file_name_pattern_validation_error_generic);
			} else {
				newFileName = getDemoFileName(after, getFileNameExtension(before));
				if (renamePatternsUtilities.matchFileNameBefore(before, newFileName) != -1) {
					return DSCApplication.getAppContext()
							.getString(R.string.file_name_pattern_validation_error_new);
				}
				for (index = 0; index < length; index++) {
					if (index == mPosition) {
						continue;
					}
					fileNameModel = mFileNameModels[index];
					str1 = fileNameModel.getBefore().toLowerCase(locale);
					// check for original name pattern
					if (str11.indexOf(str1) == 0) {
						return DSCApplication.getAppContext()
								.getString(R.string.file_name_pattern_validation_error_old);
					}
					fileNameExt = getFileNameExtension(str1);
					newFileName = getDemoFileName(fileNameModel.getAfter(), fileNameExt);
					if (renamePatternsUtilities.matchFileNameBefore(before,
							newFileName) == 0) {
						return DSCApplication.getAppContext()
								.getString(R.string.file_name_pattern_validation_error_original);
					}
					newFileName = getDemoFileName(after, fileNameExt);
					if (renamePatternsUtilities.matchFileNameBefore(fileNameModel.getBefore(),
							newFileName) == 0) {
						return DSCApplication.getAppContext()
								.getString(R.string.file_name_pattern_validation_error_rename);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Called when a view has been clicked.
	 * 
	 * @param view
	 *            The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		if (btnOk == view) {
			mApplication.saveFileNamePattern(mDefaultFileName, mPosition);
			mAdapter.updateAdapterList();
			mAdapter.notifyDataSetChanged();
		}
		super.onClick(view);
	}

	/**
	 * Method invoked when user try to select a folder to move renamed files.
	 */
	private void onSelectFolder() {
		if (mApplication.getSdkInt() < 21) {
			useInternalSelectFolderDialog(0);
		} else {
			startIntentActionOpenDocumentTree();
		}
	}

	/**
	 * Launch the internal select folder dialog.
	 *
	 * @param position The position from list of folders.
	 */
	private void useInternalSelectFolderDialog(int position) {
		new SelectFolderDialog(mContext, mApplication, this, position).show();
	}

	/**
	 * Initiate the folder chosen API
	 */
	@TargetApi(21)
	private void startIntentActionOpenDocumentTree() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
		try {
			mParentActivity.startActivityForResult(intent, SettingsActivity.REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER);
		} catch (Exception e) {
			mApplication.logE(TAG, "startIntentActionOpenDocumentTree: " + e.getMessage(), e);
			showConfirmationDialog(R.string.folder_list_title,
					DSCApplication.getAppContext().getString(R.string.folder_list_no_open_document_tree_support),
					CONFIRMATION_USE_INTERNAL_SELECT_FOLDER,
					null);
		}
	}

	@Override
	public String getSelectedFolder() {
		return mDefaultSelectedFolder.getFullPath();
	}

	@Override
	public void onFolderSelected(int folderIndex, SelectedFolderModel selectedFolder) {
		if (!Utilities.isEmpty(selectedFolder)) {
			mDefaultSelectedFolder = selectedFolder;
			mDefaultFileName.setSelectedFolder(mDefaultSelectedFolder);
		} else {
			mDefaultFileName.setSelectedFolder(null);
		}
		updateMoveFilesFields();
	}

	@Override
	protected void onConfirmation(boolean positive, int confirmationId,
								  Object anObject) {
		if (positive && CONFIRMATION_USE_INTERNAL_SELECT_FOLDER == confirmationId) {
			useInternalSelectFolderDialog(0);
		}
	}
}
