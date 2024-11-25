package dev.zbendhiba.samples.ai.jira;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;


@ApplicationScoped
@RegisterForReflection
@Named("myTransformer")
public class MyTransformer {

    private static final String JIRA_PROMPT = """
    You are a bot responsible for getting summary of a JIRA, and add comment if the user asks for the second one. 
    Use the tools provided to perform the request
    """;

    public  List<String> issueToRAGContent(Issue issue){
        StringBuilder issueContent = new StringBuilder();
        issueContent.append("Title: ").append(issue.getDescription()).append(", ");
        issueContent.append("Description: ").append(issue.getSummary()).append(", ");
        issueContent.append("Comments: ");
        for (Comment comment : issue.getComments()) {
            issueContent.append("- [").append(comment.getUpdateDate()).append("] ").append(comment.getBody());
        }

        List<String> augmentedData = List.of(
                issueContent.toString());
        return augmentedData;
    }

    public List<ChatMessage> tools(String userPrompt){
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(JIRA_PROMPT));
        messages.add(new UserMessage(userPrompt));
        return messages;
    }

 /*   public String tools(String userPrompt){
       return JIRA_PROMPT + " " + userPrompt;
    }*/


}
