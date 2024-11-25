package dev.zbendhiba.samples.ai.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_KEY;

@Path("/jira")
public class JiraResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/issue/{key}")
    @GET
    public String readIssueByKey(@PathParam("key") String key) {
        try {
            return producerTemplate.requestBodyAndHeader("direct:get-issue-details", null, ISSUE_KEY, key, String.class);
        } catch (Exception e) {
            return "Error";
        }
    }

    @Path("/summary/{key}")
    @GET
    public String summary(@PathParam("key") String key) {
        try {
            return producerTemplate.requestBodyAndHeader("direct:get-jira-summary", null, ISSUE_KEY, key, String.class);
        } catch (Exception e) {
            return "Error";
        }
    }

    @Path("/test/")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String tools( String question) {
        try {
           System.out.println("hello world");
            return  producerTemplate.requestBody("direct:test-tools", question, String.class);
        } catch (Exception e) {
            return "Error";
        }
    }


}

