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
import ro.ciubex.dscautorename.adpater.FilePrefixListAdapter;
import ro.ciubex.dscautorename.model.FilePrefix;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Define a dialog editor for a prefixes.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class PrefixesEditorDialog extends BaseDialog {
	private FilePrefixListAdapter mAdapter;
	private EditText mEditPrefixFrom, mEditPrefixTo;
	private TextView mPrefixInfo;
	private int mPosition;
	private Date mNow;
	private FilePrefix[] mPrefixes;

	public PrefixesEditorDialog(Context context, DSCApplication application,
			FilePrefixListAdapter adapter, int position) {
		super(context, application);
		setContentView(R.layout.prefix_editor_dialog_layout);
		mAdapter = adapter;
		mPosition = position;
		mPrefixes = mApplication.getOriginalFilePrefix();
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
		updateDialogTitle();
		initValues();
		updatePrefixInfo();
	}

	/**
	 * Update the title text for this dialog's window.
	 */
	private void updateDialogTitle() {
		setTitle(mPosition == -1 ? R.string.prefix_dialog_title_add
				: R.string.prefix_dialog_title_editor);
	}

	/**
	 * Initialize dialog controls.
	 */
	private void initControls() {
		mEditPrefixFrom = (EditText) findViewById(R.id.editPrefixFrom);
		mEditPrefixTo = (EditText) findViewById(R.id.editPrefixTo);
		mPrefixInfo = (TextView) findViewById(R.id.prefixInfo);
		prepareEditTextField(mEditPrefixFrom);
		prepareEditTextField(mEditPrefixTo);
	}

	/**
	 * Initialize the fields with proper values.
	 */
	private void initValues() {
		if (mPosition > -1) {
			FilePrefix filePrefix = mApplication.getOriginalFilePrefix()[mPosition];
			mEditPrefixFrom.setText(filePrefix.getBefore());
			mEditPrefixTo.setText(filePrefix.getAfter());
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
				updatePrefixInfo();
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
	 * Method used to update the prefix info view.
	 */
	private void updatePrefixInfo() {
		String prefBefore = mEditPrefixFrom.getEditableText().toString();
		if (prefBefore.length() < 1) {
			prefBefore = mContext.getString(R.string.original_file_prefix_from);
		}
		prefBefore = prefBefore.trim();
		String prefAfter = mEditPrefixTo.getEditableText().toString().trim();
		String text = validatePrefixes(prefBefore, prefAfter);
		btnOk.setEnabled(text == null);
		if (text == null) {
			String newFileName = prefAfter + mApplication.getFileName(mNow);
			String ext = mApplication.getDemoExtension(prefBefore);
			text = mContext.getString(R.string.prefix_dialog_prefix_bottom,
					prefBefore, newFileName, ext);
		}
		mPrefixInfo.setText(text);
	}

	/**
	 * Validate the prefixes.
	 * 
	 * @param before
	 *            The original file name prefix.
	 * @param after
	 *            The new file name prefix.
	 * @return Null if both prefixes are valid, otherwise a validation error
	 *         message.
	 */
	private String validatePrefixes(String before, String after) {
		Locale locale = mApplication.getLocale();
		String str1, str11 = before.toLowerCase(locale);
		String str2 = after.toLowerCase(locale);
		int index, length = mPrefixes.length;
		FilePrefix filePrefix;
		if (str11.length() < 1) {
			return mContext
					.getString(R.string.prefix_validation_error_old_empty);
		} else {
			for (index = 0; index < length; index++) {
				filePrefix = mPrefixes[index];
				str1 = filePrefix.getBefore().toLowerCase(locale);
				if (str11.indexOf(str1) == 0 && index != mPosition) {
					return mContext
							.getString(R.string.prefix_validation_error_old_prefix);
				}
				if (str2.length() > 1 && str2.indexOf(str1) == 0) {
					return mContext
							.getString(R.string.prefix_validation_error_new_prefix);
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
			savePrefix();
			mAdapter.updatePrefixes();
			mAdapter.notifyDataSetChanged();
		}
		super.onClick(view);
	}

	/**
	 * Save edited prefix.
	 */
	private void savePrefix() {
		String prefBefore = mEditPrefixFrom.getEditableText().toString();
		if (prefBefore.length() < 1) {
			prefBefore = mContext.getString(R.string.original_file_prefix_from);
		}
		String prefAfter = mEditPrefixTo.getEditableText().toString();
		mApplication.saveFilePrefix(
				new FilePrefix(prefBefore + ":" + prefAfter), mPosition);
	}

}
