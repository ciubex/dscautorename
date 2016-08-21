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

import android.util.Log;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * A thread used to write logs to a private file.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class LogThread implements Runnable, Closeable {

	private static final List<String> logs = new ArrayList<>();
	private File logsFolder;
	private File logFile;
	private boolean closing;
	private boolean closed;
	private SimpleDateFormat sFormatter;

	public LogThread(File logsFolder) {
		this.logsFolder = logsFolder;
		closing = false;
		closed = false;
		sFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", DSCApplication.getLocale());
		sFormatter.setTimeZone(TimeZone.getDefault());
	}

	/**
	 * Add a log string to logs collection
	 *
	 * @param milliseconds Log timestamp.
	 * @param message      Log text.
	 * @param throwable    An exception to log
	 */
	public void addLog(long milliseconds, String message, Throwable throwable) {
		if (!closing) {
			synchronized (logs) {
				logs.add(getLogMessage(milliseconds, message, throwable));
				logs.notifyAll();
			}
		}
	}

	/**
	 * Compose the log message by formatting date and time and add text message.
	 *
	 * @param milliseconds Log timestamp.
	 * @param message      Log text.
	 * @param throwable    An exception to log
	 * @return Composed log message.
	 */
	private String getLogMessage(long milliseconds, String message, Throwable throwable) {
		String log = sFormatter.format(new Date(milliseconds)) + "\t" + message;
		if (throwable != null) {
			log += "\t" + Log.getStackTraceString(throwable);
		}
		return log;
	}

	/**
	 * Close root shell
	 */
	@Override
	public void close() throws IOException {
		synchronized (logs) {
			closing = true;
			logs.notifyAll();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void run() {
		BufferedWriter bufferedWriter = null;
		try {
			createLogFile();
			bufferedWriter = writeLogs();
		} catch (IOException e) {
			closing = true;
		} finally {
			Utilities.doClose(bufferedWriter);
		}
		closed = true;
	}

	/**
	 * Write available logs to log file.
	 * 
	 * @throws IOException
	 */
	private BufferedWriter writeLogs() throws IOException {
		BufferedWriter bufferedWriter = getBufferedWriter();
		while (!closing) {
			synchronized (logs) {
				try {
					logs.wait();
					if (isLogFileSizeExceeded()) {
						Utilities.doClose(bufferedWriter);
						logFile.delete();
						createLogFile();
						bufferedWriter = getBufferedWriter();
					}
					for (String log : logs) {
						bufferedWriter.append(log);
						bufferedWriter.newLine();
					}
					bufferedWriter.flush();
					logs.clear();
				} catch (InterruptedException e) {
				}
			}
		}
		return bufferedWriter;
	}

	private BufferedWriter getBufferedWriter() throws IOException {
		return new BufferedWriter(new FileWriter(logFile, true));
	}

	/**
	 * Create the log file.
	 * 
	 * @throws IOException
	 */
	private void createLogFile() throws IOException {
		logFile = new File(logsFolder, DSCApplication.LOG_FILE_NAME);
		if (!logFile.exists()) {
			logFile.createNewFile();
		}
	}

	/**
	 * Check the log file size to not exceed the 5MB.
	 *
	 * @return True if the file size is larger than 5MB.
	 */
	private boolean isLogFileSizeExceeded() {
		if (logFile != null && logFile.exists()) {
			long sizeInMB = logFile.length() / 1048576;
			if (sizeInMB > 5) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the log file.
	 *
	 * @return The log file.
	 */
	public File getLogFile() {
		return logFile;
	}
}
