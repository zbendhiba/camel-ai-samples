import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.spi.SimpleFunction;

import org.jsoup.Jsoup;

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
        String text = Jsoup.parse(input.toString()).text();
        text = text.replaceAll("\\(\\s*https?://[^)]*\\)|https?://\\S+", "");
        return text.trim();
    }
}
