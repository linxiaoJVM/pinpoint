/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.context.scope.TraceScope;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.proxy.ProxyHttpHeaderRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestTraceReader;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServerRequestRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.ParameterRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.RemoteAddressResolverFactory;
import com.navercorp.pinpoint.plugin.reactor.netty.*;
import com.navercorp.pinpoint.plugin.reactor.netty.server.ParameterRecorderFactory;
import com.navercorp.pinpoint.plugin.reactor.netty.server.ReactorNettyHttpHeaderFilter;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * Created by linxiao on 2018/12/18.
 */
public class DispatcherHandlerInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();


    private TraceContext traceContext;
    private MethodDescriptor descriptor;

    public DispatcherHandlerInterceptor(final TraceContext traceContext, final MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.descriptor = methodDescriptor;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            if (!validate(args)) {
                // invalid args.
                return;
            }

            ServerHttpRequest request = ((ServerWebExchange) args[0]).getRequest();
            if (isDebug) {
                logger.debug("args[0]: {}, ServerHttpRequest: {}", args[0], request);
            }

            final SpanEventRecorder recorder = trace.traceBlockBegin();
            if (isDebug) {
                logger.debug("SpanEventRecorder {}", recorder);
            }

            recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_SERVER_METHOD);
//
            // get asyncContext from HttpServerOperations
            final AsyncContext asyncContext = getAsyncContext(((AbstractServerHttpRequest)request).getNativeRequest());
            //args[0] is DefaultServerWebExchange
            ((AsyncContextAccessor) args[0])._$PINPOINT$_setAsyncContext(asyncContext);

            if (isDebug) {
                logger.debug("Set closeable-AsyncContext {}", asyncContext);
            }
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", t.getMessage(), t);
            }
        }
    }

    private boolean validate(final Object[] args) {
        if (args == null || args.length < 1) {
            if (isDebug) {
                logger.debug("Invalid args object. args={}.", args);
            }
            return false;
        }

        if (!(((ServerWebExchange) args[0]).getRequest() instanceof ServerHttpRequest)) {
            if (isDebug) {
                logger.debug("Invalid args[0] object. {}.", args[0]);
            }
            return false;
        }

        if (!((args[0]) instanceof AsyncContextAccessor)) {
            if (isDebug) {
                logger.debug("Invalid args[0] object. Need metadata accessor({}).", AsyncContextAccessor.class.getName());
            }
            return false;
        }

        return true;
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            recorder.recordException(throwable);
        } finally {
            trace.traceBlockEnd();
        }
    }

    protected AsyncContext getAsyncContext(Object target) {
        return AsyncContextAccessorUtils.getAsyncContext(target);
    }
}
