package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.context.scope.TraceScope;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.*;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import com.navercorp.pinpoint.plugin.reactor.netty.client.*;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.channel.ChannelOperations;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

import java.net.SocketAddress;
import java.net.URI;

/**
 * Created by linxiao on 2019/1/30.
 */
public class ClientHandlerStartInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private TraceContext traceContext;
    private MethodDescriptor descriptor;
    private final ClientRequestRecorder<ClientRequestWrapper> clientRequestRecorder;
    //    private final CookieRecorder<HttpRequest> cookieRecorder;
    private final RequestTraceWriter<HttpClientRequest> requestTraceWriter;

    public ClientHandlerStartInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;

        final ReactorNettyHttpClientConfig config = new ReactorNettyHttpClientConfig(traceContext.getProfilerConfig());
        ClientRequestAdaptor<ClientRequestWrapper> clientRequestAdaptor = ClientRequestWrapperAdaptor.INSTANCE;
        this.clientRequestRecorder = new ClientRequestRecorder<>(config.isParam(), clientRequestAdaptor);

//        CookieExtractor<HttpRequest> cookieExtractor = new VertxCookieExtractor();
//        this.cookieRecorder = CookieRecorderFactory.newCookieRecorder(config.getHttpDumpConfig(), cookieExtractor);

        ClientHeaderAdaptor<HttpClientRequest> clientHeaderAdaptor = new HttpClientRequestClientHeaderAdaptor();
        this.requestTraceWriter = new DefaultRequestTraceWriter<>(clientHeaderAdaptor, traceContext);
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

        // entry scope.
//        entryAsyncTraceScope(trace);

        try {
            final SpanEventRecorder recorder = trace.traceBlockBegin();
//            if (!validate(args)) {
//                return;
//            }

            final HttpClientRequest request = (HttpClientRequest) target;
            final HttpHeaders headers = request.requestHeaders();
            if (headers == null) {
                // defense code.
                return;
            }
            logger.debug("before HttpHeaders {}",headers);

//            if (args[0] instanceof AsyncContextAccessor) {
//                ((AsyncContextAccessor) args[0])._$PINPOINT$_setAsyncContext(asyncContext);
//                logger.debug("HttpClientHandlerInterceptor  AsyncContextAccessor is {}", asyncContext);
//            }

        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", t.getMessage(), t);
            }
        }
    }

    private boolean validate(final Object[] args) {
        if (args == null || args.length < 2) {
            logger.debug("Invalid args object. args={}.", args);
            return false;
        }

        if (!(args[0] instanceof HttpRequest)) {
            logger.debug("Invalid args[0] object. {}.", args[0]);
            return false;
        }

        if (!(args[1] instanceof String)) {
            logger.debug("Invalid args[1] object. {}.", args[1]);
            return false;
        }

        return true;
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

//        Object target_tem = ((HttpClientResponseGetter)target)._$PINPOINT$_getParent();
        final AsyncContext asyncContext = getAsyncContext(target);
        if (asyncContext == null) {
            logger.debug("AsyncContext not found");
            return;
        }

        final Trace trace = getAsyncTrace(asyncContext);
        if (trace == null) {
            return;
        }

        // leave scope.
//        if (!leaveAsyncTraceScope(trace)) {
//            if (logger.isWarnEnabled()) {
//                logger.warn("Failed to leave scope of async trace {}.", trace);
//            }
//            // delete unstable trace.
//            deleteAsyncContext(trace, asyncContext);
//            return;
//        }


        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            recorder.recordException(throwable);
            recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_CLIENT);

//            if (!validate(args)) {
//                return;
//            }

            final HttpClientRequest request = (HttpClientRequest) target;

            final String host = toHostAndPort(target);
            // generate next trace id.
            final TraceId nextId = trace.getTraceId().getNextTraceId();
            recorder.recordNextSpanId(nextId.getSpanId());

            requestTraceWriter.write(request, nextId, host);

            final HttpHeaders headers = request.requestHeaders();
            logger.debug("after HttpHeaders {}",headers);

            ClientRequestWrapper clientRequest = new ReactorNettyHttpClientRequestWrapper(request, host);
            this.clientRequestRecorder.record(recorder, clientRequest, null);

        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER. Caused:{}", t.getMessage(), t);
            }
        } finally {
            trace.traceBlockEnd();
//            if (isAsyncTraceDestination(trace)) {
                deleteAsyncContext(trace, asyncContext);
//            }
        }
    }

    private String toHostAndPort(final Object target) {
        HttpClientRequest request = (HttpClientRequest) target;
        String host = request.requestHeaders().get(HttpHeaderNames.HOST);

        return host;
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
    private void deleteAsyncContext(final Trace trace, AsyncContext asyncContext) {
        if (isDebug) {
            logger.debug("Delete async trace {}.", trace);
        }

        trace.close();
        asyncContext.close();
    }
}
