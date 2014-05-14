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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;

/**
 * @author Claudiu Ciobotariu
 * 
 */
public class MediaStorageObserverService extends Service {
	private DSCApplication mApplication;
	private ContentObserver mMediaStorageContentObserver;

	/**
	 * Called by the system when the service is first created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = null;
		Context appCtx = getApplicationContext();
		if (appCtx instanceof DSCApplication) {
			mApplication = (DSCApplication) appCtx;
		}
		if (mApplication != null && mMediaStorageContentObserver == null) {
			mMediaStorageContentObserver = new MediaStorageContentObserver(
					new Handler(), mApplication);
		}
	}

	/**
	 * Called by the system to notify a Service that it is no longer used and is
	 * being removed.
	 */
	@Override
	public void onDestroy() {
		if (mMediaStorageContentObserver != null) {
			getContentResolver().unregisterContentObserver(
					mMediaStorageContentObserver);
		}
		super.onDestroy();
	}

	/**
	 * Called by the system every time a client explicitly starts the service by
	 * calling startService(Intent), providing the arguments it supplied and a
	 * unique integer token representing the start request. Do not call this
	 * method directly.
	 * 
	 * @param intent
	 *            The Intent supplied to startService(Intent), as given.
	 * @param flags
	 *            Additional data about this start request.
	 * @param startId
	 *            A unique integer representing this specific request to start.
	 * @return We want this service to continue running until it is explicitly
	 *         stopped, so return sticky.
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (mMediaStorageContentObserver != null) {
			boolean flag = false;
			getContentResolver().registerContentObserver(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, flag,
					mMediaStorageContentObserver);
			getContentResolver().registerContentObserver(
					MediaStore.Images.Media.INTERNAL_CONTENT_URI, flag,
					mMediaStorageContentObserver);
			if (mApplication.isRenameVideoEnabled()) {
				getContentResolver().registerContentObserver(
						MediaStore.Video.Media.EXTERNAL_CONTENT_URI, flag,
						mMediaStorageContentObserver);
				getContentResolver().registerContentObserver(
						MediaStore.Video.Media.INTERNAL_CONTENT_URI, flag,
						mMediaStorageContentObserver);
			}
		}
		return START_STICKY;
	}

	/**
	 * Return the communication channel to the service. Return null because
	 * clients can not bind to the service.
	 * 
	 * @param intent
	 *            Not used.
	 * @return NULL
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
