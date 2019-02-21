package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.AsyncContextSpanEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientHeaderAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.DefaultRequestTraceWriter;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestTraceWriter;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import com.navercorp.pinpoint.plugin.reactor.netty.client.HttpClientRequestClientHeaderAdaptor;
import com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor.AsyncContextSpanEventEndPointInterceptor;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by linxiao on 2019/1/9.
 */
public class HttpClientImplRequestInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

//    private final RequestTraceWriter<HttpClientRequest> requestTraceWriter;
//    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;

    public HttpClientImplRequestInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        if (traceContext == null) {
            throw new NullPointerException("traceContext must not be null");
        }

//        this.traceContext = traceContext;
        this.methodDescriptor = descriptor;

//        ClientHeaderAdaptor<HttpClientRequest> clientHeaderAdaptor = new HttpClientRequestClientHeaderAdaptor();
//        this.requestTraceWriter = new DefaultRequestTraceWriter<HttpClientRequest>(clientHeaderAdaptor, traceContext);
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        final AsyncContext asyncContext = getAsyncContext(target);
        if (asyncContext == null) {
            logger.debug("AsyncContext not found");
            return;
        }

        final Trace trace = getAsyncTrace(asyncContext);
        if (trace == null) {
            return;
        }

        trace.traceBlockBegin();
    }


    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final AsyncContext asyncContext = getAsyncContext(target);
        if (asyncContext == null) {
            logger.debug("AsyncContext not found");
            return;
        }

        final Trace trace = getAsyncTrace(asyncContext);
        if (trace == null) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(methodDescriptor);
            recorder.recordException(throwable);
            recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_CLIENT_METHOD);

            ((AsyncContextAccessor) result)._$PINPOINT$_setAsyncContext(asyncContext);
            if (isDebug) {
                logger.debug("Set asyncContext {}", asyncContext);
            }

            final String hostAndPort = toHostAndPort(args);
            if (hostAndPort != null) {
                recorder.recordAttribute(AnnotationKey.HTTP_INTERNAL_DISPLAY, hostAndPort);
                if (isDebug) {
                    logger.debug("Set hostAndPort {}", hostAndPort);
                }
            }
        } finally {
            trace.traceBlockEnd();
//            deleteAsyncContext(trace, asyncContext);
        }
    }

    private String toHostAndPort(final Object[] args) {
        if (args != null && args.length == 3) {
            final String url = (String) args[1];
            URL url1 = null;
            try {
                url1 = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            String host = url1.getHost();
            int port = url1.getPort();
            host = host + ':' + port;
            return host;
        }

        if (isDebug) {
            logger.debug("Invalid args[]. args={}.", args);
        }
        return null;
    }

    protected AsyncContext getAsyncContext(Object target) {
        return AsyncContextAccessorUtils.getAsyncContext(target);
    }
    private Trace getAsyncTrace(AsyncContext asyncContext) {
        final Trace trace = asyncContext.continueAsyncTraceObject();
        if (trace == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to continue async trace. 'result is null'");
            }
            return null;
        }
        if (isDebug) {
            logger.debug("getAsyncTrace() trace {}, asyncContext={}", trace, asyncContext);
        }

        return trace;
    }
//    private void deleteAsyncContext(final Trace trace, AsyncContext asyncContext) {
//        if (isDebug) {
//            logger.debug("Delete async trace {}.", trace);
//        }
//
//        trace.close();
//        asyncContext.close();
//    }
}
