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
import com.navercorp.pinpoint.bootstrap.interceptor.BasicMethodInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.plugin.reactor.netty.client.MonoHttpClientResponseAccess;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

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

        if (config.isEnableHttpServer()) {
            if (isInfo) {
                logger.info("Adding ReactorNetty HTTP Server.");
            }
            // Entry ReactorNetty http server Point
            addServerHandlerStart();
            //Entry webflux Point
            addDispatcherHandler();
            //add AsyncContext
            addServerWebExchange();
        }

        if (config.isEnableHttpClient()) {
            if (isInfo) {
                logger.info("Adding ReactorNetty HTTP Client.");
            }
            addNettyRoutingFilter();
            addHttpClient();
            addHttpClientOperations();
            addMonoHttpClientResponse();
            addHttpClientHandler();
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

    private void addServerHandlerStart() {
        transformTemplate.transform("reactor.ipc.netty.http.server.HttpServerOperations", ServerHandlerStartTransform.class);
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

            // entry point
            final InstrumentMethod sendMethod = target.getDeclaredMethod("onHandlerStart");
            if (sendMethod != null) {
                sendMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.ServerHandlerStartInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    private void addServerWebExchange() {
        addAsyncContextAccessor("org.springframework.web.server.adapter.DefaultServerWebExchange");
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
    //------------------------      server end     ----------------------------------------

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
        transformTemplate.transform("reactor.ipc.netty.http.client.HttpClient", HttpClientTransform.class);
    }
    public static class HttpClientTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.name("delete", "get", "patch", "post", "put", "ws"))) {
                if (method != null) {
                    method.addScopedInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientInterceptor.class, ReactorNettyConstants.HTTP_CLIENT_REQUEST_SCOPE);
                }
            }

            final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("request",
                    "io.netty.handler.codec.http.HttpMethod",
                    "java.lang.String",
                    "java.util.function.Function");
            if (handleExceptionMethod != null) {
                handleExceptionMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientImplRequestInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    private void addHttpClientOperations() {
        transformTemplate.transform("reactor.ipc.netty.http.client.HttpClientOperations", HttpClientOperationsTransform.class);
    }
    public static class HttpClientOperationsTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

//            final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("checkResponseCode",
//                    "io.netty.handler.codec.http.HttpResponse");
//            if (handleExceptionMethod != null) {
//                handleExceptionMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientOperationsResponseCodeInterceptor.class);
//            }

//            final InstrumentMethod bindHttp = target.getConstructor(
//                    "io.netty.channel.Channel",
//                    "java.util.function.BiFunction",
//                    "reactor.ipc.netty.channel.ContextHandler");
//            if (bindHttp != null) {
//                bindHttp.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.BindHttpInterceptor.class);
//            }

//            final InstrumentMethod sendMethod = target.getDeclaredMethod("onHandlerStart");
//            if (sendMethod != null) {
//                sendMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.ClientHandlerStartInterceptor.class);
//            }

            return target.toBytecode();
        }
    }

    private void addMonoHttpClientResponse() {
        transformTemplate.transform("reactor.ipc.netty.http.client.MonoHttpClientResponse", MonoHttpClientResponseTransform.class);
    }
    public static class MonoHttpClientResponseTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            target.addGetter(com.navercorp.pinpoint.plugin.reactor.netty.client.UrlGetter.class, "startURI");

            return target.toBytecode();
        }
    }

    private void addHttpClientHandler() {
        transformTemplate.transform("reactor.ipc.netty.http.client.MonoHttpClientResponse$HttpClientHandler",HttpClientHandlerTransform.class);
    }
    public static class HttpClientHandlerTransform implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            target.addField(MonoHttpClientResponseAccess.class);

            final InstrumentMethod constructor = target.getConstructor("reactor.ipc.netty.http.client.MonoHttpClientResponse",
                    "reactor.ipc.netty.http.client.MonoHttpClientResponse$ReconnectableBridge");
            if (constructor != null) {
                constructor.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerConstructorInterceptor.class);
            }

                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("apply",
                        "reactor.ipc.netty.NettyInbound",
                        "reactor.ipc.netty.NettyOutbound");
                if (handleExceptionMethod != null) {
                    handleExceptionMethod.addInterceptor(com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerInterceptor.class);
                }

            return target.toBytecode();
        }
    }



    @Override
    public void setTransformTemplate(MatchableTransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }


}
