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
package ro.ciubex.dscautorename.receiver;

import android.os.FileObserver;

import java.io.File;

import ro.ciubex.dscautorename.DSCApplication;

/**
 * Define a folder observer used to check if something was changed on the selected folder.
 *
 * @author Claudiu Ciobotariu
 */
public class FolderObserver extends FileObserver {
	private static final String TAG = FolderObserver.class.getName();
	private DSCApplication mApplication;

	private static final int MASK = (FileObserver.CREATE |
			FileObserver.DELETE |
			FileObserver.DELETE_SELF |
			FileObserver.MODIFY |
			FileObserver.MOVED_FROM |
			FileObserver.MOVED_TO |
			FileObserver.MOVE_SELF);
	private String mRootPath;

	public FolderObserver(DSCApplication application, String path) {
		super(path, MASK);
		mApplication = application;
		mRootPath = path;
	}

	/**
	 * This method is invoked when a registered event occur.
	 *
	 * @param event The type of event which happened
	 * @param path  The path, relative to the main monitored file or directory,
	 *              of the file or directory which triggered the event
	 */
	@Override
	public void onEvent(int event, String path) {
		switch (event & FileObserver.ALL_EVENTS) {
			case FileObserver.CREATE:
				checkAutoRenameTask(path);
				break;
			case FileObserver.DELETE:
			case FileObserver.DELETE_SELF:
			case FileObserver.MODIFY:
			case FileObserver.MOVED_FROM:
			case FileObserver.MOVED_TO:
			case FileObserver.MOVE_SELF:
				checkForFolder(path);
				break;
		}
	}

	/**
	 * Check auto rename task and launch it if necessary.
	 */
	private void checkAutoRenameTask(String path) {
		if (mApplication != null) {
			mApplication.launchAutoRenameTask(null, false, null, false);
		}
		checkForFolder(path);
	}

	/**
	 * Check if the current path is actually a folder, if it is a folder should be updated observers.
	 *
	 * @param path Path to be checked.
	 */
	private void checkForFolder(String path) {
		String fullPath = getFullPath(path);
		if (mApplication != null && isDirectoryCreated(fullPath)) {
			mApplication.logD(TAG, "checkForFolder: " + fullPath);
			mApplication.updateFolderObserverList();
		}
	}

	/**
	 * Get full path of a file or folder.
	 *
	 * @param file Path to be checked.
	 * @return The full path.
	 */
	private String getFullPath(String file) {
		if (file == null) {
			return mRootPath;
		}
		return mRootPath + File.separator + file;
	}

	/**
	 * Check if the path is a directory.
	 *
	 * @param path Path to be checked.
	 * @return True if the path is a directory.
	 */
	private boolean isDirectoryCreated(String path) {
		File f = new File(path);
		return (f.exists() && f.isDirectory());
	}

	/**
	 * Get the root path related with this observer.
	 *
	 * @return The root path.
	 */
	public String getPath() {
		return mRootPath;
	}
}
