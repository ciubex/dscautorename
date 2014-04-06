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
 * The model used to store image DB informations.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class DSCImage {
	private int mId;
	private String mData;

	public DSCImage(int id, String data) {
		this.mId = id;
		this.mData = data;
	}

	/**
	 * @return the id
	 */
	public int getmId() {
		return mId;
	}

	/**
	 * @return the data
	 */
	public String getmData() {
		return mData;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mId;
		return result;
	}

	/* (non-Javadoc)
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
		if (!(obj instanceof DSCImage)) {
			return false;
		}
		DSCImage other = (DSCImage) obj;
		if (mId != other.mId) {
			return false;
		}
		return true;
	}
}
