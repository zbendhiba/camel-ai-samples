import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.apache.camel.CamelContext;
import org.apache.camel.Variable;
import org.apache.camel.component.google.mail.GoogleMailComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper bean that builds a ModifyMessageRequest to move an email
 * to a Gmail label based on the triage category.
 *
 * Gmail API requires label IDs (e.g. "Label_5"), not label names
 * (e.g. "INFORMATIONAL"). This bean resolves the label name to its
 * ID by listing all labels from the Gmail account and caching the
 * mapping.
 */
public class GmailModifyHelper {

    private Map<String, String> labelCache;
    private Gmail gmailClient;

    private Gmail getGmailClient(CamelContext context) throws Exception {
        if (gmailClient == null) {
            GoogleMailComponent component = context.getComponent("google-mail", GoogleMailComponent.class);
            gmailClient = component.getClient(component.getConfiguration());
        }
        return gmailClient;
    }

    private String getLabelId(CamelContext context, String labelName) throws Exception {
        if (labelCache == null) {
            labelCache = new HashMap<>();
            List<Label> labels = getGmailClient(context).users().labels().list("me").execute().getLabels();
            for (Label label : labels) {
                labelCache.put(label.getName().toUpperCase(), label.getId());
            }
        }
        return labelCache.get(labelName.toUpperCase());
    }

    public ModifyMessageRequest moveToLabel(
            CamelContext context,
            @Variable("triageCategory") String category) throws Exception {
        String labelId = getLabelId(context, category);
        ModifyMessageRequest request = new ModifyMessageRequest();
        if (labelId != null) {
            request.setAddLabelIds(List.of(labelId));
        }
        request.setRemoveLabelIds(List.of("INBOX"));
        return request;
    }
}
