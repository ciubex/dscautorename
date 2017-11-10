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
 * Created by claudiu.ciobotariu on 11/10/2017.
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
