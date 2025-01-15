package org.acme;


import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;



@ApplicationScoped
public class CamelRoutes extends RouteBuilder {



    @Override
    public void configure() throws Exception {

        /*from("langchain4j-tools:jiraComments?tags=jira&description=Add a comment in a JIRA issue&parameter.IssueKey=string&parameter.comment=string")
                .log("hello zineb");*/

        // Hello world in Camel
        from("direct:helloCamel")
                .setBody(simple("hello from a Camel Route to ${body}"));

    }
}
