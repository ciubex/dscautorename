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
package ro.ciubex.dscautorename.dialog;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * @author Claudiu Ciobotariu
 * 
 */
public abstract class BaseDialog extends Dialog implements OnClickListener {
	protected Context mContext;
	protected DSCApplication mApplication;
	protected Button btnOk, btnCancel;
	protected final static int BUTTON_OK = 1 << 0;
	protected final static int BUTTON_CANCEL = 1 << 1;

	public BaseDialog(Context context, DSCApplication application) {
		super(context);
		mContext = context;
		mApplication = application;
	}

	/**
	 * Initialize the dialog.
	 */
	protected void initDialog(int buttons) {
		btnOk = (Button) findViewById(R.id.btnOk);
		if ((buttons & BUTTON_OK) == BUTTON_OK) {
			btnOk.setOnClickListener(this);
		} else if (btnOk != null) {
			btnOk.setVisibility(View.GONE);
		}
		btnCancel = (Button) findViewById(R.id.btnCancel);
		if ((buttons & BUTTON_CANCEL) == BUTTON_CANCEL) {
			btnCancel.setOnClickListener(this);
		} else if (btnCancel != null) {
			btnCancel.setVisibility(View.GONE);
		}
	}

	/**
	 * Called when a view has been clicked.
	 * 
	 * @param view
	 *            The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		dismiss();
	}

	/**
	 * On this method is a confirmation dialog.
	 * 
	 * @param titleStringId
	 *            The resource string id used for the confirmation dialog title.
	 * @param message
	 *            The message used for the confirmation dialog text.
	 * @param confirmationId
	 *            The id used to be identified the confirmed case.
	 * @param anObject
	 *            This could be used to send from the object needed on the
	 *            confirmation.
	 */
	protected void showConfirmationDialog(int titleStringId, String message,
			final int confirmationId, final Object anObject) {
		new AlertDialog.Builder(mContext)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(titleStringId)
				.setMessage(message)
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								onConfirmation(false, confirmationId, anObject);
							}

						})
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								onConfirmation(true, confirmationId, anObject);
							}

						}).show();
	}

	/**
	 * This method should overwrite on each dialog to handle confirmations
	 * cases.
	 * 
	 * @param positive
	 *            True if the confirmation is positive.
	 * @param confirmationId
	 *            The confirmation ID to identify the case.
	 * @param anObject
	 *            An object send by the caller method.
	 */
	protected void onConfirmation(boolean positive, int confirmationId,
			Object anObject) {
	}

	/**
	 * On this method is a confirmation dialog.
	 * 
	 * @param titleStringId
	 *            The resource string id used for the confirmation dialog title.
	 * @param message
	 *            The message used for the confirmation dialog text.
	 */
	protected void showAlertDialog(int titleStringId, String message) {
		new AlertDialog.Builder(mContext)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(titleStringId)
				.setMessage(message)
				.setNeutralButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show();
	}
}
