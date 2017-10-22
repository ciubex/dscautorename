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
package ro.ciubex.dscautorename.model;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * @author Claudiu Ciobotariu
 * 
 */
public class FileNameModel {
	private String mBefore;
	private String mAfter;
	private boolean mSelected;
	private SelectedFolderModel mSelectedFolder;

	public FileNameModel(String string) {
		mSelectedFolder = new SelectedFolderModel();
		fromString(string);
	}

	private void fromString(String string) {
		String[] arr = string.split("\\|");
		if (arr.length > 0) {
			preparePatterns(arr[0]);
			if (arr.length > 1) {
				mSelectedFolder.fromString(arr[1]);
			}
		}
	}

	private void preparePatterns(String string) {
		String[] arr = string.split(":");
		switch (arr.length) {
			case 1:
				mBefore = arr[0];
				mAfter = "'PIC_'yyyyMMdd_HHmmss";
				break;
			case 2:
				mBefore = arr[0];
				mAfter = arr[1];
				break;
			default:
				mBefore = "DSC_*.JPG";
				mAfter = "\'PIC_\'yyyyMMdd_HHmmss";
		}
	}

	public void setBefore(String before) {
		this.mBefore = before;
	}

	public void setAfter(String after) {
		this.mAfter = after;
	}

	/**
	 * @return the before
	 */
	public String getBefore() {
		return mBefore;
	}

	/**
	 * @return the after
	 */
	public String getAfter() {
		return mAfter;
	}

	public SelectedFolderModel getSelectedFolder() {
		return mSelectedFolder;
	}

	public void setSelectedFolder(SelectedFolderModel selectedFolder) {
		mSelectedFolder = selectedFolder;
	}

	@Override
	public String toString() {
		return mBefore + ":" + mAfter +
				(mSelectedFolder != null ? ("|" + mSelectedFolder.toString()) : "");
	}

	public boolean isSelected() {
		return mSelected;
	}

	public void setSelected(boolean selected) {
		this.mSelected = selected;
	}

	public String getDemoExtension() {
		int i = mBefore.indexOf('.');
		if (i > -1) {
			return mBefore.substring(i + 1);
		}
		return "JPG";
	}

	public String getDemoBefore() {
		String name = mBefore;
		if (name.indexOf('*') > -1) {
			name = name.replaceAll("\\*", "001");
		}
		if (name.indexOf('?') > -1) {
			name = name.replaceAll("\\?", "0");
		}
		if (name.indexOf('.') == -1) {
			name += ".JPG";
		}
		return name;
	}
}
