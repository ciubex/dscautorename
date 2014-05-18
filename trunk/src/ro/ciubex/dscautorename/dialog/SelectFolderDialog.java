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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.adpater.FileListAdapter;
import ro.ciubex.dscautorename.model.FileItem;
import ro.ciubex.dscautorename.task.FolderScannAsyncTask;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

/**
 * @author Claudiu Ciobotariu
 * 
 */
public class SelectFolderDialog extends Dialog implements OnClickListener,
		FolderScannAsyncTask.Responder {
	private DSCApplication mApplication;
	private List<FileItem> mFiles;
	private File mCurrentFolder;
	private ListView mFilesListView;
	protected Button btnOk, btnCancel;

	public SelectFolderDialog(Context context, DSCApplication application) {
		super(context);
		setContentView(R.layout.select_folder_dialog_layout);
		mApplication = application;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDialog();
		initFilesList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		updateDialogTitle();
		startFolderScanningTask();
	}

	/**
	 * Initialize the dialog.
	 */
	private void initDialog() {
		btnOk = (Button) findViewById(R.id.btnOk);
		btnOk.setOnClickListener(this);
		btnCancel = (Button) findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(this);
	}

	/**
	 * Initialize the list o files and files list view.
	 */
	private void initFilesList() {
		mCurrentFolder = new File(mApplication.getFolderScanning());
		mFiles = new ArrayList<FileItem>();
		mFilesListView = (ListView) findViewById(R.id.folderList);
		mFilesListView.setEmptyView(findViewById(R.id.emptyFolderList));
		mFilesListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						setSelectedFolder(position);
					}
				});
	}

	/**
	 * Set selected folder from the list.
	 * 
	 * @param position
	 *            Position on the list of selected folder.
	 */
	private void setSelectedFolder(int position) {
		if (position > -1 && position < mFiles.size()) {
			FileItem fileItem = mFiles.get(position);
			if (fileItem != null) {
				if (fileItem.isDirectory()) {
					mCurrentFolder = fileItem.getFile();
					startFolderScanningTask();
				}
			}
		}
	}

	/**
	 * Update the title text for this dialog's window.
	 */
	private void updateDialogTitle() {
		setTitle(mCurrentFolder.getAbsolutePath());
	}

	/**
	 * Create the folder scanning task.
	 */
	private void startFolderScanningTask() {
		new FolderScannAsyncTask(this, mCurrentFolder, mFiles).execute();
	}

	/**
	 * Method invoked when the folder scanning task is started.
	 */
	@Override
	public void startFolderScanning() {
		Context context = getContext();
		mApplication.showProgressDialog(null, context,
				context.getString(R.string.loading_wait), 0);
	}

	/**
	 * Method invoked when the folder scanning task is finished.
	 */
	@Override
	public void endFolderScanning(int result) {
		if (result > -1) {
			updateDialogTitle();
			FileListAdapter adapter = new FileListAdapter(getContext(), mFiles);
			mFilesListView.setAdapter(adapter);
		}
		mApplication.hideProgressDialog();
	}

	/**
	 * Called when a view has been clicked.
	 * 
	 * @param view
	 *            The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		if (btnOk == view) {
			mApplication.setFolderScanning(mCurrentFolder.getAbsolutePath());
		}
		dismiss();
	}

}
