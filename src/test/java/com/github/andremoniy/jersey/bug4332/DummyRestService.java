package com.github.andremoniy.jersey.bug4332;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Based on https://github.com/nhenneaux/jersey-http2-jetty-connector/blob/master/jersey-http2-jetty-connector/src/test/java/org/glassfish/jersey/jetty/connector/DummyRestService.java
 */

@Path("/")
public class DummyRestService {

    static String helloMessage = "Hello " + ThreadLocalRandom.current().nextLong();

    @GET
    @Path("world")
    @Produces("application/json")
    public Response hello() {
        return Response.accepted().entity(new DummyRestApi.Data(helloMessage)).build();
    }


}
