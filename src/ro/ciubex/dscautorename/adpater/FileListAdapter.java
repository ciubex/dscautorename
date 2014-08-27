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

import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.model.FileItem;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * This custom base adapter class is used to manage list items.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileListAdapter extends BaseAdapter {
	private Context mContext;
	private Drawable mIconGenericFile, mIconFolderUp, mIconFolder,
			mIconPicture, mIconMovies, mIconMusic;
	private static final int ICON_WIDTH = 48;
	private static final int ICON_HEIGHT = 48;
	private LayoutInflater mInflater;
	private List<FileItem> mFiles;

	public FileListAdapter(Context context) {
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mFiles = new ArrayList<FileItem>();
		initIcons();
	}

	/**
	 * Initialize the icons.
	 */
	private void initIcons() {
		Resources res = mContext.getResources();
		mIconGenericFile = res.getDrawable(R.drawable.icon_generic_file);
		mIconGenericFile.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
		mIconFolderUp = res.getDrawable(R.drawable.icon_folder_up);
		mIconFolderUp.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
		mIconFolder = res.getDrawable(R.drawable.icon_folder);
		mIconFolder.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
		mIconPicture = res.getDrawable(R.drawable.icon_picture);
		mIconPicture.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
		mIconMovies = res.getDrawable(R.drawable.icon_movies);
		mIconMovies.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
		mIconMusic = res.getDrawable(R.drawable.icon_music);
		mIconMusic.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
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
		if (position > -1 && position < getCount()) {
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
			view = mInflater.inflate(R.layout.list_item_layout, parent, false);
			viewHolder = initViewHolder(view);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}
		if (viewHolder != null) {
			FileItem fileItem = getItem(position);
			Drawable img = null;
			if (fileItem != null) {
				img = mIconGenericFile;
				if (fileItem.isParent()) {
					img = mIconFolderUp;
				} else if (fileItem.isDirectory()) {
					img = mIconFolder;
				} else if (fileItem.isImage()) {
					img = mIconPicture;
				} else if (fileItem.isVideo()) {
					img = mIconMovies;
				} else if (fileItem.isAudio()) {
					img = mIconMusic;
				}
				String text = "?";
				if (fileItem.isParent()) {
					text = "..";
				} else if (fileItem.getFile() != null) {
					text = fileItem.getFile().getName();
				}
				viewHolder.itemText.setText(text);
				viewHolder.itemText.setCompoundDrawables(img, null, null, null);
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
		viewHolder.itemText = (TextView) view.findViewById(R.id.itemText);
		return viewHolder;
	}

	/**
	 * View holder for item list elements
	 * 
	 */
	private class ViewHolder {
		TextView itemText;
	}

	/**
	 * Clear the list.
	 */
	public void clear() {
		if (!mFiles.isEmpty()) {
			mFiles.clear();
		}
	}

	/**
	 * Add a new file list to the adapter.
	 * 
	 * @param files
	 *            The new file list to be added.
	 */
	public void addAll(List<FileItem> files) {
		mFiles.addAll(files);
	}
}
