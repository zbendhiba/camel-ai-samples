import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.spi.SimpleFunction;

import org.apache.commons.text.StringEscapeUtils;

@BindToRegistry("html-decode-function")
public class HtmlDecodeFunction implements SimpleFunction {

    @Override
    public String getName() {
        return "htmlDecode";
    }

    @Override
    public boolean allowNull() {
        return false;
    }

    @Override
    public Object apply(Exchange exchange, Object input) throws Exception {
        String text = input.toString();
        // Strip HTML tags
        text = text.replaceAll("<[^>]+>", " ");
        // Strip URLs (bare and parenthesized)
        text = text.replaceAll("\\(\\s*http[^)]*\\)", "");
        text = text.replaceAll("https?://\\S+", "");
        // Decode HTML entities (unescapeHtml4 does not handle &apos;)
        text = text.replace("&apos;", "'");
        text = StringEscapeUtils.unescapeHtml4(text);
        // Collapse whitespace
        text = text.replaceAll(" {2,}", " ");
        return text.trim();
    }
}
