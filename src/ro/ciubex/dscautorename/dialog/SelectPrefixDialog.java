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
import ro.ciubex.dscautorename.adpater.FilePrefixListAdapter;
import ro.ciubex.dscautorename.model.FilePrefix;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

/**
 * This is the dialog used to show list of prefixes.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class SelectPrefixDialog extends BaseDialog {
	private FilePrefixListAdapter mAdapter;
	private ListView mListView;
	private Button mBtnAdd, mBtnDelete;

	public SelectPrefixDialog(Context context, DSCApplication application) {
		super(context, application);
		setContentView(R.layout.items_list_dialog_layout);
		mAdapter = new FilePrefixListAdapter(context, application);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.define_file_prefix_title);
		initDialog(BUTTON_CANCEL);
		mBtnAdd = (Button) findViewById(R.id.btnAdd);
		mBtnAdd.setOnClickListener(this);
		mBtnDelete = (Button) findViewById(R.id.btnDelete);
		mBtnDelete.setOnClickListener(this);
		initListView();
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
		new PrefixesEditorDialog(mContext, mApplication, mAdapter, position)
				.show();
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
			clickOnItem(-1);
		} else if (mBtnDelete == view) {
			onDelete();
		} else {
			super.onClick(view);
		}
	}

	/**
	 * Invoked by the Delete button.
	 */
	private void onDelete() {
		int i = 0, k = 0, len = mAdapter.getCount();
		FilePrefix item;
		for (i = 0; i < len; i++) {
			item = mAdapter.getItem(i);
			if (item.isSelected()) {
				k++;
			}
		}
		if (k == 0) {
			showAlertDialog(R.string.define_file_prefix_title,
					mContext.getString(R.string.prefix_list_no_selection));
		} else if (k == len) {
			showAlertDialog(R.string.define_file_prefix_title,
					mContext.getString(R.string.prefix_list_all_selected));
		} else {
			showConfirmationDialog(R.string.define_file_prefix_title,
					mContext.getString(R.string.prefix_list_confirmation), 0,
					null);
		}
	}

	@Override
	protected void onConfirmation(boolean positive, int confirmationId,
			Object anObject) {
		if (positive) {
			int i = 0, len = mAdapter.getCount();
			StringBuilder sb = new StringBuilder();
			FilePrefix item;
			for (i = 0; i < len; i++) {
				item = mAdapter.getItem(i);
				if (!item.isSelected()) {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(item.toString());
				}
			}
			mApplication.saveFilePrefix(sb.toString());
			mAdapter.updatePrefixes();
			mAdapter.notifyDataSetChanged();
		}
	}

}
