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
package ro.ciubex.dscautorename.receiver;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import android.database.ContentObserver;
import android.os.Handler;

/**
 * Define a media content observer used to check if something was changed on the
 * media storage.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class MediaStorageContentObserver extends ContentObserver {
	private DSCApplication mApplication;

	public MediaStorageContentObserver(Handler handler,
			DSCApplication application) {
		super(handler);
		this.mApplication = application;
	}

	/**
	 * This method is called when a content change occurs.
	 * 
	 * @param selfChange
	 *            True if this is a self-change notification.
	 */
	@Override
	public void onChange(boolean selfChange) {
		// super.onChange(selfChange);
		checkAutoRenameTask();
	}

	/**
	 * Check auto rename task and launch it if necessary.
	 */
	private void checkAutoRenameTask() {
		if (mApplication != null && mApplication.isAutoRenameEnabled()) {
			mApplication.setRenameFileRequested(true);
			if (!mApplication.isRenameFileTaskRunning()) {
				new RenameFileAsyncTask(mApplication).execute();
			}
		}
	}

}
