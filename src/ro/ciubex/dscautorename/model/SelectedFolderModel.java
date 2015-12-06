/**
 * This file is part of DSCAutoRename application.
 *
 * Copyright (C) 2015 Claudiu Ciobotariu
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

import android.net.Uri;

import java.io.File;

import ro.ciubex.dscautorename.DSCApplication;

/**
 * Created by claudiu on 08.04.2015.
 */
public class SelectedFolderModel {
	private static final String TAG = SelectedFolderModel.class.getName();
	private String mSchema;
	private String mAuthority;
	private String mUuid;
	private String mPath = "";
	private String mRootPath = "";
	private int mFlags;
	private boolean selected;

	public void fromString(DSCApplication application, String value) {
		int idx = value.length();
		if (idx > 0 && value.charAt(0) == '[' && value.charAt(idx-1) == ']') {
			String[] values = value.substring(1, idx-1).split(":");
			mSchema = values[0];
			mAuthority = values[1];
			mUuid = values[2];
			mPath = values[3];
			mFlags = getIntValue(application, values[4], value);
		} else {
			mPath = value;
			mFlags = 195;
		}
	}

	private int getIntValue(DSCApplication application, String value, String fullValue) {
		int result = 195;
		try {
			result = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			application.logE(TAG, "Exception on parsing for: " + value + " fullValue: " + fullValue, e);
		}
		return result;
	}

	public void fromUri(Uri uri, int flags) {
		mSchema = uri.getScheme();
		mAuthority = uri.getAuthority();
		String[] uriPathArr = uri.getPath().split(":");
		mUuid = extractUuid(uriPathArr[0]);
		if (uriPathArr.length > 1) {
			mPath = uriPathArr[1];
		}
		this.mFlags = flags;
	}

	private String extractUuid(String uirPath1) {
		int idx = uirPath1.lastIndexOf('/');
		if (idx > -1) {
			return uirPath1.substring( idx + 1 );
		}
		return uirPath1;
	}

	public String getSchema() {
		return mSchema;
	}

	public String getAuthority() {
		return mAuthority;
	}

	public String getUuid() {
		return mUuid;
	}

	public void setPath(String path) {
		this.mPath = path;
	}

	public String getPath() {
		return mPath;
	}

	public String getRootPath() {
		return mRootPath;
	}

	public void setRootPath(String rootPath) {
		this.mRootPath = rootPath;
	}

	public int getFlags() {
		return mFlags;
	}

	public boolean isNewModel() {
		return mSchema != null && mSchema.length() > 0;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		sb.append(mSchema);
		sb.append(':').append(mAuthority);
		sb.append(':').append(mUuid);
		sb.append(':').append(mPath);
		sb.append(':').append(mFlags);
		sb.append(']');
		return sb.toString();
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public Uri getUri() {
		return new Uri.Builder()
				.scheme(mSchema)
				.authority(mAuthority)
				.appendPath("tree")
				.appendPath(mUuid + ":" + mPath)
				.build();
	}

	public String getFullPath() {
		return (mRootPath.length() == 0 ? mPath : (mRootPath + File.separatorChar + mPath));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SelectedFolderModel folderModel = (SelectedFolderModel) o;

		if (!getFullPath().equals(folderModel.getFullPath())) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return getFullPath().hashCode();
	}
}
