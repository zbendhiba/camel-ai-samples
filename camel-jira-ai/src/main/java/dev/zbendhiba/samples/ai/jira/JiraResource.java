package dev.zbendhiba.samples.ai.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_KEY;

@Path("/jira")
public class JiraResource {
    @Inject
    ProducerTemplate producerTemplate;

   /* @Path("/issue/{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readIssueByKey(@PathParam("key") String key) {
        try {
            Issue issue = producerTemplate.requestBodyAndHeader("jira:fetchIssue", null, ISSUE_KEY, key, Issue.class);
            return Response.ok(issueToJson(issue)).build();
        } catch (Exception e) {
            return Response.status(404).build();
        }
    }*/

    @Path("/issue/{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String readIssueByKey(@PathParam("key") String key) {
        try {
            String issue = producerTemplate.requestBodyAndHeader("direct:get-issue-details", null, ISSUE_KEY, key, String.class);
            return issue;
        } catch (Exception e) {
            return "N/A";
        }
    }

    @Path("/summary/{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String summary(@PathParam("key") String key) {
        try {
            String issue = producerTemplate.requestBodyAndHeader("direct:get-jira-summary", null, ISSUE_KEY, key, String.class);
            return issue;
        } catch (Exception e) {
            return "N/A";
        }
    }

    private JsonObject issueToJson(Issue issue) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("description", issue.getDescription());
        builder.add("key", issue.getKey());
        builder.add("summary", issue.getSummary());
        builder.add("type", issue.getIssueType().getName());
        return builder.build();
    }

}

