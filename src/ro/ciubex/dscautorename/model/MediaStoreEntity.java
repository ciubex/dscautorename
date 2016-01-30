/**
 * This file is part of DSCAutoRename application.
 * <p/>
 * Copyright (C) 2016 Claudiu Ciobotariu
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.dscautorename.model;

import android.net.Uri;

/**
 * The model used to store updated image or video DB informations.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class MediaStoreEntity {
	private int id;
	private Uri uri;
	private String data;
	private String oldData;
	private String title;
	private String displayName;
	private long size;

	public MediaStoreEntity() {	}

	public MediaStoreEntity(int id, Uri uri, String data, String oldData, String title, String displayName, long size) {
		this.id = id;
		this.uri = uri;
		this.data = data;
		this.oldData = oldData;
		this.title = title;
		this.displayName = displayName;
		this.size = size;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Uri getUri() {
		return uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getOldData() {
		return oldData;
	}

	public void setOldData(String oldData) {
		this.oldData = oldData;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
}
