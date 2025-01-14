package org.acme;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

//class copied from Burr Sutter https://github.com/burrsutter/quarkus-mcp-server-basic-tool/blob/main/src/main/java/org/acme/BasicTool.java
public class BasicTool {

   // hello world style tool

    @Tool(description = "Burr says Hello, Greetings from Burr \uD83D\uDE00")
    String burrHello(
            @ToolArg(description = "To Whom") String toWhom) {
        
        String greeting = "Aloha, Bonjour, Hola, Jambo, Hej, Hallo " + toWhom + " \uD83D\uDE00 ";
        return greeting;
    }

    @Tool(description = "Burr says Goodbye \uD83D\uDE00")
    String burrGoodbye(
            @ToolArg(description = "To Whom") String toWhom) {
        
        String greeting = "Aloha  " + toWhom + " \uD83D\uDE00 ";
        return greeting;
    }

    @Tool(description = "Burr says Thank You, Gratefulness from Burr \uD83D\uDE00")
    String burrThankYou(
            @ToolArg(description = "To Whom") String toWhom) {
        
        String gratitude = "Mahalo, Merci, Gracias, Asante, Tack, Danke " + toWhom + " \uD83D\uDE00 ";
        return gratitude;
    }

}
