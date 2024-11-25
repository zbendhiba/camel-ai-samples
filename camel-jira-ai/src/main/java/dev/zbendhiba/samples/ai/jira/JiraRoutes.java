package dev.zbendhiba.samples.ai.jira;


import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.chat.rag.LangChain4jRagAggregatorStrategy;

import static org.apache.camel.component.jira.JiraConstants.ISSUE_KEY;


@ApplicationScoped
public class JiraRoutes extends RouteBuilder {

    private static final String SUMMARY_PROMPT = """
            You are a helpful assistant that summarizes JIRA issues for handovers and adds the summaries as comments.
                        
            When provided with a JIRA issue's details, including the title, description, and comments (with their updated dates), you should:
            1. Read the information carefully.
            2. Generate a concise summary of the issue, focusing on the key points and any action items.
            3. Ensure the summary is short, clear, and useful for handovers. No more than 50 words.
            """;


    @Override
    public void configure() throws Exception {
        // Create an instance of the RAG aggregator strategy
        LangChain4jRagAggregatorStrategy aggregatorStrategy = new LangChain4jRagAggregatorStrategy();

        // Get details of a JIRA issue
        from("direct:get-issue-details")
                .to("jira:fetchIssue")
                .bean(MyTransformer.class, "issueToRAGContent");

        // Get an AI summary of the JIRA issue
        /*from("direct:get-jira-summary")
                .setBody(constant(SUMMARY_PROMPT))
                // add details of the JIRA issue
                .enrich("direct:get-issue-details", aggregatorStrategy)
                .to("langchain4j-chat:jiraSummary?tags")
                .to("jira:addComment");*/


        // Add the JIRA summary to the JIRA

        from("direct:test-tools")
                .bean(MyTransformer.class, "tools")

         .to("langchain4j-tools:jiraSummary?tags=jira");

        // Update the JIRA issue with the provided summary
       from("langchain4j-tools:jiraComments?tags=jira&description=Add a comment in a JIRA issue&parameter.issue=string&parameter.comment=string")
               .process(e -> {
                   Object body = e.getIn().getBody();
                   System.out.println("hello");
               })
               .setHeader(ISSUE_KEY, simple(":#issue"))
                .setBody(simple(":#comment"))
                .to("jira:addComment");

        from("langchain4j-tools:jiraDetails?tags=jira&description=GET the description of a JIRA issue&parameter.issueKey=string")
                .log("hello from tools get information of a comment")


              //  .setBody(constant(SUMMARY_PROMPT))
                // add details of the JIRA issue
              //  .setHeader(ISSUE_KEY, simple(":#issue"))
                .process(e -> {
                    Object body = e.getIn().getBody();
                    System.out.println("hello");
                })
                .setBody(constant("response"))
               // .enrich("direct:get-issue-details", aggregatorStrategy)
               // .log("step 1")
              //  .log("header information is ${header.IssueKey}")
            //    .to("direct:get-issue-details")
                .log("step 2")
        ;

    }
}
