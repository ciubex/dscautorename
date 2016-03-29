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
package ro.ciubex.dscautorename.util;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import ro.ciubex.dscautorename.DSCApplication;

/**
 * This is backup agent helper class used to backup shared data for the application.
 *
 * @author Claudiu Ciobotariu
 */
public class DSCBackupAgent extends BackupAgentHelper {

	private DSCApplication mApplication;
	static final String MY_PREFS_BACKUP_KEY = "backup_prefs";

	public void onCreate() {
		Context app = getApplicationContext();
		if (app instanceof DSCApplication) {
			mApplication = (DSCApplication) app;
		}
		SharedPreferencesBackupHelper helper =
				new SharedPreferencesBackupHelper(this, getDefaultSharedPreferencesName());
		addHelper(MY_PREFS_BACKUP_KEY, helper);
	}

	private static String getDefaultSharedPreferencesName() {
		return "ro.ciubex.dscautorename_preferences";
	}

	/**
	 * Invoked when the application's restore operation has completed.
	 */
	@Override
	public void onRestoreFinished() {
		super.onRestoreFinished();
		if (mApplication != null && mApplication.getSdkInt() > 18) {
			mApplication.invalidatePaths();
		}
	}
}
