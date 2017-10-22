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
package ro.ciubex.dscautorename.adpater;

import java.util.Date;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FileNameModel;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

/**
 * Here is defined file name pattern list adapter used on rename process.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileNamePatternListAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private DSCApplication mApplication;
	private FileNameModel[] mFileNameModels;
	private Date mNow;
    private CompoundButton.OnCheckedChangeListener mListener;

	public FileNamePatternListAdapter(Context context, DSCApplication application) {
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mApplication = application;
		updateAdapterList();
		mNow = new Date();
	}

	/**
	 * Update list models;
	 */
	public void updateAdapterList() {
		mFileNameModels = mApplication.getOriginalFileNamePattern();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return mFileNameModels.length;
	}

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        this.mListener = listener;
    }

    /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getItem(int)
         */
	@Override
	public FileNameModel getItem(int position) {
		if (position > -1 && position < getCount()) {
			return mFileNameModels[position];
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
		FileNameModel fileNameModel = getItem(position);
		if (view == null) {
			view = mInflater.inflate(R.layout.dlg_list_item_layout, parent,
					false);
			viewHolder = initViewHolder(view, fileNameModel);
			view.setTag(viewHolder);
            if (mListener != null) {
                viewHolder.checkBoxItem.setOnCheckedChangeListener(mListener);
            }
		} else {
			viewHolder = (ViewHolder) view.getTag();
			viewHolder.checkBoxItem.setTag(fileNameModel);
		}
		if (viewHolder != null && fileNameModel != null) {
			viewHolder.checkBoxItem.setChecked(fileNameModel.isSelected());
			viewHolder.firstItemText.setText(getFileNameBefore(fileNameModel));
			viewHolder.secondItemText.setText(getFileNameAfter(fileNameModel));
		}
		return view;
	}

	/**
	 * Obtain the 'before' file name.
	 * 
	 * @param fileNameModel
	 *            The file name pattern object.
	 * @return The before rename file name pattern.
	 */
	private String getFileNameBefore(FileNameModel fileNameModel) {
		return fileNameModel.getDemoBefore();
	}

	/**
	 * Obtain the 'after file' name.
	 * 
	 * @param fileNameModel
	 *            File name model object.
	 * @return After rename file name.
	 */
	private String getFileNameAfter(FileNameModel fileNameModel) {
		String ext = fileNameModel.getDemoExtension();
		return mApplication.getFileNameFormatted(fileNameModel.getAfter(), mNow) + "." + ext;
	}

	/**
	 * Initialize the item view holder elements.
	 * 
	 * @param view
	 *            The view used to obtain the holder elements.
	 * @param fileNameModel The file name model.
	 * @return The item view holder.
	 */
	private ViewHolder initViewHolder(View view, FileNameModel fileNameModel) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.checkBoxItem = (CheckBox) view
				.findViewById(R.id.checkBoxItem);
		viewHolder.checkBoxItem.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CheckBox cb = (CheckBox) v;
				Object item = cb.getTag();
				if (item instanceof FileNameModel) {
					((FileNameModel) item).setSelected(cb.isChecked());
				}
			}
		});
		viewHolder.checkBoxItem.setTag(fileNameModel);
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
