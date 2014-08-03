/**
 * 
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
	private ListView mPrefixList;
	private Button mBtnAdd, mBtnDelete;

	public SelectPrefixDialog(Context context, DSCApplication application) {
		super(context, application);
		setContentView(R.layout.prefix_list_dialog_layout);
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
		initPrefixesList();
	}

	/**
	 * Initialize the list view.
	 */
	private void initPrefixesList() {
		mPrefixList = (ListView) findViewById(R.id.prefixList);
		mPrefixList.setEmptyView(findViewById(R.id.emptyPrefixList));
		mPrefixList.setAdapter(mAdapter);
		mPrefixList
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						clickOnPrefix(position);
					}
				});
	}

	/**
	 * Method invoked when a position in list is selected.
	 * 
	 * @param position
	 *            Selected position.
	 */
	private void clickOnPrefix(int position) {
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
			clickOnPrefix(-1);
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
		FilePrefix fp;
		for (i = 0; i < len; i++) {
			fp = mAdapter.getItem(i);
			if (fp.isSelected()) {
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
			FilePrefix fp;
			for (i = 0; i < len; i++) {
				fp = mAdapter.getItem(i);
				if (!fp.isSelected()) {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(fp.toString());
				}
			}
			mApplication.saveFilePrefix(sb.toString());
			mAdapter.updatePrefixes();
			mAdapter.notifyDataSetChanged();
		}
	}

}
