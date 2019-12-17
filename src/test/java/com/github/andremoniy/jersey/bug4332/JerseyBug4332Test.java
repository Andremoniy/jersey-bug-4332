package com.github.andremoniy.jersey.bug4332;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.jetty.connector.JettyClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Based on https://github.com/nhenneaux/jersey-http2-jetty-connector/blob/master/jersey-http2-jetty-connector/src/test/java/org/glassfish/jersey/jetty/connector/Http2Test.java
 */

class JerseyBug4332Test {

    private static final String KEYSTORE_PASSWORD = "TEST==ONLY==jks-keystore-password";
    private static final String CERTIFICATE_PASSWORD = "TEST==ONLY==vXzZO7sjy3jP4U7tDlihgOaf+WLlA7/vqnqlkLZzzQo=";
    private static final int PORT = 2223;
    private static final int N_THREADS = 4;
    private static final int ITERATIONS = 10_000;

    @Test
    void shouldFailWithSSLHandshakeException() throws Exception {
        final var clientConfig = new ClientConfig().property(JettyClientProperties.ENABLE_SSL_HOSTNAME_VERIFICATION, Boolean.TRUE);
        final var tlsSecurityConfiguration = tlsConfig();
        final var truststore = getKeyStore("jks-password".toCharArray(), "truststore.jks");
        try (var ignored = jerseyServer(
                tlsSecurityConfiguration,
                DummyRestService.class
        )) {
            final AtomicInteger counter = new AtomicInteger();
            final Runnable runnable = () -> {
                final DummyRestApi client = getClient(truststore, clientConfig);
                for (int i = 0; i < ITERATIONS; i++) {

                    // =====================================================================
                    // Here we artificially simulate the defaultSSLSocketFactory replacement
                    // =====================================================================
                    HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());

                    client.hello();
                    counter.incrementAndGet();
                }
            };
            final Set<Thread> threads = IntStream
                    .range(0, N_THREADS)
                    .mapToObj(i -> runnable)
                    .map(Thread::new)
                    .collect(Collectors.toSet());

            threads.forEach(Thread::start);

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(N_THREADS * ITERATIONS, counter.get());
        }
    }

    private static DummyRestApi getClient(final KeyStore trustStore, final ClientConfig configuration) {
        return WebResourceFactory.newResource(
                DummyRestApi.class,
                ClientBuilder.newBuilder()
                        .register(new JacksonJsonProvider())
                        .trustStore(trustStore)
                        .withConfig(configuration)
                        .build()
                        .target("https://localhost:" + JerseyBug4332Test.PORT)
        );
    }

    private static AutoCloseable jerseyServer(final TlsSecurityConfiguration tlsSecurityConfiguration, final Class<?>... serviceClasses) {
        return new AutoCloseable() {
            private final Server server;

            {
                this.server = new Server();
                final ServerConnector http2Connector = new ServerConnector(server, getConnectionFactories(tlsSecurityConfiguration));
                http2Connector.setPort(JerseyBug4332Test.PORT);
                server.addConnector(http2Connector);

                final ServletContextHandler context = new ServletContextHandler(server, "/*", ServletContextHandler.GZIP);
                final ServletHolder servlet = new ServletHolder(new ServletContainer(new ResourceConfig() {
                    {
                        for (Class<?> serviceClass : serviceClasses) {
                            register(serviceClass);
                        }
                    }
                }));

                context.addServlet(servlet, "/*");

                try {
                    server.start();
                } catch (Exception e) {
                    try {
                        close();
                    } catch (RuntimeException closeException) {
                        final MultiException multiException = new MultiException();
                        multiException.add(e);
                        multiException.add(closeException);
                        throw new IllegalStateException(multiException);
                    }
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void close() {
                if (server != null) {
                    try {
                        server.stop();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    } finally {
                        server.destroy();
                    }
                }
            }
        };
    }

    private static KeyStore getKeyStore(final char[] password, final String keystoreClasspathLocation) {
        KeyStore keystore;
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream myKeys = JerseyBug4332Test.class.getClassLoader().getResourceAsStream(keystoreClasspathLocation)) {
            keystore.load(myKeys, password);
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
        return keystore;
    }

    private static ConnectionFactory[] getConnectionFactories(final TlsSecurityConfiguration tlsSecurityConfiguration) {
        final HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        final HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);
        final SslContextFactory sslContextFactory = new SslContextFactory.Server.Server();

        sslContextFactory.setKeyStore(tlsSecurityConfiguration.keyStore);
        sslContextFactory.setKeyManagerPassword(tlsSecurityConfiguration.certificatePassword);
        sslContextFactory.setCertAlias(tlsSecurityConfiguration.certificateAlias);
        sslContextFactory.setIncludeProtocols(tlsSecurityConfiguration.protocol);
        sslContextFactory.setProtocol(tlsSecurityConfiguration.protocol);

        return new ConnectionFactory[]{
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                h2,
                new HttpConnectionFactory(httpsConfig)};
    }

    private TlsSecurityConfiguration tlsConfig() {
        return new TlsSecurityConfiguration(
                getKeyStore(KEYSTORE_PASSWORD.toCharArray(), "keystore.jks"),
                "server",
                CERTIFICATE_PASSWORD,
                "TLSv1.2"
        );
    }

    private static class TlsSecurityConfiguration {
        private final KeyStore keyStore;
        private final String certificateAlias;
        private final String certificatePassword;
        private final String protocol;

        private TlsSecurityConfiguration(final KeyStore keyStore, final String certificateAlias, final String certificatePassword, final String protocol) {
            this.keyStore = keyStore;
            this.certificateAlias = certificateAlias;
            this.certificatePassword = certificatePassword;
            this.protocol = protocol;
        }
    }

}
