package com.github.andremoniy.jersey.bug4332;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Based on https://github.com/nhenneaux/jersey-http2-jetty-connector/blob/master/jersey-http2-jetty-connector/src/test/java/org/glassfish/jersey/jetty/connector/DummyRestApi.java
 */

@Path("/")
public interface DummyRestApi {

    @GET
    @Path("world")
    @Produces("application/json")
    Data hello();

    class Data {
        private String data;

        public Data() {
        }

        public Data(String data) {
            this.data = data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

}
