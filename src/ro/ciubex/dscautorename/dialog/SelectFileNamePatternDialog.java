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

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.adpater.FileNamePatternListAdapter;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.SelectedFolderModel;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;

/**
 * This is the dialog used to show list of file name patterns.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class SelectFileNamePatternDialog extends BaseDialog implements CompoundButton.OnCheckedChangeListener {
	private Activity mParentActivity;
	private FileNamePatternListAdapter mAdapter;
	private ListView mListView;
	private Button mBtnAdd, mBtnDelete;
	private FileNamePatternEditorDialog mFileNamePatternEditorDialog;
	private int mCheckedCounter;

	public SelectFileNamePatternDialog(Context context, DSCApplication application, Activity parentActivity) {
		super(context, application);
		setContentView(R.layout.items_list_dialog_layout);
		mParentActivity = parentActivity;
		mAdapter = new FileNamePatternListAdapter(context, application);
		mAdapter.setOnCheckedChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.define_file_name_pattern_title);
		initDialog(BUTTON_CANCEL);
		mBtnAdd = (Button) findViewById(R.id.btnAdd);
		mBtnAdd.setOnClickListener(this);
		mBtnDelete = (Button) findViewById(R.id.btnDelete);
		mBtnDelete.setOnClickListener(this);
		initListView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mCheckedCounter = 0;
		prepareButtons();
	}

	/**
	 * Initialize the list view.
	 */
	private void initListView() {
		mListView = (ListView) findViewById(R.id.itemsList);
		mListView.setEmptyView(findViewById(R.id.emptyItemsList));
		mListView.setAdapter(mAdapter);
		mListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						clickOnItem(position);
					}
				});
	}

	/**
	 * Method invoked when a position in list is selected.
	 * 
	 * @param position
	 *            Selected position.
	 */
	private void clickOnItem(int position) {
		if (mFileNamePatternEditorDialog == null) {
			mFileNamePatternEditorDialog = new FileNamePatternEditorDialog(mContext, mApplication, mParentActivity, mAdapter);
		}
		mFileNamePatternEditorDialog.setInitModel(null);
		if (position == -2) {
			FileNameModel item = getFirstSelectedItem();
			if (item != null) {
				mFileNamePatternEditorDialog.setInitModel(item);
			}
			position = -1;
		}
		mFileNamePatternEditorDialog.setPosition(position);
		mFileNamePatternEditorDialog.show();
	}

	/**
	 * Update the selected folder.
	 * @param selectedFolder The selected folder.
	 */
	public void updateSelectedFolder(SelectedFolderModel selectedFolder) {
		if (mFileNamePatternEditorDialog != null) {
			mFileNamePatternEditorDialog.onFolderSelected(0, selectedFolder);
		}
	}

	/**
	 * Called when a view has been clicked.
	 * 
	 * @param view
	 *            The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		if (mBtnAdd == view) {
			clickOnItem(mCheckedCounter == 1 ? -2 : -1);
		} else if (mBtnDelete == view) {
			onDelete();
		} else {
			super.onClick(view);
		}
	}

	/**
	 * Obtain first selected item.
	 *
	 * @return First selected item or null.
	 */
	private FileNameModel getFirstSelectedItem() {
		FileNameModel item;
		for (int i = 0; i < mAdapter.getCount(); i++) {
			item = mAdapter.getItem(i);
			if (item.isSelected()) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Invoked by the Delete button.
	 */
	private void onDelete() {
		int i = 0, k = 0, len = mAdapter.getCount();
		FileNameModel item;
		for (i = 0; i < len; i++) {
			item = mAdapter.getItem(i);
			if (item.isSelected()) {
				k++;
			}
		}
		if (k == 0) {
			showAlertDialog(R.string.define_file_name_pattern_title,
					mApplication.getApplicationContext().getString(R.string.file_name_pattern_list_no_selection));
		} else if (k == len) {
			showAlertDialog(R.string.define_file_name_pattern_title,
					mApplication.getApplicationContext().getString(R.string.file_name_pattern_list_all_selected));
		} else {
			showConfirmationDialog(R.string.define_file_name_pattern_title,
					mApplication.getApplicationContext().getString(R.string.file_name_pattern_list_confirmation), 0,
					null);
		}
	}

	@Override
	protected void onConfirmation(boolean positive, int confirmationId,
			Object anObject) {
		if (positive) {
			int i, len = mAdapter.getCount();
			StringBuilder sb = new StringBuilder();
			FileNameModel item;
			for (i = 0; i < len; i++) {
				item = mAdapter.getItem(i);
				if (item.isSelected()) {
					mCheckedCounter--;
				} else {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(item.toString());
				}
			}
			mApplication.saveFileNamePattern(sb.toString());
			mAdapter.updateAdapterList();
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
			mCheckedCounter++;
		} else if (mCheckedCounter > 0) {
			mCheckedCounter--;
		}
		prepareButtons();
	}

	/**
	 * Prepare the buttons.
	 */
	private void prepareButtons() {
		mBtnDelete.setEnabled(mCheckedCounter > 0);
		mBtnAdd.setText(mCheckedCounter == 1 ? R.string.copy : R.string.add);
	}
}
