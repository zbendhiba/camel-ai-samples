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

    private static final String SUMMARY_PROMPT = """
            You are a helpful assistant that summarizes JIRA issues for handovers and adds the summaries as comments.
                        
            When provided with a JIRA issue's details, including the title, description, and comments (with their updated dates), you should:
            1. Read the information carefully.
            2. Generate a concise summary of the issue, focusing on the key points and any action items.
            3. Ensure the summary is short, clear, and useful for handovers. No more than 50 words.
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
        messages.add(new SystemMessage(SUMMARY_PROMPT));
        messages.add(new UserMessage(userPrompt));
        return messages;
    }



}
