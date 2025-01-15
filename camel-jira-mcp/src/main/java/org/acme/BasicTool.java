package org.acme;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

//class copied from Burr Sutter https://github.com/burrsutter/quarkus-mcp-server-basic-tool/blob/main/src/main/java/org/acme/BasicTool.java
public class BasicTool {

    @Inject
    ProducerTemplate producerTemplate;

   // hello world style tool


    @Tool(description = "Burr says Hello, Greetings from Burr \uD83D\uDE00")
    String burrHello(
            @ToolArg(description = "To Whom") String toWhom) {
        
        String greeting = "Aloha, Bonjour, Hola, Jambo, Hej, Hallo " + toWhom + " \uD83D\uDE00 ";
        return greeting;
    }




    // addition here : a Camel Route
    @Tool(description = "Zineb is saying Hello  \uD83D\uDE00")
    String zinebHello(
            @ToolArg(description = "To Whom") String toWhom) {
        String greeting =  producerTemplate.requestBody("direct:helloCamel", toWhom, String.class);
        return greeting;
    }

}
