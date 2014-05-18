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

import java.io.File;

/**
 * This class is a wrapper for file object.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileItem {
	private boolean parent;
	private boolean directory;
	private boolean audio;
	private boolean image;
	private boolean video;
	private File file;

	/**
	 * @return the parent
	 */
	public boolean isParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(boolean parent) {
		this.parent = parent;
	}

	/**
	 * @return the directory
	 */
	public boolean isDirectory() {
		return directory;
	}

	/**
	 * @return the audio
	 */
	public boolean isAudio() {
		return audio;
	}

	/**
	 * @param audio the audio to set
	 */
	public void setAudio(boolean audio) {
		this.audio = audio;
	}

	/**
	 * @param directory
	 *            the directory to set
	 */
	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	/**
	 * @return the image
	 */
	public boolean isImage() {
		return image;
	}

	/**
	 * @param image
	 *            the image to set
	 */
	public void setImage(boolean image) {
		this.image = image;
	}

	/**
	 * @return the video
	 */
	public boolean isVideo() {
		return video;
	}

	/**
	 * @param video
	 *            the video to set
	 */
	public void setVideo(boolean video) {
		this.video = video;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public void setFile(File file) {
		this.file = file;
	}
}
