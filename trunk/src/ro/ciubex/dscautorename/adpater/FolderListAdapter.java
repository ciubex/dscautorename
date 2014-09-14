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

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FolderItem;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * @author Claudiu
 * 
 */
public class FolderListAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private DSCApplication mApplication;
	private FolderItem[] mFolders;

	/**
	 * 
	 */
	public FolderListAdapter(Context context, DSCApplication application) {
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mApplication = application;
		updateFolders();
	}

	/**
	 * Update folders.
	 */
	public void updateFolders() {
		mFolders = mApplication.getFoldersScanning();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return mFolders.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public FolderItem getItem(int position) {
		if (position > -1 && position < getCount()) {
			return mFolders[position];
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
		FolderItem item = getItem(position);
		if (view == null) {
			view = mInflater.inflate(R.layout.dlg_folders_list_layout, parent,
					false);
			viewHolder = initViewHolder(view, item);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
			viewHolder.checkBoxItem.setTag(item);
		}
		if (viewHolder != null && item != null) {
			viewHolder.checkBoxItem.setChecked(item.isSelected());
			viewHolder.itemText.setText(item.toString());
		}
		return view;
	}

	/**
	 * Initialize the item view holder elements.
	 * 
	 * @param view
	 *            The view used to obtain the holder elements.
	 * @param position
	 * @return The item view holder.
	 */
	private ViewHolder initViewHolder(View view, FolderItem folderItem) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.checkBoxItem = (CheckBox) view
				.findViewById(R.id.checkBoxFolder);
		viewHolder.checkBoxItem.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CheckBox cb = (CheckBox) v;
				Object item = cb.getTag();
				if (item instanceof FolderItem) {
					((FolderItem) item).setSelected(cb.isChecked());
				}
			}
		});
		viewHolder.checkBoxItem.setTag(folderItem);
		viewHolder.itemText = (TextView) view.findViewById(R.id.textFolderName);
		return viewHolder;
	}

	/**
	 * View holder for item list elements
	 * 
	 */
	private class ViewHolder {
		CheckBox checkBoxItem;
		TextView itemText;
	}
}
