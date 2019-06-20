/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.*;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

/**
 * Created by linxiao on 2018/12/17.
 */
public class ReactorNettyPlugin implements ProfilerPlugin, MatchableTransformTemplateAware {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isInfo = logger.isInfoEnabled();

    private MatchableTransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        final ReactorNettyConfig config = new ReactorNettyConfig(context.getConfig());
        if (logger.isInfoEnabled()) {
            logger.info("ReactorNettyConfig config:{}", config);
        }

        if (!config.isEnable() || (!config.isEnableHttpServer() )) {
            if (isInfo) {
                logger.info("Disable ReactorNettyPlugin.");
            }
            return;
        }

//        final ReactorNettyDetector detector = new ReactorNettyDetector(config.getBootstrapMains());
//        context.addApplicationTypeDetector(detector);

        //add AsyncContext
        addAsyncContext();

        if (config.isEnableHttpServer()) {
            if (isInfo) {
                logger.info("Adding ReactorNetty HTTP Server.");
            }
            // Entry ReactorNetty http server Point
            addServerHandlerStart();
            addDispatcherHandler();
            addOutboundComplete();
        }

        if (config.isEnableHttpClient()) {
            if (isInfo) {
                logger.info("Adding ReactorNetty HTTP Client.");
            }
            addNettyRoutingFilter();
            addHttpClient();
            addHttpClientTcpConfig();
            addTcpClientOperator();
            addHttpClientConfiguration();
            addHttpClientHandler();
        }
    }

    private void addServerHandlerStart() {
        transformTemplate.transform("reactor.netty.http.server.HttpServerHandle", HttpServerHandleTransform.class);
    }
    public static class HttpServerHandleTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            // entry point  0.8.4.RELEASE
            final InstrumentMethod onStateChange = target.getDeclaredMethod("onStateChange",
                    "reactor.netty.Connection","reactor.netty.ConnectionObserver$State");
            if (onStateChange != null) {
                onStateChange.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.HttpServerHandleInterceptior.class);
            }
            return target.toBytecode();
        }
    }

    private void addDispatcherHandler() {
        transformTemplate.transform("org.springframework.web.reactive.DispatcherHandler", DispatcherHandlerTransform.class);
    }
    public static class DispatcherHandlerTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            final InstrumentMethod handleRequestMethod = target.getDeclaredMethod("handle",
                    "org.springframework.web.server.ServerWebExchange");
            if (handleRequestMethod != null) {
                handleRequestMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.DispatcherHandlerInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    private void addOutboundComplete() {
        transformTemplate.transform("reactor.netty.http.server.HttpServerOperations", ServerHandlerStartTransform.class);
    }
    public static class ServerHandlerStartTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);
            //Complete status
            final InstrumentMethod onOutboundComplete = target.getDeclaredMethod("onOutboundComplete");
            if (onOutboundComplete != null) {
                onOutboundComplete.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.OutboundCompleteInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    //------------------------      server end     ----------------------------------------

    private void addAsyncContext() {
        addAsyncContextAccessor("org.springframework.web.server.adapter.DefaultServerWebExchange");
        addAsyncContextAccessor("io.netty.bootstrap.Bootstrap");
        addAsyncContextAccessor("reactor.netty.http.client.HttpClientOperations");
    }
    private void addAsyncContextAccessor(final String className) {
        transformTemplate.transform(className, AsyncContextAccessorTransform.class);
    }
    public static class AsyncContextAccessorTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);
            return target.toBytecode();
        }
    }

    //------------------------      client begin    ----------------------------------------
    private void addNettyRoutingFilter() {
        transformTemplate.transform("org.springframework.cloud.gateway.filter.NettyRoutingFilter", NettyRoutingFilterTransform.class);
    }
    public static class NettyRoutingFilterTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);

            target.addGetter(com.navercorp.pinpoint.plugin.reactor.netty.client.HttpClientGetter.class, "httpClient");

            final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("filter",
                    "org.springframework.web.server.ServerWebExchange",
                    "org.springframework.cloud.gateway.filter.GatewayFilterChain");
            if (handleExceptionMethod != null) {
                handleExceptionMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.NettyRoutingFilterInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    private void addHttpClient() {
        transformTemplate.transform("reactor.netty.http.client.HttpClient", HttpClientTransform.class);
    }
    public static class HttpClientTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            final InstrumentMethod tcpConfiguration = target.getDeclaredMethod("tcpConfiguration","java.util.function.Function");
            if (tcpConfiguration != null) {
                tcpConfiguration.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientTcpConfigInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    private void addHttpClientTcpConfig() {
        transformTemplate.transform("reactor.netty.http.client.HttpClientTcpConfig", HttpClientTcpConfigTransform.class);
    }
    public static class HttpClientTcpConfigTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);
            final InstrumentMethod tcpConfiguration = target.getDeclaredMethod("tcpConfiguration");
            if (tcpConfiguration != null) {
                tcpConfiguration.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientTcpConfigInterceptor.class);
            }
            return target.toBytecode();
        }
    }

    private void addTcpClientOperator() {
        transformTemplate.transform("reactor.netty.tcp.TcpClientOperator", TcpClientOperatorTransform.class);
    }
    public static class TcpClientOperatorTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            final InstrumentMethod connect = target.getDeclaredMethod("connect","io.netty.bootstrap.Bootstrap");

            if (connect != null) {
                connect.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.TcpClientConnectInterceptor.class);
            }
            return target.toBytecode();
        }
    }


    private void addHttpClientConfiguration() {
        transformTemplate.transform("reactor.netty.http.client.HttpClientConfiguration", HttpClientConfigurationTransform.class);
    }
    public static class HttpClientConfigurationTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            final InstrumentMethod connect = target.getDeclaredMethod("getAndClean","io.netty.bootstrap.Bootstrap");

            if (connect != null) {
                connect.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientConfigurationGetAndCleanInterceptor.class);
            }
            return target.toBytecode();
        }
    }

    private void addHttpClientHandler() {
        transformTemplate.transform("reactor.netty.http.client.HttpClientConnect$HttpClientHandler", HttpClientHandlerTransform.class);
    }
    public static class HttpClientHandlerTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            final InstrumentMethod connect = target.getConstructor("reactor.netty.http.client.HttpClientConfiguration",
                    "java.net.SocketAddress","reactor.netty.tcp.SslProvider",
                    "reactor.netty.tcp.ProxyProvider");
            if (connect != null) {
                connect.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerConstructorInterceptor.class);
            }

            final InstrumentMethod requestWithBody = target.getDeclaredMethod("requestWithBody",
                        "reactor.netty.http.client.HttpClientOperations");
            if (requestWithBody != null) {
                requestWithBody.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerInterceptor.class);
            }

            return target.toBytecode();
        }
    }



    @Override
    public void setTransformTemplate(MatchableTransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }


}
