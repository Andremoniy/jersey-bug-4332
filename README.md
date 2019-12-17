# jersey-bug-4332
A simple test which demonstrates the problem described in https://github.com/eclipse-ee4j/jersey/issues/4332

The test is based on https://github.com/nhenneaux/jersey-http2-jetty-connector/blob/master/jersey-http2-jetty-connector/src/test/java/org/glassfish/jersey/jetty/connector/Http2Test.java

The main idea of the test is to artifially replace the default SSL factory object:
```$xslt
                    HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
```

This causes failure of the logic in `org.glassfish.jersey.client.internal.HttpUrlConnector` class in this place:

```$xslt

            if (HttpsURLConnection.getDefaultSSLSocketFactory() == suc.getSSLSocketFactory()) {
                // indicates that the custom socket factory was not set
                suc.setSSLSocketFactory(sslSocketFactory.get());
            }
```

Just run the test to reproduce the problem:
`mvn clean test`

```$xslt
Exception in thread "Thread-8" javax.ws.rs.ProcessingException: javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at org.glassfish.jersey.client.internal.HttpUrlConnector.apply(HttpUrlConnector.java:260)
        at org.glassfish.jersey.client.ClientRuntime.invoke(ClientRuntime.java:254)
        at org.glassfish.jersey.client.JerseyInvocation.lambda$invoke$2(JerseyInvocation.java:770)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:292)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:274)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:205)
        at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:390)
        at org.glassfish.jersey.client.JerseyInvocation.invoke(JerseyInvocation.java:768)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.method(JerseyInvocation.java:414)
        at org.glassfish.jersey.client.proxy.WebResourceFactory.invoke(WebResourceFactory.java:331)
        at com.sun.proxy.$Proxy34.hello(Unknown Source)
        at com.github.andremoniy.jersey.bug4332.JerseyBug4332Test.lambda$shouldFailWithSSLHandshakeException$0(JerseyBug4332Test.java:71)
        at java.base/java.lang.Thread.run(Thread.java:830)
Caused by: javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at java.base/sun.security.ssl.Alert.createSSLException(Alert.java:131)
        at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:324)
        at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:267)
        at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:262)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.checkServerCerts(CertificateMessage.java:641)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.onCertificate(CertificateMessage.java:460)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.consume(CertificateMessage.java:360)
        at java.base/sun.security.ssl.SSLHandshake.consume(SSLHandshake.java:396)
        at java.base/sun.security.ssl.HandshakeContext.dispatch(HandshakeContext.java:444)
        at java.base/sun.security.ssl.HandshakeContext.dispatch(HandshakeContext.java:422)
        at java.base/sun.security.ssl.TransportContext.dispatch(TransportContext.java:181)
        at java.base/sun.security.ssl.SSLTransport.decode(SSLTransport.java:164)
        at java.base/sun.security.ssl.SSLSocketImpl.decode(SSLSocketImpl.java:1460)
        at java.base/sun.security.ssl.SSLSocketImpl.readHandshakeRecord(SSLSocketImpl.java:1368)
        at java.base/sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:437)
        at java.base/sun.net.www.protocol.https.HttpsClient.afterConnect(HttpsClient.java:567)
        at java.base/sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection.connect(AbstractDelegateHttpsURLConnection.java:171)
        at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1587)
        at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1515)
        at java.base/java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:527)
        at java.base/sun.net.www.protocol.https.HttpsURLConnectionImpl.getResponseCode(HttpsURLConnectionImpl.java:308)
        at org.glassfish.jersey.client.internal.HttpUrlConnector._apply(HttpUrlConnector.java:366)
        at org.glassfish.jersey.client.internal.HttpUrlConnector.apply(HttpUrlConnector.java:258)
        ... 12 more
Caused by: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at java.base/sun.security.validator.PKIXValidator.doBuild(PKIXValidator.java:384)
        at java.base/sun.security.validator.PKIXValidator.engineValidate(PKIXValidator.java:289)
        at java.base/sun.security.validator.Validator.validate(Validator.java:264)
        at java.base/sun.security.ssl.X509TrustManagerImpl.checkTrusted(X509TrustManagerImpl.java:231)
        at java.base/sun.security.ssl.X509TrustManagerImpl.checkServerTrusted(X509TrustManagerImpl.java:132)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.checkServerCerts(CertificateMessage.java:625)
        ... 30 more
Caused by: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at java.base/sun.security.provider.certpath.SunCertPathBuilder.build(SunCertPathBuilder.java:141)
        at java.base/sun.security.provider.certpath.SunCertPathBuilder.engineBuild(SunCertPathBuilder.java:126)
        at java.base/java.security.cert.CertPathBuilder.build(CertPathBuilder.java:297)
        at java.base/sun.security.validator.PKIXValidator.doBuild(PKIXValidator.java:379)
        ... 35 more
Exception in thread "Thread-11" javax.ws.rs.ProcessingException: javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at org.glassfish.jersey.client.internal.HttpUrlConnector.apply(HttpUrlConnector.java:260)
        at org.glassfish.jersey.client.ClientRuntime.invoke(ClientRuntime.java:254)
        at org.glassfish.jersey.client.JerseyInvocation.lambda$invoke$2(JerseyInvocation.java:770)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:292)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:274)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:205)
        at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:390)
        at org.glassfish.jersey.client.JerseyInvocation.invoke(JerseyInvocation.java:768)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.method(JerseyInvocation.java:414)
        at org.glassfish.jersey.client.proxy.WebResourceFactory.invoke(WebResourceFactory.java:331)
        at com.sun.proxy.$Proxy34.hello(Unknown Source)
        at com.github.andremoniy.jersey.bug4332.JerseyBug4332Test.lambda$shouldFailWithSSLHandshakeException$0(JerseyBug4332Test.java:71)
        at java.base/java.lang.Thread.run(Thread.java:830)
Caused by: javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at java.base/sun.security.ssl.Alert.createSSLException(Alert.java:131)
        at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:324)
        at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:267)
        at java.base/sun.security.ssl.TransportContext.fatal(TransportContext.java:262)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.checkServerCerts(CertificateMessage.java:641)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.onCertificate(CertificateMessage.java:460)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.consume(CertificateMessage.java:360)
        at java.base/sun.security.ssl.SSLHandshake.consume(SSLHandshake.java:396)
        at java.base/sun.security.ssl.HandshakeContext.dispatch(HandshakeContext.java:444)
        at java.base/sun.security.ssl.HandshakeContext.dispatch(HandshakeContext.java:422)
        at java.base/sun.security.ssl.TransportContext.dispatch(TransportContext.java:181)
        at java.base/sun.security.ssl.SSLTransport.decode(SSLTransport.java:164)
        at java.base/sun.security.ssl.SSLSocketImpl.decode(SSLSocketImpl.java:1460)
        at java.base/sun.security.ssl.SSLSocketImpl.readHandshakeRecord(SSLSocketImpl.java:1368)
        at java.base/sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:437)
        at java.base/sun.net.www.protocol.https.HttpsClient.afterConnect(HttpsClient.java:567)
        at java.base/sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection.connect(AbstractDelegateHttpsURLConnection.java:171)
        at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1587)
        at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1515)
        at java.base/java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:527)
        at java.base/sun.net.www.protocol.https.HttpsURLConnectionImpl.getResponseCode(HttpsURLConnectionImpl.java:308)
        at org.glassfish.jersey.client.internal.HttpUrlConnector._apply(HttpUrlConnector.java:366)
        at org.glassfish.jersey.client.internal.HttpUrlConnector.apply(HttpUrlConnector.java:258)
        ... 12 more
Caused by: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at java.base/sun.security.validator.PKIXValidator.doBuild(PKIXValidator.java:384)
        at java.base/sun.security.validator.PKIXValidator.engineValidate(PKIXValidator.java:289)
        at java.base/sun.security.validator.Validator.validate(Validator.java:264)
        at java.base/sun.security.ssl.X509TrustManagerImpl.checkTrusted(X509TrustManagerImpl.java:231)
        at java.base/sun.security.ssl.X509TrustManagerImpl.checkServerTrusted(X509TrustManagerImpl.java:132)
        at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.checkServerCerts(CertificateMessage.java:625)
        ... 30 more
Caused by: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at java.base/sun.security.provider.certpath.SunCertPathBuilder.build(SunCertPathBuilder.java:141)
        at java.base/sun.security.provider.certpath.SunCertPathBuilder.engineBuild(SunCertPathBuilder.java:126)
        at java.base/java.security.cert.CertPathBuilder.build(CertPathBuilder.java:297)
        at java.base/sun.security.validator.PKIXValidator.doBuild(PKIXValidator.java:379)
        ... 35 more

```