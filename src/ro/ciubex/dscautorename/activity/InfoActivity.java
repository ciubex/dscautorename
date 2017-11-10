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
package ro.ciubex.dscautorename.activity;

import java.io.IOException;
import java.io.InputStream;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.util.Utilities;
import ro.ciubex.dscautorename.widget.HtmlView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

/**
 * This is the info activity. Usually is used for about and license activities.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class InfoActivity extends Activity {
	private DSCApplication mApplication;
	public static final String TITLE = "title";
	public static final String FILE_NAME = "file_name";
	public static final String MESSAGE = "message";
	public static final String MESSAGE_CONTENT = "message_content";
	public static final String HTML_MESSAGE = "html_message";
	private HtmlView mInfoView;
	private String mBufferedText;
	private String mFileName;

	/**
	 * The method invoked when the activity is creating
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mApplication = (DSCApplication) getApplication();
		applyApplicationTheme();
		applyApplicationLocale();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_layout);
		checkBundle();
		initControls();
	}

	/**
	 * Apply application theme.
	 */
	private void applyApplicationTheme() {
		this.setTheme(mApplication.getApplicationTheme());
	}

	/**
	 * Apply application locale.
	 */
	private void applyApplicationLocale() {
		Resources resources = getBaseContext().getResources();
		Configuration config = resources.getConfiguration();
		config.locale = DSCApplication.getLocale();
		resources.updateConfiguration(config, resources.getDisplayMetrics());
	}

	/**
	 * Check if the activity was initialized with bundles.
	 */
	private void checkBundle() {
		Bundle b = getIntent().getExtras();
		if (b != null) {
			int resId;
			String bundleValue;
			if (b.containsKey(TITLE)) {
				resId = b.getInt(TITLE);
				setTitle(resId);
			}
			if (b.containsKey(MESSAGE)) {
				resId = b.getInt(MESSAGE);
				mBufferedText = mApplication.getApplicationContext().getString(resId);
			} else if (b.containsKey(FILE_NAME)) {
				mFileName = b.getString(FILE_NAME);
			} else if (b.containsKey(MESSAGE_CONTENT)) {
				mBufferedText = b.getString(MESSAGE_CONTENT);
			}
		}
	}

	/**
	 * Initialize controls
	 */
	private void initControls() {
		mInfoView = (HtmlView) findViewById(R.id.infoView);
		if (Build.VERSION_CODES.HONEYCOMB >= mApplication.getSdkInt()) {
			initNewControls();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void initNewControls() {
		mInfoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	/**
	 * This method is invoked when the activity is started
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mFileName != null) {
			mInfoView.setHtml(getStreamText(mFileName));
		} else if (mBufferedText != null) {
			mInfoView.setHtml(mBufferedText);
		}

	}

	@Override
	protected void onPause() {
		mBufferedText = null;
		super.onPause();
	}

	/**
	 * In this method is loaded the license text
	 * 
	 * @param fileName
	 *            File name with the license text
	 * @return The license text
	 */
	private String getStreamText(String fileName) {
		StringBuilder sb = new StringBuilder();
		InputStream in = null;
		try {
			in = mApplication.getAppAssets().open(fileName);
			if (in != null && in.available() > 0) {
				char c;
				while (in.available() > 0) {
					c = (char) in.read();
					sb.append(c);
				}
			}
		} catch (IOException e) {
		} finally {
			Utilities.doClose(in);
		}
		return sb.toString();
	}
}
