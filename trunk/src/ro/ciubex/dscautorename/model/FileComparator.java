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
package ro.ciubex.dscautorename.model;

import java.io.File;
import java.util.Comparator;

/**
 * A comparator used to sort files and directories.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class FileComparator implements Comparator<FileItem> {

	@Override
	public int compare(FileItem fi1, FileItem fi2) {
		int n1 = 0;
		int n2 = 0;
		File f1 = fi1.getFile();
		File f2 = fi2.getFile();
		n1 = (f1 != null && f1.isDirectory()) ? 0 : 1;
		n2 = (f2 != null && f2.isDirectory()) ? 0 : 1;
		if (n1 == n2) {
			String s1 = f1 != null ? f1.getName() : null;
			String s2 = f2 != null ? f2.getName() : null;
			n1 = s1 != null ? s1.length() : 0;
			n2 = s2 != null ? s2.length() : 0;
			int min = Math.min(n1, n2);
			for (int i = 0; i < min; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 != c2) {
					c1 = Character.toUpperCase(c1);
					c2 = Character.toUpperCase(c2);
					if (c1 != c2) {
						c1 = Character.toLowerCase(c1);
						c2 = Character.toLowerCase(c2);
						if (c1 != c2) {
							return c1 - c2;
						}
					}
				}
			}
		}
		return n1 - n2;
	}

}
