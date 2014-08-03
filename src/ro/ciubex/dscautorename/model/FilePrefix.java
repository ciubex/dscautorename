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

/**
 * @author Claudiu Ciobotariu
 * 
 */
public class FilePrefix {
	private String mBefore;
	private String mAfter;
	private boolean selected;

	public FilePrefix(String string) {
		fromString(string);
	}

	private void fromString(String string) {
		String[] arr = string.split(":");
		switch (arr.length) {
		case 1:
			mBefore = arr[0];
			mAfter = "";
			break;
		case 2:
			mBefore = arr[0];
			mAfter = arr[1];
			break;
		default:
			mBefore = "DSC_";
			mAfter = "";
		}
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

	public String toString() {
		return mBefore + ":" + mAfter;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
