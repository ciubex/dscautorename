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
package ro.ciubex.dscautorename.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.adpater.FileListAdapter;
import ro.ciubex.dscautorename.model.FileItem;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.task.FolderScannAsyncTask;
import ro.ciubex.dscautorename.util.Utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * A dialog used to show a list of folders and files.
 *
 * @author Claudiu Ciobotariu
 */
public class SelectFolderDialog extends BaseDialog implements
		FolderScannAsyncTask.Responder {

	private SelectFolderListener mSelectFolderListener;
	private int mFolderIndex;
	private File mCurrentFolder;
	private ListView mFilesListView;
	private FileListAdapter mFileListAdapter;
	private List<FileItem> mFiles;
	private boolean mIsFolderScanning;
	private Button mBtnNewFolder;

	public interface SelectFolderListener {
		public String getSelectedFolder();

		public void onFolderSelected(int folderIndex, SelectedFolderModel selectedFolder);
	}

	public SelectFolderDialog(Context context, DSCApplication application, SelectFolderListener selectFolderListener,
							  int folderIndex) {
		super(context, application);
		setContentView(R.layout.select_folder_dialog_layout);
		mSelectFolderListener = selectFolderListener;
		mFolderIndex = folderIndex;
		this.mFiles = new ArrayList<FileItem>();
		mFileListAdapter = new FileListAdapter(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDialog(BUTTON_OK | BUTTON_CANCEL);
		initFilesList();
		mBtnNewFolder = (Button) findViewById(R.id.btnNewFolder);
		mBtnNewFolder.setOnClickListener(this);
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
	 * Initialize the list o files and files list view.
	 */
	private void initFilesList() {
		String folderName = null;
		if (mSelectFolderListener != null) {
			folderName = mSelectFolderListener.getSelectedFolder();
		}
		if (folderName == null) {
			folderName = mApplication.getDefaultFolderScanning();
			SelectedFolderModel[] folders = mApplication.getSelectedFolders();
			if (folders.length > 0) {
				int index = mFolderIndex;
				if (mFolderIndex < 0) {
					index = 0;
				}
				folderName = folders[index].getFullPath();
			}
		}
		mCurrentFolder = new File(folderName);
		mFilesListView = (ListView) findViewById(R.id.folderList);
		mFilesListView.setEmptyView(findViewById(R.id.emptyFolderList));
		mFilesListView.setAdapter(mFileListAdapter);
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
	 * @param position Position on the list of selected folder.
	 */
	private void setSelectedFolder(int position) {
		if (mIsFolderScanning) {
			mApplication.createProgressDialog(null, DSCApplication.getAppContext(),
					DSCApplication.getAppContext().getString(R.string.loading_wait));
			mApplication.showProgressDialog();
		} else {
			if (position > -1 && position < mFileListAdapter.getCount()) {
				FileItem fileItem = mFileListAdapter.getItem(position);
				if (fileItem != null) {
					if (fileItem.isDirectory()) {
						mCurrentFolder = fileItem.getFile();
						startFolderScanningTask();
					}
				}
			}
		}
	}

	/**
	 * Update the title text for this dialog's window.
	 */
	private void updateDialogTitle() {
		String fullPath = mCurrentFolder.getAbsolutePath();
		String title;
		int length = fullPath.length();
		int maxChars = 20;
		if (length < maxChars) {
			title = fullPath;
		} else {
			title = "..." + fullPath.substring(length - maxChars);
		}
		setTitle(title);
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
		mIsFolderScanning = true;
	}

	/**
	 * Method invoked when the folder scanning task is finished.
	 */
	@Override
	public void endFolderScanning(int result) {
		if (result > -1) {
			updateDialogTitle();
			mFileListAdapter.clear();
			mFileListAdapter.addAll(mFiles);
			mFileListAdapter.notifyDataSetChanged();
		}
		mIsFolderScanning = false;
		mApplication.hideProgressDialog();
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param view The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		if (btnOk == view) {
			if (mSelectFolderListener != null) {
				SelectedFolderModel selectedFolder = new SelectedFolderModel();
				String selectedPath = mCurrentFolder.getAbsolutePath();
				MountVolume volume = mApplication.getMountVolumeByPath(selectedPath);
				if (mApplication.getSdkInt() > 20 && volume != null) {
					selectedFolder.setPath(selectedPath.substring(volume.getPath().length() + 1));
					selectedFolder.setSchema("content");
					selectedFolder.setAuthority("com.android.externalstorage.documents");
					selectedFolder.setUuid(volume.getUuid());
					selectedFolder.setFlags(195);
				} else {
					selectedFolder.setRootPath("");
					selectedFolder.setPath(selectedPath);
				}
				mSelectFolderListener.onFolderSelected(mFolderIndex, selectedFolder);
			}
		} else if (mBtnNewFolder == view) {
			onNewFolderHandler();
		}
		super.onClick(view);
	}

	/**
	 * Handle actions related with the new folder button.
	 */
	private void onNewFolderHandler() {
		final EditText input = new EditText(mContext);
		input.setHint(R.string.new_folder);
		final BaseDialog parentDialog = this;
		new AlertDialog.Builder(mContext)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.new_folder)
				.setView(input)
				.setNegativeButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								String value = input.getText().toString().trim();
								makeNewFolder(value);
								parentDialog.show();
							}
						})
				.setPositiveButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								parentDialog.show();
							}
						}).show();
	}

	/**
	 * Create a new folder on the current folder.
	 * @param folderName New folder to be created.
	 */
	private void makeNewFolder(String folderName) {
		if (!Utilities.isEmpty(folderName)) {
			File newFolder = new File(mCurrentFolder, folderName);
			newFolder.mkdir();
		}
	}
}
