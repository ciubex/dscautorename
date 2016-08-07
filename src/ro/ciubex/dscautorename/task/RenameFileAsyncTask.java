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
package ro.ciubex.dscautorename.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;

/**
 * An AsyncTask used to rename files.
 *
 * @author Claudiu Ciobotariu
 */
public class RenameFileAsyncTask extends AsyncTask<Void, Integer, Integer>
		implements FileRenameThread.Listener {
	private final WeakReference<Listener> mListener;
	private Context mContext;
	private FileRenameThread mFileRenameThread;
	private boolean mFinished;
	private int mCount;

	public interface Listener {
		void onTaskStarted();

		void onTaskUpdate(int position, int max, String message);

		void onTaskFinished(int count);

		boolean isFinishing();
	}

	public RenameFileAsyncTask(DSCApplication application, Listener listener, boolean noDelay, List<Uri> fileUris) {
		mFileRenameThread = new FileRenameThread(application, this, noDelay, fileUris);
		this.mListener = new WeakReference<>(listener);
		mContext = application.getApplicationContext();
	}

	/**
	 * Runs on the UI thread before doInBackground(Params...).
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Listener listener = mListener.get();
		if (listener != null && !listener.isFinishing()) {
			new Thread(mFileRenameThread).start();
			listener.onTaskStarted();
		}
	}

	/**
	 * Runs on the UI thread after publishProgress(Progress...) is invoked. The
	 * specified values are the values passed to publishProgress(Progress...).
	 *
	 * @param values Not used.
	 */
	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		if (mListener != null) {
			Listener listener = mListener.get();
			if (listener != null && !listener.isFinishing()) {
				int position = values[0];
				int max = values[1];
				String message = mContext.getString(
						position == 1 ? R.string.manually_file_rename_progress_1
								: R.string.manually_file_rename_progress_more,
						position, max);
				listener.onTaskUpdate(position, max, message);
			}
		}
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...). The specified
	 * result is the value returned by doInBackground(Params...).
	 *
	 * @param count The result of the operation computed by
	 *              doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(Integer count) {
		super.onPostExecute(count);
		if (mListener != null) {
			Listener listener = mListener.get();
			if (listener != null && !listener.isFinishing()) {
				listener.onTaskFinished(count);
			}
		}
	}

	/**
	 * Method which is executed in background.
	 *
	 * @param params Not used.
	 * @return Number of processed items.
	 */
	@Override
	protected Integer doInBackground(Void... params) {
		while(!mFinished) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return mCount;
	}


	@Override
	public void onThreadStarted() {
	}

	@Override
	public void onThreadUpdate(int position, int max) {
		this.publishProgress(position, max);
	}

	@Override
	public void onThreadFinished(int count) {
		mCount = count;
		mFinished = true;
	}

	@Override
	public boolean isFinishing() {
		Listener listener = mListener.get();
		if (listener != null) {
			return listener.isFinishing();
		}
		return true;
	}
}
