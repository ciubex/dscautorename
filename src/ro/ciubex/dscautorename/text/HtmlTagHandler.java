/**
 * This file is part of DSCAutoRename application.
 *
 * Copyright (C) 2017 Claudiu Ciobotariu
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
package ro.ciubex.dscautorename.text;

import android.text.Editable;
import android.text.Html;
import android.text.TextPaint;

import org.xml.sax.XMLReader;

/**
 * HTML handler to be used on a TextView component, mostly to display properly HTML list items.
 * @author Claudiu Ciobotariu
 */

public class HtmlTagHandler implements Html.TagHandler {
    private final TextPaint mTextPaint;
    private int itemCount;

    private static final String OL_TAG = "OL_TAG";
    private static final String UL_TAG = "UL_TAG";
    private static final String LI_TAG = "LI_TAG";

    public HtmlTagHandler(TextPaint textPaint) {
        this.mTextPaint = textPaint;
    }

    public String prepareListTags(String html) {
        if (html != null) {
            html = html.replaceAll("(?<=<title>).*?(?=</title>)", "");
            html = html.replace("<ul", "<" + UL_TAG);
            html = html.replace("</ul>", "</" + UL_TAG + ">");
            html = html.replace("<ol", "<" + OL_TAG);
            html = html.replace("</ol>", "</" + OL_TAG + ">");
            html = html.replace("<li", "<" + LI_TAG);
            html = html.replace("</li>", "</" + LI_TAG + ">");
        }
        return html;
    }

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if (opening && UL_TAG.equalsIgnoreCase(tag)) {
            itemCount = -1;
        } else if (opening && OL_TAG.equalsIgnoreCase(tag)) {
            itemCount = 1;
        } else if (LI_TAG.equalsIgnoreCase(tag)) {
            if (opening) {
                if (itemCount == -1) {
                    output.append("\t\u2022 ");
                } else {
                    output.append("\t " + Integer.toString(itemCount)
                            + ". ");
                    itemCount++;
                }
            } else {
                output.append('\n');
            }
        }
    }
}
