/**
 * 
 */
package ro.ciubex.dscautorename.dialog;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.adpater.FolderListAdapter;
import ro.ciubex.dscautorename.model.FolderItem;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

/**
 * @author Claudiu
 * 
 */
public class SelectFoldersListDialog extends BaseDialog {
	private FolderListAdapter mAdapter;
	private ListView mListView;
	private Button mBtnAdd, mBtnDelete;

	/**
	 * @param context
	 * @param application
	 */
	public SelectFoldersListDialog(Context context, DSCApplication application) {
		super(context, application);
		setContentView(R.layout.items_list_dialog_layout);
		mAdapter = new FolderListAdapter(context, application);
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
	 * @param position
	 *            Selected position.
	 */
	private void clickOnItem(int position) {
		new SelectFolderDialog(mContext, mApplication, mAdapter, position)
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
		FolderItem item;
		for (i = 0; i < len; i++) {
			item = mAdapter.getItem(i);
			if (item.isSelected()) {
				k++;
			}
		}
		if (k == 0) {
			showAlertDialog(R.string.folder_list_title,
					mContext.getString(R.string.folder_list_no_selection));
		} else if (k == len) {
			showAlertDialog(R.string.folder_list_title,
					mContext.getString(R.string.folder_list_all_selected));
		} else {
			showConfirmationDialog(R.string.folder_list_title,
					mContext.getString(R.string.folder_list_confirmation), 0,
					null);
		}
	}

	@Override
	protected void onConfirmation(boolean positive, int confirmationId,
			Object anObject) {
		if (positive) {
			int i = 0, len = mAdapter.getCount();
			StringBuilder sb = new StringBuilder();
			FolderItem item;
			for (i = 0; i < len; i++) {
				item = mAdapter.getItem(i);
				if (!item.isSelected()) {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(item.toString());
				}
			}
			mApplication.setFoldersScanning(sb.toString());
			mAdapter.updateFolders();
			mAdapter.notifyDataSetChanged();
		}
	}
}
