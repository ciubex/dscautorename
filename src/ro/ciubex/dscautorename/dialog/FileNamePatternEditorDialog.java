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
package ro.ciubex.dscautorename.dialog;

import java.util.Date;
import java.util.Locale;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.adpater.FileNamePatternListAdapter;
import ro.ciubex.dscautorename.model.FileNameModel;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Define a dialog editor for a file name patterns.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileNamePatternEditorDialog extends BaseDialog {
	private FileNamePatternListAdapter mAdapter;
	private EditText mEditFileNamePatternFrom, mEditFileNamePatternTo;
	private TextView mFileNamePatternInfo;
	private int mPosition;
	private Date mNow;
	private FileNameModel[] mFileNameModels;
	private FileNameModel mFileNameModel;

	public FileNamePatternEditorDialog(Context context, DSCApplication application,
									   FileNamePatternListAdapter adapter, int position) {
		super(context, application);
		setContentView(R.layout.file_name_pattern_editor_dialog_layout);
		mAdapter = adapter;
		mPosition = position;
		mFileNameModels = mApplication.getOriginalFileNamePattern();
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onStart()
	 */
	@Override
	protected void onStart() {
		mNow = new Date();
		mFileNameModel = new FileNameModel(DSCApplication.getAppContext().getString(R.string.default_file_name_pattern));
		updateDialogTitle();
		initValues();
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
		prepareEditTextField(mEditFileNamePatternFrom);
		prepareEditTextField(mEditFileNamePatternTo);
	}

	/**
	 * Initialize the fields with proper values.
	 */
	private void initValues() {
		if (mPosition > -1) {
			FileNameModel fileNameModel = mApplication.getOriginalFileNamePattern()[mPosition];
			mEditFileNamePatternFrom.setText(fileNameModel.getBefore());
			mEditFileNamePatternTo.setText(fileNameModel.getAfter());
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
		if (prefBefore.length() < 1) {
			prefBefore = DSCApplication.getAppContext().getString(R.string.original_file_name_pattern_from);
		}
		prefBefore = prefBefore.trim();
		String prefAfter = mEditFileNamePatternTo.getEditableText().toString().trim();
		String text = validateFileNamePatterns(prefBefore, prefAfter);
		btnOk.setEnabled(text == null);
		if (text == null) {
			mFileNameModel.setBefore(prefBefore);
			if (prefAfter.length() < 1) {
				prefAfter = mFileNameModel.getAfter();
			} else {
				mFileNameModel.setAfter(prefAfter);
			}
			String newFileName = mApplication.getFileNameFormatted(prefAfter, mNow);
			newFileName += "." + mFileNameModel.getDemoExtension();
			text = DSCApplication.getAppContext().getString(R.string.file_name_pattern_dialog_bottom,
					mFileNameModel.getDemoBefore(), newFileName);
		}
		mFileNamePatternInfo.setText(text);
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
		String str2 = after.toLowerCase(locale);
		int index, length = mFileNameModels.length;
		FileNameModel fileNameModel;
		if (str11.length() < 1) {
			return DSCApplication.getAppContext()
					.getString(R.string.file_name_pattern_validation_error_old_empty);
		} else {
			index = str11.indexOf('.');
			if (index == 1 && str11.charAt(0) == '*') {
				return DSCApplication.getAppContext().getString(R.string.file_name_pattern_validation_error_generic);
			} else {
				for (index = 0; index < length; index++) {
					fileNameModel = mFileNameModels[index];
					str1 = fileNameModel.getBefore().toLowerCase(locale);
					if (str11.indexOf(str1) == 0 && index != mPosition) {
						return DSCApplication.getAppContext()
								.getString(R.string.file_name_pattern_validation_error_old);
					}
					if (str2.length() > 1 && str2.indexOf(str1) == 0) {
						return DSCApplication.getAppContext()
								.getString(R.string.file_name_pattern_validation_error_new);
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
			mApplication.saveFileNamePattern(mFileNameModel, mPosition);
			mAdapter.updateAdapterList();
			mAdapter.notifyDataSetChanged();
		}
		super.onClick(view);
	}
}
