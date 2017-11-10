package ro.ciubex.dscautorename.text;

import android.text.Editable;
import android.text.Html;
import android.text.TextPaint;

import org.xml.sax.XMLReader;

/**
 * Created by claudiu.ciobotariu on 11/10/2017.
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
