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

import java.util.Locale;
import java.util.regex.Pattern;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.model.FileNameModel;

/**
 * An utilities class used to check patterns.
 *
 * @author Claudiu Ciobotariu
 */
public class RenamePatternsUtilities {
	private DSCApplication mApplication;
	private Locale mLocale;
	private Pattern[] mPatterns;
	private FileNameModel[] mFileNameModels;

	public RenamePatternsUtilities(DSCApplication application) {
		mApplication = application;
		mLocale = mApplication.getLocale();
		mFileNameModels = mApplication.getOriginalFileNamePattern();
	}

	/**
	 * Prepare file name patterns.
	 */
	public void buildPatterns() {
		int i, len = mFileNameModels.length, lst;
		mPatterns = new Pattern[len];
		Pattern pattern;
		FileNameModel fileNameModel;
		String before;
		for (i = 0; i < len; i++) {
			fileNameModel = mFileNameModels[i];
			before = fileNameModel.getBefore().toLowerCase(mLocale);
			lst = before.length();
			if (lst == 0) {
				before = "*";
			} else if (lst > 0) {
				if (before.charAt(lst - 1) != '*') {
					before += "*";
				}
			}
			pattern = Pattern.compile(wildcardToRegex(before));
			mPatterns[i] = pattern;
		}
	}

	/**
	 * Convert wildcard to a regex expression.
	 *
	 * @param wildcard Wildcard expression to convert.
	 * @return Converted expression.
	 */
	private String wildcardToRegex(String wildcard) {
		StringBuffer s = new StringBuffer(wildcard.length());
		s.append('^');
		for (int i = 0, is = wildcard.length(); i < is; i++) {
			char c = wildcard.charAt(i);
			switch (c) {
				case '*':
					s.append(".*");
					break;
				case '?':
					s.append(".");
					break;
				// escape special regexp-characters
				case '(':
				case ')':
				case '[':
				case ']':
				case '$':
				case '^':
				case '.':
				case '{':
				case '}':
				case '|':
				case '\\':
					s.append("\\");
					s.append(c);
					break;
				default:
					s.append(c);
					break;
			}
		}
		s.append('$');
		return (s.toString());
	}

	/**
	 * Look on the path and check with existing file name matches.
	 *
	 * @param fileName Path to be checked.
	 * @return -1 if the path is not matching with a file name pattern otherwise is
	 * returned file name pattern index.
	 */
	public int matchFileNameBefore(String fileName) {
		String lower = fileName.toLowerCase(mLocale);
		int i, len = mPatterns.length;
		Pattern pattern;
		for (i = 0; i < len; i++) {
			pattern = mPatterns[i];
			if (pattern.matcher(lower).matches()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Look on the path and check with existing file name matches.
	 *
	 * @param patternString Pattern string to check the provided file name.
	 * @param fileName Path to be checked.
	 * @return -1 if the path is not matching with a file name pattern otherwise is
	 * returned 0.
	 */
	public int matchFileNameBefore(String patternString, String fileName) {
		String before = patternString.toLowerCase(mLocale);
		String lower = fileName.toLowerCase(mLocale);
		Pattern pattern = Pattern.compile(wildcardToRegex(before));
		if (pattern.matcher(lower).matches()) {
			return 0;
		}
		return -1;
	}
}
