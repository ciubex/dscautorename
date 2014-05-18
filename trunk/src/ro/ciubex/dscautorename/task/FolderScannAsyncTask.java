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
package ro.ciubex.dscautorename.task;

import java.io.File;
import java.util.Collections;
import java.util.List;

import ro.ciubex.dscautorename.model.FileComparator;
import ro.ciubex.dscautorename.model.FileItem;
import android.os.AsyncTask;

/**
 * @author Claudiu Ciobotariu
 * 
 */
public class FolderScannAsyncTask extends AsyncTask<Void, Void, Integer> {

	/**
	 * Responder used on loading process.
	 */
	public interface Responder {
		public void startFolderScanning();

		public void endFolderScanning(int result);
	}

	private static final String[] audio = { "aif", "iff", "m3u", "m4a", "mid",
			"mp3", "mpa", "ogg", "ra", "wav", "wma" };
	private static final String[] images = { "bmp", "gif", "jpg", "jpeg",
			"png", "psd", "pspimage", "thm", "tif", "tiff", "yuv" };
	private static final String[] videos = { "3g2", "3gp", "asf", "asx", "avi",
			"flv", "mov", "mp4", "mpg", "rm", "swf", "vob", "wmv" };

	private File mCurrentFolder;
	private List<FileItem> mFiles;
	private Responder mResponder;

	public FolderScannAsyncTask(Responder responder, File currentFolder,
			List<FileItem> files) {
		this.mResponder = responder;
		this.mCurrentFolder = currentFolder;
		this.mFiles = files;
	}

	/**
	 * Method invoked on the UI thread before the task is executed.
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mResponder.startFolderScanning();
	}

	/**
	 * Method invoked on the UI thread after the background computation
	 * finishes.
	 */
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		mResponder.endFolderScanning(result);
	}

	@Override
	protected Integer doInBackground(Void... params) {
		int count = 0;
		File[] arr = mCurrentFolder.listFiles();
		if (arr == null) {
			return -1;
		}
		File file;
		FileItem fileItem;
		String ext;
		if (!mFiles.isEmpty()) {
			mFiles.clear();
		}
		for (count = 0; count < arr.length; count++) {
			file = arr[count];
			if (!file.isHidden()) {
				fileItem = new FileItem();
				fileItem.setFile(file);
				if (file.isDirectory()) {
					fileItem.setDirectory(true);
				} else {
					ext = getExtension(file);
					if (checkIsInList(ext, audio)) {
						fileItem.setAudio(true);
					} else if (checkIsInList(ext, images)) {
						fileItem.setImage(true);
					} else {
						fileItem.setVideo(checkIsInList(ext, videos));
					}
				}
				mFiles.add(fileItem);
			}
		}
		if (!mFiles.isEmpty()) {
			Collections.sort(mFiles, new FileComparator());
		}

		if (mCurrentFolder.getParentFile() != null) {
			fileItem = new FileItem();
			fileItem.setParent(true);
			fileItem.setDirectory(true);
			fileItem.setFile(mCurrentFolder.getParentFile());
			mFiles.add(0, fileItem);
		}
		return count;
	}

	/**
	 * Obtain the file extension.
	 * 
	 * @param file
	 *            The provided file object.
	 * @return The file extension or null if don't have extension.
	 */
	private String getExtension(File file) {
		String ext = null;
		String fileName = file.getName();
		int idx = fileName.lastIndexOf(".");
		if (idx > 0) {
			ext = fileName.substring(idx + 1);
		}
		return ext;
	}

	/**
	 * Check if the provided extension is in the list.
	 * 
	 * @param extension
	 *            Provided extension.
	 * @param list
	 *            The list to be checked.
	 * @return True if the extension is in the list.
	 */
	private boolean checkIsInList(String extension, String[] list) {
		if (extension != null) {
			for (String ext : list) {
				if (ext.equalsIgnoreCase(extension)) {
					return true;
				}
			}
		}
		return false;
	}

}
