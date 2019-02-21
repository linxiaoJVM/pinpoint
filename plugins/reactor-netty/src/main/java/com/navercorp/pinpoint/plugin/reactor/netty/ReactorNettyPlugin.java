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
        transformTemplate.transform("org.springframework.web.reactive.DispatcherHandler", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                final InstrumentMethod handleRequestMethod = target.getDeclaredMethod("handle",
                        "org.springframework.web.server.ServerWebExchange");
                if (handleRequestMethod != null) {
                    handleRequestMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.DispatcherHandlerInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addServerHandlerStart() {
        transformTemplate.transform("reactor.ipc.netty.http.server.HttpServerOperations", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());
                //Complete status
                final InstrumentMethod onOutboundComplete = target.getDeclaredMethod("onOutboundComplete");
                if (onOutboundComplete != null) {
                    onOutboundComplete.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.OutboundCompleteInterceptor");
                }

                // entry point
                final InstrumentMethod sendMethod = target.getDeclaredMethod("onHandlerStart");
                if (sendMethod != null) {
                    sendMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.ServerHandlerStartInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addServerWebExchange() {
        addAsyncContextAccessor("org.springframework.web.server.adapter.DefaultServerWebExchange");
    }

    private void addAsyncContextAccessor(final String className) {
        transformTemplate.transform(className, new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());
                return target.toBytecode();
            }
        });
    }

    //------------------------      server end     ----------------------------------------

    //------------------------      client begin    ----------------------------------------
    private void addNettyRoutingFilter() {
        transformTemplate.transform("org.springframework.cloud.gateway.filter.NettyRoutingFilter", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);

                target.addGetter("com.navercorp.pinpoint.plugin.reactor.netty.client.HttpClientGetter", "httpClient");

                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("filter",
                        "org.springframework.web.server.ServerWebExchange",
                        "org.springframework.cloud.gateway.filter.GatewayFilterChain");
                if (handleExceptionMethod != null) {
                    handleExceptionMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.NettyRoutingFilterInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addHttpClient() {
        transformTemplate.transform("reactor.ipc.netty.http.client.HttpClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());

                for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.name("delete", "get", "patch", "post", "put", "ws"))) {
                    if (method != null) {
                        method.addScopedInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientInterceptor", ReactorNettyConstants.HTTP_CLIENT_REQUEST_SCOPE);
                    }
                }

                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("request",
                        "io.netty.handler.codec.http.HttpMethod",
                        "java.lang.String",
                        "java.util.function.Function");
                if (handleExceptionMethod != null) {
                    handleExceptionMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientImplRequestInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addHttpClientOperations() {
        transformTemplate.transform("reactor.ipc.netty.http.client.HttpClientOperations", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());

                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("checkResponseCode",
                        "io.netty.handler.codec.http.HttpResponse");
                if (handleExceptionMethod != null) {
                    handleExceptionMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientOperationsResponseCodeInterceptor");
                }

//                final InstrumentMethod sendMethod = target.getDeclaredMethod("onHandlerStart");
//                if (sendMethod != null) {
//                    sendMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.ClientHandlerStartInterceptor");
//                }

                return target.toBytecode();
            }
        });
    }

    private void addMonoHttpClientResponse() {
        transformTemplate.transform("reactor.ipc.netty.http.client.MonoHttpClientResponse", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());

                target.addGetter("com.navercorp.pinpoint.plugin.reactor.netty.client.UrlGetter", "startURI");

//                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("subscribe",
//                        "reactor.core.CoreSubscriber");
//                if (handleExceptionMethod != null) {
//                    handleExceptionMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerInterceptor");
//                }


                return target.toBytecode();
            }
        });
    }

    private void addHttpClientHandler() {
        transformTemplate.transform("reactor.ipc.netty.http.client.MonoHttpClientResponse$HttpClientHandler", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());

                target.addField(MonoHttpClientResponseAccess.class.getName());

                final InstrumentMethod constructor = target.getConstructor("reactor.ipc.netty.http.client.MonoHttpClientResponse",
                        "reactor.ipc.netty.http.client.MonoHttpClientResponse$ReconnectableBridge");
                if (constructor != null) {
                    constructor.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerConstructorInterceptor");
                }

                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("apply",
                        "reactor.ipc.netty.NettyInbound",
                        "reactor.ipc.netty.NettyOutbound");
                if (handleExceptionMethod != null) {
                    handleExceptionMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor.HttpClientHandlerInterceptor");
                }

                return target.toBytecode();
            }
        });
    }



    @Override
    public void setTransformTemplate(MatchableTransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }


}
