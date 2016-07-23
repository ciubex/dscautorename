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
package ro.ciubex.dscautorename.model;

/**
 * This is a model used for identified device informations: Retail Branding, Marketing Name, Device, Model.
 * @see <a href="https://support.google.com/googleplay/answer/1727131">Supported devices - Google Play Help</a>
 *
 * @author Claudiu Ciobotariu
 */
public class DeviceInfo {
    private String mRetailBranding;
    private String mDevice;
    private String mModel;
    private String mMarketingName;

    public String getRetailBranding() {
        return mRetailBranding;
    }

    public void setRetailBranding(String retailBranding) {
        this.mRetailBranding = retailBranding;
    }

    public String getDevice() {
        return mDevice;
    }

    public void setDevice(String device) {
        this.mDevice = device;
    }

    public String getModel() {
        return mModel;
    }

    public void setModel(String model) {
        this.mModel = model;
    }

    public String getMarketingName() {
        return mMarketingName;
    }

    public void setMarketingName(String marketingName) {
        this.mMarketingName = marketingName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mMarketingName.startsWith(mRetailBranding)) {
            sb.append(mMarketingName);
        } else {
            sb.append(mRetailBranding).append(" ").append(mMarketingName);
        }
        sb.append(" (").append(mModel).append(")");
        return sb.toString();
    }
}
