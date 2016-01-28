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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.activity.SettingsActivity;
import ro.ciubex.dscautorename.adpater.FolderListAdapter;
import ro.ciubex.dscautorename.model.SelectedFolderModel;

/**
 * A dialog used as a folder picker.
 *
 * @author Claudiu Ciobotariu
 */
public class SelectFoldersListDialog extends BaseDialog implements
		SelectFolderDialog.SelectFolderListener {
	private static final String TAG = SelectFoldersListDialog.class.getName();
	private Activity mParentActivity;
	private FolderListAdapter mAdapter;
	private ListView mListView;
	private TextView mItemsListNote;
	private Button mBtnAdd, mBtnDelete;
	private int mSelectedIndex;

	private static final int CONFIRMATION_DELETE_FOLDER = 1;
	private static final int CONFIRMATION_USE_INTERNAL_SELECT_FOLDER = 2;

	/**
	 * @param context
	 * @param application
	 */
	public SelectFoldersListDialog(Context context, DSCApplication application, Activity parentActivity) {
		super(context, application);
		this.mParentActivity = parentActivity;
		setContentView(R.layout.items_list_dialog_layout);
		mAdapter = new FolderListAdapter(context, application);
		mSelectedIndex = -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.folder_list_title);
		initDialog(BUTTON_CANCEL);
		mItemsListNote = (TextView) findViewById(R.id.itemsListNote);
		mBtnAdd = (Button) findViewById(R.id.btnAdd);
		mBtnAdd.setOnClickListener(this);
		mBtnDelete = (Button) findViewById(R.id.btnDelete);
		mBtnDelete.setOnClickListener(this);
		mItemsListNote.setVisibility(View.GONE);
		initListView();
	}

	/**
	 * Initialize the list view.
	 */
	private void initListView() {
		mListView = (ListView) findViewById(R.id.itemsList);
		mListView.setEmptyView(findViewById(R.id.emptyFolderList));
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
	 * @param position Selected position.
	 */
	private void clickOnItem(int position) {
		if (mApplication.getSdkInt() < 21) {
			useInternalSelectFolderDialog(position);
		} else {
			mSelectedIndex = position;
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
			mParentActivity.startActivityForResult(intent, SettingsActivity.REQUEST_OPEN_DOCUMENT_TREE);
		} catch (Exception e) {
			mApplication.logE(TAG, "startIntentActionOpenDocumentTree: " + e.getMessage(), e);
			showConfirmationDialog(R.string.folder_list_title,
					DSCApplication.getAppContext().getString(R.string.folder_list_no_open_document_tree_support),
					CONFIRMATION_USE_INTERNAL_SELECT_FOLDER,
					null);
		}
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param view The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		if (mBtnAdd == view) {
			clickOnItem(-1);
		} else if (mBtnDelete == view) {
			onDelete();
		} else {
			if (mParentActivity instanceof SettingsActivity) {
				((SettingsActivity) mParentActivity).updateSelectedFolders();
			}
			super.onClick(view);
		}
	}

	/**
	 * Invoked by the Delete button.
	 */
	private void onDelete() {
		int i, k = 0, len = mAdapter.getCount();
		SelectedFolderModel item;
		for (i = 0; i < len; i++) {
			item = mAdapter.getItem(i);
			if (item.isSelected()) {
				k++;
			}
		}
		if (k == 0) {
			showAlertDialog(R.string.folder_list_title,
					DSCApplication.getAppContext().getString(R.string.folder_list_no_selection));
		} else if (k == len) {
			showAlertDialog(R.string.folder_list_title,
					DSCApplication.getAppContext().getString(R.string.folder_list_all_selected));
		} else {
			showConfirmationDialog(R.string.folder_list_title,
					DSCApplication.getAppContext().getString(R.string.folder_list_confirmation), CONFIRMATION_DELETE_FOLDER,
					null);
		}
	}

	@Override
	protected void onConfirmation(boolean positive, int confirmationId,
								  Object anObject) {
		if (positive && CONFIRMATION_DELETE_FOLDER == confirmationId) {
			int i, len = mAdapter.getCount();
			SelectedFolderModel item;
			List<SelectedFolderModel> folderList = new ArrayList<SelectedFolderModel>();
			for (i = 0; i < len; i++) {
				item = mAdapter.getItem(i);
				if (!item.isSelected()) {
					folderList.add(item);
				}
			}
			mApplication.persistFolderList(folderList);
			mAdapter.updateFolders();
			mAdapter.notifyDataSetChanged();
		} else if (positive && CONFIRMATION_USE_INTERNAL_SELECT_FOLDER == confirmationId) {
			useInternalSelectFolderDialog(mSelectedIndex);
		}
	}

	/**
	 * Obtain selected index from the list.
	 *
	 * @return The selected index from the list.
	 */
	public int getSelectedIndex() {
		return mSelectedIndex;
	}

	/**
	 * Update the selected folder list.
	 */
	public void updateSelectedFolders() {
		mAdapter.updateFolders();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public String getSelectedFolder() {
		return null;
	}

	@Override
	public void onFolderSelected(int folderIndex, SelectedFolderModel selectedFolder) {
		mApplication.setFolderScanning(folderIndex, selectedFolder);
		updateSelectedFolders();
	}
}
