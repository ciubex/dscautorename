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
package ro.ciubex.dscautorename.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

import ro.ciubex.dscautorename.text.HtmlTagHandler;

/**
 * HTML text view component to be used instead the WebView component.
 * @author Claudiu Ciobotariu
 */
public class HtmlView extends AppCompatTextView {
    public HtmlView(Context context) {
        super(context);
    }

    public HtmlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setHtml(@NonNull String html) {
        setHtml(html, null);
    }

    public void setHtml(@NonNull String html, @Nullable Html.ImageGetter imageGetter) {
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(getPaint());
        String content = htmlTagHandler.prepareListTags(html);
        setText(Html.fromHtml(content, imageGetter, htmlTagHandler));
        setMovementMethod(LinkMovementMethod.getInstance());
    }
}
