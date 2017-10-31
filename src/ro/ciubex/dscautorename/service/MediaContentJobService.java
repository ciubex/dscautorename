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
package ro.ciubex.dscautorename.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * This is a media content job service which will replace the CameraRenameService for the
 * new Android versions (api > 24).
 *
 * @author Claudiu Ciobotariu
 */
@TargetApi(Build.VERSION_CODES.N)
public class MediaContentJobService extends JobService {
    private static final String TAG = MediaContentJobService.class.getName();
    public static final int JOB_ID = 681979;

    /**
     * Log method used by static methods.
     *
     * @param context  Application context.
     * @param logLevel Log level.
     * @param tag      Actually this is this Class name.
     * @param msg      Message to be logged.
     */
    private static void log(Context context, int logLevel, String tag, String msg) {
        Context appCtx = context.getApplicationContext();
        if (appCtx instanceof DSCApplication) {
            DSCApplication application = (DSCApplication) appCtx;
            if (logLevel == Log.ERROR) {
                application.logE(tag, msg);
            } else {
                application.logD(tag, msg);
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Context appCtx = getApplicationContext();
        if (appCtx instanceof DSCApplication) {
            DSCApplication application = (DSCApplication) appCtx;
            MediaContentJobService.log(appCtx, Log.DEBUG, TAG, "onStartJob()");
            Uri[] uris = params.getTriggeredContentUris();
            if (Utilities.isEmpty(uris)) {
                application.rescheduleMediaContentJobService();
            } else {
                if (!application.isRenameFileTaskRunning()) {
                    application.launchAutoRenameTask(null, false, null, true);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        MediaContentJobService.log(getApplicationContext(), Log.DEBUG, TAG, "onStopJob()");
        return true;
    }
}
