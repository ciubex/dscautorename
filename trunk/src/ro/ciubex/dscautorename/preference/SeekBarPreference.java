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
package ro.ciubex.dscautorename.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * A custom seek bar preference.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class SeekBarPreference extends Preference implements
		OnSeekBarChangeListener {
	private static final String TAG = SeekBarPreference.class.getName();

	private static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/ro.ciubex.dscautorename";
	private static final int DEFAULT_VALUE = 0;

	private static final String ATTR_MIN_VALUE = "minValue";
	private static final String ATTR_MAX_VALUE = "maxValue";
	private static final String ATTR_UNITS = "units";

	// Default values for defaults
	private static final int DEFAULT_MIN_VALUE = 0;
	private static final int DEFAULT_MAX_VALUE = 100;
	private static final String DEFAULT_UNITS = "s";

	private AttributeSet mAttrs;

	// Real defaults
	private int mMaxValue;
	private int mMinValue;
	private String mUnits;

	// Current value
	private int mCurrentValue;

	private TextView mStatusText;
	private SeekBar mSeekBar;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setValuesFromXml(attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setValuesFromXml(attrs);
	}

	/**
	 * Initialize internal parameter based on the provided attributes.
	 * 
	 * @param attrs
	 *            The attributes of the XML tag that is inflating the
	 *            preference.
	 */
	private void setValuesFromXml(AttributeSet attrs) {
		this.mAttrs = attrs;
		// Read parameters from attributes
		mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE,
				DEFAULT_MIN_VALUE);
		mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE,
				DEFAULT_MAX_VALUE);
		mUnits = getAttributeStringValue(attrs, PREFERENCE_NS, ATTR_UNITS,
				DEFAULT_UNITS);
	}

	private String getAttributeStringValue(AttributeSet attrs,
			String namespace, String name, String defaultValue) {
		String value = attrs.getAttributeValue(namespace, name);
		if (value == null)
			value = defaultValue;
		if (value != null && value.length() > 1 && value.charAt(0) == '@') {
			String temp = value.substring(1);
			try {
				value = getContext().getString(Integer.parseInt(temp, 10));
			} catch (Exception ex) {
				Log.e(TAG, "getAttributeStringValue: " + value, ex);
			}
		}
		return value;
	}

	/**
	 * Creates the View to be shown for this Preference in the
	 * PreferenceActivity.
	 * 
	 * @param parent
	 *            The parent that this View will eventually be attached to.
	 * @return The View that displays this Preference.
	 */
	@Override
	protected View onCreateView(ViewGroup parent) {
		View prefView = super.onCreateView(parent);
		TextView titleView = (TextView) prefView
				.findViewById(android.R.id.title);
		TextView summaryView = (TextView) prefView
				.findViewById(android.R.id.summary);
		ViewParent viewParent = titleView.getParent();
		if (viewParent instanceof ViewGroup) {
			ViewGroup parentLayout = (ViewGroup) viewParent;
			parentLayout.removeView(titleView);
			parentLayout.removeView(summaryView);

			LinearLayout layoutV = new LinearLayout(parent.getContext());
			layoutV.setOrientation(LinearLayout.VERTICAL);

			LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutV.addView(titleView, lpTitle);

			LinearLayout layoutH = new LinearLayout(parent.getContext());
			LinearLayout.LayoutParams lpSummary = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.MATCH_PARENT, 1);
			lpSummary.gravity = Gravity.LEFT;
			layoutH.addView(summaryView, lpSummary);

			mStatusText = new TextView(parent.getContext());
			mStatusText.setGravity(Gravity.RIGHT);
			mStatusText.setText(getStatusText());

			LinearLayout.LayoutParams lpStatus = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.MATCH_PARENT, 2);
			lpStatus.gravity = Gravity.RIGHT;
			layoutH.addView(mStatusText, lpStatus);

			LinearLayout.LayoutParams lpHoriz = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutV.addView(layoutH, lpHoriz);

			mSeekBar = new SeekBar(parent.getContext(), mAttrs);
			mSeekBar.setMax(mMaxValue - mMinValue);
			mSeekBar.setOnSeekBarChangeListener(this);
			LinearLayout.LayoutParams lpSeek = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutV.addView(mSeekBar, lpSeek);

			LinearLayout.LayoutParams lpVertical = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			parentLayout.addView(layoutV, lpVertical);
		}
		return prefView;
	}

	/**
	 * Binds the created View to the data for this Preference.
	 * 
	 * @param view
	 *            The View that shows this Preference.
	 */
	@Override
	public void onBindView(View view) {
		super.onBindView(view);
		updateView(view);
	}

	/**
	 * Update a SeekBarPreference view with our current state
	 * 
	 * @param view
	 */
	protected void updateView(View view) {
		if (mStatusText != null) {
			mStatusText.setText(getStatusText());
			mStatusText.setMinimumWidth(30);
		}
		if (mSeekBar != null) {
			mSeekBar.setProgress(mCurrentValue - mMinValue);
		}
	}

	/**
	 * Notification that the progress level has changed.
	 * 
	 * @param seekBar
	 *            The SeekBar whose progress has changed
	 * @param progress
	 *            The current progress level. This will be in the range 0..max
	 *            where max was set by setMax(int). (The default value for max
	 *            is 100.)
	 * @param fromUser
	 *            True if the progress change was initiated by the user.
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int newValue = progress + mMinValue;

		if (newValue > mMaxValue)
			newValue = mMaxValue;
		else if (newValue < mMinValue)
			newValue = mMinValue;

		// change rejected, revert to the previous value
		if (!callChangeListener(newValue)) {
			seekBar.setProgress(mCurrentValue - mMinValue);
			return;
		}

		// change accepted, store it
		mCurrentValue = newValue;
		if (mStatusText != null) {
			mStatusText.setText(getStatusText());
		}
		persistInt(newValue);
	}

	/**
	 * Compute the status text, the current seek bar value and the unit.
	 * 
	 * @return Current status text.
	 */
	private String getStatusText() {
		String statusText = String.valueOf(mCurrentValue);
		if (mUnits != null && mUnits.length() > 0) {
			statusText += " " + mUnits;
		}
		return statusText;
	}

	/**
	 * Notification that the user has started a touch gesture.
	 * 
	 * @param seekBar
	 *            The SeekBar in which the touch gesture began
	 */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * Notification that the user has finished a touch gesture.
	 * 
	 * @param seekBar
	 *            The SeekBar in which the touch gesture began.
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		notifyChanged();
	}

	/**
	 * Called when a Preference is being inflated and the default value
	 * attribute needs to be read.
	 * 
	 * @param ta
	 *            The set of attributes.
	 * @param index
	 *            The index of the default value attribute.
	 */
	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		int defaultValue = ta.getInt(index, DEFAULT_VALUE);
		return defaultValue;
	}

	/**
	 * Implement this to set the initial value of the Preference.
	 * 
	 * @param restoreValue
	 *            True to restore the persisted value; false to use the given
	 *            defaultValue.
	 * @param defaultValue
	 *            The default value for this Preference. Only use this if
	 *            restorePersistedValue is false.
	 */
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		if (restoreValue) {
			mCurrentValue = getPersistedInt(mCurrentValue);
		} else {
			int temp = 0;
			try {
				temp = (Integer) defaultValue;
			} catch (Exception ex) {
				Log.e(TAG, "Invalid default value: " + defaultValue.toString());
			}
			persistInt(temp);
			mCurrentValue = temp;
		}
	}

	/**
	 * Make sure that the seekbar is disabled too if the preference is disabled.
	 * 
	 * @param enabled
	 *            Set true to enable it.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (mSeekBar != null) {
			mSeekBar.setEnabled(enabled);
		}
	}

	/**
	 * Called when the dependency changes.
	 * 
	 * @param dependency
	 *            The Preference that this Preference depends on.
	 * @param disableDependent
	 *            Set true to disable this Preference.
	 */
	@Override
	public void onDependencyChanged(Preference dependency,
			boolean disableDependent) {
		super.onDependencyChanged(dependency, disableDependent);

		// Disable movement of seek bar when dependency is false
		if (mSeekBar != null) {
			mSeekBar.setEnabled(!disableDependent);
		}
	}
}
