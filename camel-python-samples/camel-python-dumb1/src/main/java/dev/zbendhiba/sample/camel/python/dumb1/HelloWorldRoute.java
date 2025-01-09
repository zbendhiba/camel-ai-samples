package dev.zbendhiba.sample.camel.python.dumb1;

import org.apache.camel.builder.RouteBuilder;

public class HelloWorldRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        rest("hello")
                .get()
                .to("direct:hello");


        from("direct:hello")
                .setBody(constant("Hello from Camel Route!"));
    }
}
