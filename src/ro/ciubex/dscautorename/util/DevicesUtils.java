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

import android.content.res.AssetManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ro.ciubex.dscautorename.model.DeviceInfo;

/**
 * An utilities class used to extract device information.
 *
 * @author Claudiu Ciobotariu
 */
public class DevicesUtils {
    private static DeviceInfo mDeviceInfo;

    /**
     * Load into memory the device information.
     */
    private static void loadDeviceInformation(AssetManager assetManager) {
        String toFind = Build.DEVICE + "," + Build.MODEL;
        String deviceData = findTextInFile(assetManager, "supported_devices.csv", toFind);
        String[] data = deviceData.split(",");
        DeviceInfo deviceInfo = new DeviceInfo();
        if (data.length == 4) {
            deviceInfo.setRetailBranding(data[0]);
            deviceInfo.setMarketingName(data[1]);
            deviceInfo.setDevice(data[2]);
            deviceInfo.setModel(data[3]);
        } else { // build manually the device info
            deviceInfo.setRetailBranding(Build.MANUFACTURER);
            deviceInfo.setMarketingName("");
            deviceInfo.setDevice(Build.DEVICE);
            deviceInfo.setModel(Build.MODEL);
        }
        mDeviceInfo = deviceInfo;
    }

    /**
     * Returns the consumer friendly device name
     */
    public static String getDeviceName(AssetManager assetManager) {
        if (mDeviceInfo == null) {
            loadDeviceInformation(assetManager);
        }
        return mDeviceInfo.toString();
    }

    /**
     * This method is used to parse the supported_devices.csv to identify current device info.
     *
     * @param assetManager The asset manager used to obtain the file stream.
     * @param fileName     File name with the content.
     * @param stringToFind Text to find in the file content.
     * @return The whole line which contain the searched text.
     */
    private static String findTextInFile(AssetManager assetManager, String fileName, String stringToFind) {
        InputStream in = null;
        BufferedReader br = null;
        String result = "";
        String line;
        try {
            in = assetManager.open(fileName);
            br = new BufferedReader(new InputStreamReader(in, "UTF-16"));
            while ((line = br.readLine()) != null) {
                if (line.contains(stringToFind)) {
                    result = line;
                }
            }
        } catch (IOException e) {
        } finally {
            Utilities.doClose(in);
            Utilities.doClose(br);
        }
        return result;
    }
}
