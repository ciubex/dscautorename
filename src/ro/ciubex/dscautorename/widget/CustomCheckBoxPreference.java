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
package ro.ciubex.dscautorename.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom preference UI component used to disable click on the checkbox.
 *
 * @author Claudiu Ciobotariu
 */

public class CustomCheckBoxPreference extends CheckBoxPreference {
    public CustomCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        int id = Resources.getSystem().getIdentifier("checkbox",
                "id", "android");
        View checkboxView = view.findViewById(id);
        if (checkboxView != null) {
            checkboxView.setClickable(false);
        }
    }
}
