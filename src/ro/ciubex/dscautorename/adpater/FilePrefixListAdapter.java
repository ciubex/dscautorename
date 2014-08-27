/**
 * 
 */
package ro.ciubex.dscautorename.adpater;

import java.util.Date;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FilePrefix;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Here is defined file prefix list adapter used on rename process.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FilePrefixListAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private DSCApplication mApplication;
	private FilePrefix[] mPrefixes;
	private Date mNow;

	public FilePrefixListAdapter(Context context, DSCApplication application) {
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mApplication = application;
		updatePrefixes();
		mNow = new Date();
	}

	/**
	 * Update prefixes;
	 */
	public void updatePrefixes() {
		mPrefixes = mApplication.getOriginalFilePrefix();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return mPrefixes.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public FilePrefix getItem(int position) {
		if (position > -1 && position < getCount()) {
			return mPrefixes[position];
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int id) {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder viewHolder = null;
		FilePrefix filePrefix = getItem(position);
		if (view == null) {
			view = mInflater.inflate(R.layout.dlg_list_item_layout, parent,
					false);
			viewHolder = initViewHolder(view, filePrefix);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
			viewHolder.checkBoxItem.setTag(filePrefix);
		}
		if (viewHolder != null && filePrefix != null) {
			viewHolder.checkBoxItem.setChecked(filePrefix.isSelected());
			viewHolder.firstItemText.setText(getBeforePrefix(filePrefix));
			viewHolder.secondItemText.setText(getAfterPrefix(filePrefix));
		}
		return view;
	}

	/**
	 * Obtain the 'before' file name.
	 * 
	 * @param filePrefix
	 *            The file prefix object.
	 * @return The before rename file prefix.
	 */
	private String getBeforePrefix(FilePrefix filePrefix) {
		String prefix = filePrefix.getBefore();
		return prefix + "001" + mApplication.getDemoExtension(prefix);
	}

	/**
	 * Obtain the 'after file' name.
	 * 
	 * @param filePrefix
	 *            File prefix object.
	 * @return After rename file prefix.
	 */
	private String getAfterPrefix(FilePrefix filePrefix) {
		String prefix = filePrefix.getAfter();
		return prefix + mApplication.getFileName(mNow)
				+ mApplication.getDemoExtension(filePrefix.getBefore());
	}

	/**
	 * Initialize the item view holder elements.
	 * 
	 * @param view
	 *            The view used to obtain the holder elements.
	 * @param position
	 * @return The item view holder.
	 */
	private ViewHolder initViewHolder(View view, FilePrefix filePrefix) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.checkBoxItem = (CheckBox) view
				.findViewById(R.id.checkBoxItem);
		viewHolder.checkBoxItem.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CheckBox cb = (CheckBox) v;
				Object item = cb.getTag();
				if (item instanceof FilePrefix) {
					((FilePrefix) item).setSelected(cb.isChecked());
				}
			}
		});
		viewHolder.checkBoxItem.setTag(filePrefix);
		viewHolder.firstItemText = (TextView) view
				.findViewById(R.id.firstItemText);
		viewHolder.secondItemText = (TextView) view
				.findViewById(R.id.secondItemText);
		return viewHolder;
	}

	/**
	 * View holder for item list elements
	 * 
	 */
	private class ViewHolder {
		CheckBox checkBoxItem;
		TextView firstItemText;
		TextView secondItemText;
	}

}
