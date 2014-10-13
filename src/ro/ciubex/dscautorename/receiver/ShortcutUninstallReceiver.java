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
import ro.ciubex.dscautorename.activity.RenameShortcutUpdateListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver for shortcut uninstall
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class ShortcutUninstallReceiver extends BroadcastReceiver {
	private static final String TAG = ShortcutUninstallReceiver.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent data) {
		DSCApplication application = null;
		Context appCtx = context.getApplicationContext();
		if (appCtx instanceof DSCApplication) {
			application = (DSCApplication) appCtx;
		}
		if (application != null) {
			Log.d(TAG, "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx UNINSTALL");
			application.prepareShortcutPref(data,
					RenameShortcutUpdateListener.TYPE.UNINSTALL);
		}
	}

}
