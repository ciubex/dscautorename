/**
 * This file is part of DSCAutoRename application.
 * 
 * Copyright (C) 2016 Claudiu Ciobotariu
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

/**
 * The model used to store image or video DB informations.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileRenameData {
	private int mId;
	private Uri mUri;
	private String mData;
	private String mTitle;
	private String mDisplayName;
	private long mDateAdded;
	private String mFileNamePatternBefore;
	private String mFileNamePatternAfter;
	private String mFullPath;
	private String mFileTitle;
	private String mFileTitleZero;
	private String mFileName;
	private String mPreviousFileName;
	private String mFileNameZero;
	private String mMoveToFolderPath;
	private String mMimeType;
	private long mSize;
	private File mParentFolder;

	public FileRenameData(int id, Uri uri, String data) {
		this(id, uri, data, null, null, null, -1, 0);
	}

	public FileRenameData(int id, Uri uri, String data, String title, String displayName, String mimeType, long dateAdded, long size) {
		this.mId = id;
		this.mUri = uri;
		this.mData = data;
		this.mTitle = title;
		this.mDisplayName = displayName;
		this.mMimeType = mimeType;
		this.mDateAdded = dateAdded;
		this.mSize = size;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return mId;
	}

	/**
	 * @return the Uri
	 */
	public Uri getUri() {
		return mUri;
	}

	/**
	 * @return the data
	 */
	public String getData() {
		return mData;
	}

	/**
	 * @return the mTitle
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * @return the mDisplayName
	 */
	public String getDisplayName() {
		return mDisplayName;
	}

	/**
	 * @return the mMimeType
	 */
	public String getMimeType() {
		return mMimeType;
	}

	/**
	 * @return the mDateAdded
	 */
	public long getDateAdded() {
		return mDateAdded;
	}

	public String getFileNamePatternBefore() {
		return mFileNamePatternBefore;
	}

	public void setFileNamePatternBefore(String fileNamePatternBefore) {
		this.mFileNamePatternBefore = fileNamePatternBefore;
	}

	public String getFileNamePatternAfter() {
		return mFileNamePatternAfter;
	}

	public void setFileNamePatternAfter(String fileNamePatternAfter) {
		this.mFileNamePatternAfter = fileNamePatternAfter;
	}

	/**
	 * @return the fullPath
	 */
	public String getFullPath() {
		return mFullPath;
	}

	/**
	 * @param fullPath the fullPath to set
	 */
	public void setFullPath(String fullPath) {
		this.mFullPath = fullPath;
	}

	/**
	 * @return the fileTitle
	 */
	public String getFileTitle() {
		return mFileTitle;
	}

	/**
	 * @param fileTitle the fileTitle to set
	 */
	public void setFileTitle(String fileTitle) {
		this.mFileTitle = fileTitle;
	}

	/**
	 * @return the fileTitleZero
	 */
	public String getFileTitleZero() {
		return mFileTitleZero;
	}

	/**
	 * @param fileTitleZero the fileTitleZero to set
	 */
	public void setFileTitleZero(String fileTitleZero) {
		this.mFileTitleZero = fileTitleZero;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return mFileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.mFileName = fileName;
	}

	/**
	 * @return the fileNameZero
	 */
	public String getFileNameZero() {
		return mFileNameZero;
	}

	/**
	 * @param fileNameZero the fileNameZero to set
	 */
	public void setFileNameZero(String fileNameZero) {
		this.mFileNameZero = fileNameZero;
	}

	public String getPreviousFileName() {
		return mPreviousFileName;
	}

	public void setPreviousFileName(String previousFileName) {
		this.mPreviousFileName = previousFileName;
	}

	public String getMoveToFolderPath() {
		return mMoveToFolderPath;
	}

	public void setMoveToFolderPath(String moveToFolderPath) {
		this.mMoveToFolderPath = moveToFolderPath;
	}

	public long getSize() {
		return mSize;
	}

	public void setSize(long size) {
		this.mSize = size;
	}

	public File getParentFolder() {
		return mParentFolder;
	}

	public void setParentFolder(File mParentFolder) {
		this.mParentFolder = mParentFolder;
	}

	/*
					 * (non-Javadoc)
					 *
					 * @see java.lang.Object#hashCode()
					 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mId;
		result = prime * result + ((mUri == null) ? 0 : mUri.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FileRenameData)) {
			return false;
		}
		FileRenameData other = (FileRenameData) obj;
		if (mId != other.mId) {
			return false;
		}
		if (mUri == null) {
			if (other.mUri != null) {
				return false;
			}
		} else if (!mUri.equals(other.mUri)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "FileRenameData{" +
				"mId=" + mId +
				", mUri=" + mUri +
				", mSize=" + mSize +
				", mData='" + mData + '\'' +
				", mTitle='" + mTitle + '\'' +
				", mDisplayName='" + mDisplayName + '\'' +
				", mMimeType='" + mMimeType + '\'' +
				", mDateAdded=" + mDateAdded +
				", mFileNamePatternBefore='" + mFileNamePatternBefore + '\'' +
				", mFileNamePatternAfter='" + mFileNamePatternAfter + '\'' +
				", mFullPath='" + mFullPath + '\'' +
				", mFileTitle='" + mFileTitle + '\'' +
				", mFileTitleZero='" + mFileTitleZero + '\'' +
				", mFileName='" + mFileName + '\'' +
				", mFileNameZero='" + mFileNameZero + '\'' +
				", mPreviousFileName='" + mPreviousFileName + '\'' +
				'}';
	}
}
