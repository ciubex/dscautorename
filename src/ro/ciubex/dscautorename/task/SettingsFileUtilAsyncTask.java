/**
 * This file is part of DSCAutoRename application.
 * <p>
 * Copyright (C) 2017 Claudiu Ciobotariu
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.dscautorename.task;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * This class allow the application to export or import settings.
 *
 * @author Claudiu Ciobotariu
 */
public class SettingsFileUtilAsyncTask extends AsyncTask<Void, Void, AsyncTaskResult> {
    private static final String TAG = SettingsFileUtilAsyncTask.class.getName();

    /**
     * Define available operations type
     */
    public enum Operation {
        EXPORT, IMPORT
    }

    private Responder responder;
    private Operation operationType;
    private SelectedFolderModel selectedFolder;

    private static final String FILE_NAME = "dsc_settings.properties";

    /**
     * The listener should implement this interface
     */
    public interface Responder {
        public DSCApplication getDSCApplication();

        public void startFileAsynkTask(Operation operationType);

        public void endFileAsynkTask(Operation operationType,
                                     AsyncTaskResult result);
    }

    /**
     * The constructor of this task
     *
     * @param responder      The listener of this task
     * @param operationType  Type of operation
     * @param selectedFolder Full file name path of exported / imported settings.
     */
    public SettingsFileUtilAsyncTask(Responder responder, Operation operationType, SelectedFolderModel selectedFolder) {
        this.responder = responder;
        this.operationType = operationType;
        this.selectedFolder = selectedFolder;
    }

    /**
     * Method used on the background.
     *
     * @param params Parameters ar ignored.
     * @return True if the operation was successfully.
     */
    @Override
    protected AsyncTaskResult doInBackground(Void... params) {
        AsyncTaskResult taskResult = new AsyncTaskResult();
        taskResult.resultId = AsyncTaskResult.OK;
        if (operationType == Operation.IMPORT) {
            importFromFile(taskResult);
        } else {
            exportToFile(taskResult);
        }
        return taskResult;
    }

    /**
     * Method invoked when is started this task
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        responder.startFileAsynkTask(operationType);
    }

    /**
     * Method invoked at the end of this task
     *
     * @param result The result of this task
     */
    @Override
    protected void onPostExecute(AsyncTaskResult result) {
        super.onPostExecute(result);
        responder.endFileAsynkTask(operationType, result);
    }

    /**
     * Method used to create and export the settings content.
     *
     * @param taskResult The process result entity.
     */
    private void exportToFile(AsyncTaskResult taskResult) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(responder.getDSCApplication());
        Map<String, ?> keys = prefs.getAll();
        String key, clazz, value;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            key = entry.getKey();
            clazz = entry.getValue().getClass().getName();
            value = String.valueOf(entry.getValue());
            sb.append(key).append(':').append(clazz).append(':')
                    .append(value).append('\n');
        }
        writeToFile(sb.toString(), taskResult);
    }

    /**
     * General method used to write a content to a file.
     *
     * @param content    The content to be written to the file.
     * @param taskResult The process result.
     */
    private void writeToFile(String content, AsyncTaskResult taskResult) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            writeToFileUsingJavaAPI(content, taskResult);
        } else {
            writeToFileUsingApi21(content, taskResult);
        }
    }

    /**
     * This method it is used to write a content to a file using old Java API.
     *
     * @param content    The content to be written to the file.
     * @param taskResult The process result.
     */
    private void writeToFileUsingJavaAPI(String content, AsyncTaskResult taskResult) {
        OutputStream fos = null;
        String filePath = getFullFilePath();
        File outFile = null;
        try {
            outFile = new File(filePath);
            fos = new FileOutputStream(outFile);
            fos.write(content.getBytes());
            fos.flush();
            taskResult.resultMessage = filePath;
        } catch (FileNotFoundException e) {
            taskResult.resultId = AsyncTaskResult.ERROR;
            taskResult.resultMessage = e.getMessage();
            Log.e(TAG, "writeToFileUsingJavaAPI FileNotFoundException: " + e.getMessage(), e);
        } catch (IOException e) {
            taskResult.resultId = AsyncTaskResult.ERROR;
            taskResult.resultMessage = e.getMessage();
            Log.e(TAG, "writeToFileUsingJavaAPI IOException: " + e.getMessage(), e);
        } finally {
            Utilities.doClose(outFile);
            Utilities.doClose(fos);
        }
    }

    /**
     * Get the full file path.
     *
     * @return The full file path.
     */
    private String getFullFilePath() {
        return selectedFolder.getFullPath() + File.separatorChar + FILE_NAME;
    }

    /**
     * Check if the file exist.
     *
     * @return True if the file exits.
     */
    private boolean fileExist() {
        File f = new File(getFullFilePath());
        return f != null && f.exists();
    }

    /**
     * This method it is used to write a content to a file using Android API 21 methods.
     *
     * @param content    The content to be written to the file.
     * @param taskResult The process result.
     */
    @TargetApi(21)
    private void writeToFileUsingApi21(String content, AsyncTaskResult taskResult) {
        ParcelFileDescriptor destFileDesc = null;
        FileOutputStream fos = null;
        try {
            DSCApplication application = responder.getDSCApplication();
            List<SelectedFolderModel> selectedFolders = Arrays.asList(selectedFolder);
            Uri folderUri = application.getDocumentUri(selectedFolders, selectedFolder.getFullPath());
            if (fileExist()) {
                Uri oldFile = application.getDocumentUri(selectedFolders, getFullFilePath());
                DocumentsContract.deleteDocument(application.getContentResolver(), oldFile);
            }
            Uri fileUri = DocumentsContract.createDocument(application.getContentResolver(), folderUri, "text/x-java-properties", FILE_NAME);
            destFileDesc = application.getContentResolver().openFileDescriptor(fileUri, "w", null);
            fos = new FileOutputStream(destFileDesc.getFileDescriptor());
            fos.write(content.getBytes());
            fos.flush();
            taskResult.resultMessage = getFullFilePath();
        } catch (FileNotFoundException e) {
            taskResult.resultId = AsyncTaskResult.ERROR;
            taskResult.resultMessage = e.getMessage();
            Log.e(TAG, "writeToFileUsingApi21 FileNotFoundException: " + e.getMessage(), e);
        } catch (IOException e) {
            taskResult.resultId = AsyncTaskResult.ERROR;
            taskResult.resultMessage = e.getMessage();
            Log.e(TAG, "writeToFileUsingApi21 IOException: " + e.getMessage(), e);
        } finally {
            Utilities.doClose(fos);
            Utilities.doClose(destFileDesc);
        }
    }

    /**
     * General method used to read settings from selected file.
     *
     * @param taskResult The process result.
     */
    private void importFromFile(AsyncTaskResult taskResult) {
        String filePath = getFullFilePath();
        File file = new File(filePath);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(responder.getDSCApplication());
            SharedPreferences.Editor editor = prefs.edit();
            String line;
            String[] arrLine;
            while ((line = br.readLine()) != null) {
                arrLine = currentLine(line);
                if (arrLine != null) {
                    storeCurrentLine(editor, arrLine);
                }
            }
            editor.commit();
            taskResult.resultMessage = filePath;
        } catch (FileNotFoundException e) {
            taskResult.resultId = AsyncTaskResult.ERROR;
            taskResult.resultMessage = e.getMessage();
            Log.e(TAG, "importFromFile: " + e.getMessage(), e);
        } catch (IOException e) {
            taskResult.resultId = AsyncTaskResult.ERROR;
            taskResult.resultMessage = e.getMessage();
            Log.e(TAG, "importFromFile: " + e.getMessage(), e);
        } finally {
            Utilities.doClose(fileReader);
        }
    }

    /**
     * Store the values on the application preferences.
     *
     * @param editor  The editor used to store the values.
     * @param arrLine The array of values to be stored, the array contain: [key, type, value]
     */
    private void storeCurrentLine(SharedPreferences.Editor editor, String[] arrLine) {
        String key = arrLine[0], clazz = arrLine[1], value = arrLine[2];
        int intValue;
        float floatValue;
        long longValue;
        if ("java.lang.String".equals(clazz)) {
            if (DSCApplication.KEY_FOLDER_SCANNING.equals(key)) {
                editor.putString(key, getFolderScanningPaths(value));
            } else if (DSCApplication.KEY_ORIGINAL_FILE_NAME_PATTERN.equals(key)) {
                editor.putString(key, getOriginalFileNamePattern(value));
            } else {
                editor.putString(key, value);
            }
        } else if ("java.lang.Boolean".equals(clazz)) {
            editor.putBoolean(key, "true".equalsIgnoreCase(value));
        } else if ("java.lang.Integer".equals(clazz)) {
            intValue = Utilities.parseToInt(value);
            editor.putInt(key, intValue);
        } else if ("java.lang.Float".equals(clazz)) {
            floatValue = Utilities.parseToFloat(value);
            editor.putFloat(key, floatValue);
        } else if ("java.lang.Long".equals(clazz)) {
            longValue = Utilities.parseToLong(value);
            editor.putLong(key, longValue);
        }
    }

    /**
     * Parse the line string to obtain the preference key, type and value.
     *
     * @param line The current line.
     * @return The array contain: [key, type, value]
     */
    private String[] currentLine(String line) {
        String arr[] = null;
        if (line != null) {
            String text = line.trim();
            if (text.length() > 0) {
                int idx1 = text.indexOf(':');
                int idx2 = text.indexOf(':', idx1 + 1);
                String key, clazz, value;
                if (idx1 > 0 && idx2 > 0) {
                    key = text.substring(0, idx1);
                    clazz = text.substring(idx1 + 1, idx2);
                    value = text.substring(idx2 + 1);
                    arr = new String[3];
                    arr[0] = key;
                    arr[1] = clazz;
                    arr[2] = value;
                }
            }
        }
        return arr;
    }

    /**
     * Method used validate and obtain the right selected folder values.
     *
     * @param values Selected folders preference value.
     * @return A string with valid selected folders.
     */
    private String getFolderScanningPaths(String values) {
        String arr[] = values.split(",");
        StringBuilder sb = new StringBuilder("");
        DSCApplication application = responder.getDSCApplication();
        for (String value : arr) {
            SelectedFolderModel sfm = new SelectedFolderModel();
            sfm.fromString(value);
            if (sfm.isValidPath() && sfm.haveGrantUriPermission(application)) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(sfm.toString());
            }
        }
        return sb.toString();
    }

    /**
     * Method used to check and populate the values for the original file name patterns.
     *
     * @param values The imported preference values.
     * @return The validated imported values.
     */
    private String getOriginalFileNamePattern(String values) {
        String arr[] = values.split(",");
        StringBuilder sb = new StringBuilder("");
        DSCApplication application = responder.getDSCApplication();
        for (String value : arr) {
            FileNameModel fnm = new FileNameModel(value);
            if (sb.length() > 0) {
                sb.append(',');
            }
            // remove the selected folder
            SelectedFolderModel sfm = fnm.getSelectedFolder();
            if (!sfm.isValidPath() || !sfm.haveGrantUriPermission(application)) {
                fnm.setSelectedFolder(null);
            }
            sb.append(fnm.toString());
        }
        return sb.toString();
    }
}
