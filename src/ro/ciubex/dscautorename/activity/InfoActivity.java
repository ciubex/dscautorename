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

import ro.ciubex.dscautorename.R;
import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * This is the info activity. Usually is used for about and license activities.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class InfoActivity extends Activity {
	public static final String TITLE = "title";
	public static final String FILE_NAME = "file_name";
	public static final String MESSAGE = "message";
	public static final String HTML_MESSAGE = "html_message";
	private TextView mInfoTextView;
	private String mBufferedText;
	private Button mOkButton;
	private boolean mIsHtmlMessage;

	/**
	 * The method invoked when the activity is creating
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_layout);
		checkBundle();
		initControls();
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
				mBufferedText = getResources().getString(resId);
			} else if (b.containsKey(FILE_NAME)) {
				bundleValue = b.getString(FILE_NAME);
				mBufferedText = getStreamText(bundleValue);
			}
			if (b.containsKey(HTML_MESSAGE)) {
				mIsHtmlMessage = b.getBoolean(HTML_MESSAGE);
			}
		}
	}

	/**
	 * Initialize controls
	 */
	private void initControls() {
		mOkButton = (Button) findViewById(R.id.okButton);
		mOkButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mInfoTextView = (TextView) findViewById(R.id.infoTextView);
	}

	/**
	 * This method is invoked when the activity is started
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (mBufferedText != null) {
			if (mIsHtmlMessage) {
				mInfoTextView.setMovementMethod(LinkMovementMethod.getInstance());
				mInfoTextView.setText(Html.fromHtml(mBufferedText));
			} else {
				mInfoTextView.setText(mBufferedText);
			}
		}
	}

	/**
	 * In this method is loaded the license text
	 * 
	 * @param fileName
	 *            File name with the license text
	 * @return The license text
	 */
	private String getStreamText(String fileName) {
		AssetManager assetManager = getAssets();
		StringBuilder sb = new StringBuilder();
		InputStream in = null;
		try {
			in = assetManager.open(fileName);
			if (in != null && in.available() > 0) {
				char c;
				while (in.available() > 0) {
					c = (char) in.read();
					sb.append(c);
				}
			}
		} catch (IOException e) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
}
