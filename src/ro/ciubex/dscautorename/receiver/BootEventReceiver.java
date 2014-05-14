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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This broadcast receiver is used only when the Media observer is enabled.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class BootEventReceiver extends BroadcastReceiver {

	/**
	 * This method is called when the BroadcastReceiver is receiving an Intent
	 * broadcast.
	 * 
	 * @param context
	 *            The Context in which the receiver is running.
	 * @param intent
	 *            The Intent being received.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		DSCApplication application = null;
		Context appCtx = context.getApplicationContext();
		if (appCtx instanceof DSCApplication) {
			application = (DSCApplication) appCtx;
		}
		if (application != null
				&& DSCApplication.SERVICE_TYPE_CONTENT == application
						.getServiceType()) {
			application.checkRegisteredServiceType(true);
		}
	}

}
