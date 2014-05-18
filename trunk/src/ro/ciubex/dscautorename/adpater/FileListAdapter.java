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

import java.util.List;

import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FileItem;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This custom base adapter class is used to manage list items.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileListAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private List<FileItem> mFiles;

	public FileListAdapter(Context context, List<FileItem> files) {
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mFiles = files;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return mFiles.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public FileItem getItem(int position) {
		if (position > -1 && position < mFiles.size()) {
			return mFiles.get(position);
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
		if (view == null) {
			view = mInflater.inflate(R.layout.list_item_layout, null);
			viewHolder = initViewHolder(view);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}
		if (viewHolder != null) {
			FileItem fileItem = getItem(position);
			if (fileItem != null) {
				int iconResId = R.drawable.icon_generic_file;
				if (fileItem.isParent()) {
					iconResId = R.drawable.icon_folder_up;
				} else if (fileItem.isDirectory()) {
					iconResId = R.drawable.icon_folder;
				} else if (fileItem.isImage()) {
					iconResId = R.drawable.icon_picture;
				} else if (fileItem.isVideo()) {
					iconResId = R.drawable.icon_movies;
				} else if (fileItem.isAudio()) {
					iconResId = R.drawable.icon_music;
				}
				String text = "?";
				if (fileItem.isParent()) {
					text = "..";
				} else if (fileItem.getFile() != null) {
					text = fileItem.getFile().getName();
				}
				viewHolder.itemText.setText(text);
				viewHolder.itemIcon.setImageResource(iconResId);
			}
		}
		return view;
	}

	/**
	 * Initialize the item view holder elements.
	 * 
	 * @param view
	 *            The view used to obtain the holder elements.
	 * @return The item view holder.
	 */
	private ViewHolder initViewHolder(View view) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.itemIcon = (ImageView) view.findViewById(R.id.itemIcon);
		viewHolder.itemText = (TextView) view.findViewById(R.id.itemText);
		return viewHolder;
	}

	/**
	 * View holder for item list elements
	 * 
	 */
	static class ViewHolder {
		ImageView itemIcon;
		TextView itemText;
	}
}
