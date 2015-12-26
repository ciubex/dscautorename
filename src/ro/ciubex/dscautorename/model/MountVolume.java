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

import java.io.File;

import android.content.Context;
import android.os.Environment;

/**
 * This class is a lighter copy of StorageVolume.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class MountVolume {
	private String mId;
	private String mUuid;
	private int mStorageId;
	private int mDescriptionId;
	private String mDescription;
	private File mPath;
	private String mWrongPath;
	private boolean mRemovable;
	private boolean mPrimary;
	private boolean mEmulated;
	private String mState;

	public String getId() {
		return mId;
	}

	public void setId(String id) {
		this.mId = id;
	}

	public void setUuid(String uuid) {
		mUuid = uuid;
	}

	public String getUuid() {
		return mUuid;
	}

	/**
	 * Returns the MTP storage ID for the volume.
	 *
	 * @return MTP storage ID
	 */
	public int getStorageId() {
		return mStorageId;
	}

	/**
	 * The MTP storage ID for the volume.
	 *
	 * @param storageId
	 *            MTP storage ID
	 */
	public void setStorageId(int storageId) {
		this.mStorageId = storageId;
	}

	/**
	 * Returns a user visible description of the volume.
	 *
	 * @param context
	 *            Current application context.
	 * @return The volume description.
	 */
	public String getDescription(Context context) {
		return context.getResources().getString(mDescriptionId);
	}

	public int getDescriptionId() {
		return mDescriptionId;
	}

	public void setDescriptionId(int descriptionId) {
		this.mDescriptionId = descriptionId;
	}

	/**
	 * Returns a user visible description of the volume.
	 *
	 * @return the volume description
	 */
	public String getDescription() {
		return mDescription;
	}

	/**
	 * @param description
	 *            the mDescription to set
	 */
	public void setDescription(String description) {
		this.mDescription = description;
	}

	/**
	 * Returns the mount path for the volume.
	 *
	 * @return The mount path.
	 */
	public String getPath() {
		return mPath.toString();
	}

	public File getPathFile() {
		return mPath;
	}

	public void setPathFile(File path) {
		this.mPath = path;
	}

	public String getWrongPath() {
		return mWrongPath;
	}

	public void setWrongPath(String wrongPath) {
		this.mWrongPath = wrongPath;
	}

	/**
	 * Returns true if the volume is removable.
	 *
	 * @return Is removable.
	 */
	public boolean isRemovable() {
		return mRemovable;
	}

	/**
	 * Set volume removable flag.
	 *
	 * @param removable
	 *            Volume removable flag to set.
	 */
	public void setRemovable(boolean removable) {
		this.mRemovable = removable;
	}

	public boolean isPrimary() {
		return mPrimary;
	}

	public void setPrimary(boolean primary) {
		this.mPrimary = primary;
	}

	/**
	 * @return the emulated
	 */
	public boolean isEmulated() {
		return mEmulated;
	}

	/**
	 * @param emulated
	 *            the emulated to set
	 */
	public void setEmulated(boolean emulated) {
		this.mEmulated = emulated;
	}

	/**
	 * @return the volume State
	 */
	public String getState() {
		return mState;
	}

	/**
	 * @param state the volume State to set
	 */
	public void setState(String state) {
		this.mState = state;
	}

	/**
	 * Check if the volume is mounted.
	 * @return True if the volume is mounted.
	 */
	public boolean isMounted() {
		return Environment.MEDIA_MOUNTED.equals(mState)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(mState);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("MountVolume [");
		builder.append("mUuid=").append(mUuid)
				.append(", mStorageId=").append(mStorageId)
				.append(", mDescriptionId=").append(mDescriptionId)
				.append(", mPath=").append(mPath)
				.append(", mRemovable=").append(mRemovable)
				.append(", mPrimary=").append(mPrimary)
				.append(", mEmulated=").append(mEmulated)
				.append(", mState=").append(mState)
				.append("]");
		return builder.toString();
	}

}