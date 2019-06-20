package com.navercorp.pinpoint.plugin.reactor.netty.server.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
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
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyHttpServerMethodDescriptor;
import com.navercorp.pinpoint.plugin.reactor.netty.server.HttpServerRequestAdaptor;
import com.navercorp.pinpoint.plugin.reactor.netty.server.ParameterRecorderFactory;
import com.navercorp.pinpoint.plugin.reactor.netty.server.ReactorNettyHttpHeaderFilter;
import com.navercorp.pinpoint.plugin.reactor.netty.server.ReactorNettyHttpServerConfig;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerState;

/**
 * Created by linxiao on 2019/3/14.
 */
public class HttpServerHandleInterceptior implements AroundInterceptor {
    private static final String SCOPE_NAME = "##REACTOR_NETTY_SERVER_CONNECTION_TRACE";
    private static final ReactorNettyHttpServerMethodDescriptor REACTOR_NETTY_HTTP_SERVER_METHOD_DESCRIPTOR = new ReactorNettyHttpServerMethodDescriptor();

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isTrace = logger.isTraceEnabled();

    private final Filter<String> excludeUrlFilter;

    private final ProxyHttpHeaderRecorder<HttpServerRequest> proxyHttpHeaderRecorder;
    private final ReactorNettyHttpHeaderFilter httpHeaderFilter;
    private final ServerRequestRecorder<HttpServerRequest> serverRequestRecorder;
    private final RequestTraceReader<HttpServerRequest> requestTraceReader;
    private final ParameterRecorder<HttpServerRequest> parameterRecorder;
    private   RequestAdaptor<HttpServerRequest> requestAdaptor;

    private TraceContext traceContext;
    private MethodDescriptor descriptor;

    public HttpServerHandleInterceptior(final TraceContext traceContext, final MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.descriptor = methodDescriptor;

        final ReactorNettyHttpServerConfig config = new ReactorNettyHttpServerConfig(traceContext.getProfilerConfig());
        this.excludeUrlFilter = config.getExcludeUrlFilter();

        requestAdaptor = new HttpServerRequestAdaptor();
        requestAdaptor = RemoteAddressResolverFactory.wrapRealIpSupport(requestAdaptor, config.getRealIpHeader(), config.getRealIpEmptyValue());
        this.parameterRecorder = ParameterRecorderFactory.newParameterRecorderFactory(config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());

        this.proxyHttpHeaderRecorder = new ProxyHttpHeaderRecorder<>(traceContext.getProfilerConfig().isProxyHttpHeaderEnable(), requestAdaptor);
        this.httpHeaderFilter = new ReactorNettyHttpHeaderFilter(config.isHidePinpointHeader());
        this.serverRequestRecorder = new ServerRequestRecorder<>(requestAdaptor);
        this.requestTraceReader = new RequestTraceReader<>(traceContext, requestAdaptor, true);
        traceContext.cacheApi(REACTOR_NETTY_HTTP_SERVER_METHOD_DESCRIPTOR);
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        logger.debug("The traceContext {}", traceContext);
        if (traceContext.currentRawTraceObject() != null) {
            // duplicate trace.
            logger.debug("duplicate trace {}", traceContext.currentRawTraceObject());
            return;
        }
        if (!validate(args)) {
            return;
        }

        try {
            HttpServerRequest request = (HttpServerRequest) args[0];
            if (isDebug) {
                logger.debug("HttpServerRequest {}", request);
            }

            // create trace for standalone entry point.
            final Trace trace = createTrace(request);
            if (trace == null) {
                logger.debug("trace is null");
                return;
            }

            entryScope(trace);
            this.httpHeaderFilter.filter(request);

            if (!trace.canSampled()) {
                return;
            }

            final SpanEventRecorder recorder = trace.traceBlockBegin();
            if (isDebug) {
                logger.debug("SpanEventRecorder {}", recorder);
            }

            recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_SERVER_METHOD);

            // make asynchronous trace-id
            final AsyncContext asyncContext = recorder.recordNextAsyncContext(true);

            ((AsyncContextAccessor) request)._$PINPOINT$_setAsyncContext(asyncContext);
            //((AbstractServerHttpRequest)request).getNativeRequest()  is HttpServerOperations
//            ((AsyncContextAccessor) ((AbstractServerHttpRequest)request).getNativeRequest())._$PINPOINT$_setAsyncContext(asyncContext);

            if (isDebug) {
                logger.debug("Set closeable-AsyncContext {}", asyncContext);
            }
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", t.getMessage(), t);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            logger.debug("The  Trace is null, the traceContext is {}", traceContext);
            return;
        }

        if (!hasScope(trace)) {
            // not reactor-netty trace.
            return;
        }

        if (!validate(args)) {
            return;
        }

        if (!leaveScope(trace)) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to leave scope. trace={}, sampled={}", trace, trace.canSampled());
            }
            // delete unstable trace.
            deleteTrace(trace);
            return;
        }

        if (!isEndScope(trace)) {
            // ignored recursive call.
            return;
        }

        if (!trace.canSampled()) {
            deleteTrace(trace);
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            recorder.recordException(throwable);

            final HttpServerRequest request = (HttpServerRequest) args[0];
            parameterRecorder.record(recorder, request, throwable);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER. Caused:{}", t.getMessage(), t);
            }
        } finally {
            trace.traceBlockEnd();
            deleteTrace(trace);
        }
    }
    private Trace createTrace(final HttpServerRequest request) {
        final String requestURI = requestAdaptor.getRpcName(request);
//        logger.debug("requestURI:{}, excludeUrlFilter:{}", requestURI, excludeUrlFilter);
        //过滤不需要监控的url
        if (requestURI != null && excludeUrlFilter.filter(requestURI)) {
            // skip request.
            if (isDebug) {
                logger.debug("filter requestURI:{}", requestURI);
            }
            return null;
        }

        Trace read = this.requestTraceReader.read(request);
        final Trace trace = read;
        if (isDebug) {
            logger.debug("The begin Trace object: {}",trace);
        }

        if (trace.canSampled()) {
            final SpanRecorder recorder = trace.getSpanRecorder();
            if (isDebug) {
                logger.debug("The SpanRecorder object: {}",recorder);
            }

            // root
            recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_SERVER);
            recorder.recordApi(REACTOR_NETTY_HTTP_SERVER_METHOD_DESCRIPTOR);
            this.serverRequestRecorder.record(recorder, request);
            // record proxy HTTP header.
            this.proxyHttpHeaderRecorder.record(recorder, request);
        }

        if (!initScope(trace)) {
            // invalid scope.
            deleteTrace(trace);
            return null;
        }
        if (isDebug) {
            logger.debug("The end Trace object: {}",trace);
        }
        return trace;
    }

    private void deleteTrace(final Trace trace) {
        if (isDebug) {
            logger.debug("Delete async trace {}.", trace);
        }
        traceContext.removeTraceObject();
        trace.close();
    }



    private boolean initScope(final Trace trace) {
        // add user scope.
        final TraceScope oldScope = trace.addScope(SCOPE_NAME);
        if (oldScope != null) {
            // delete corrupted trace.
            if (logger.isInfoEnabled()) {
                logger.info("Duplicated trace scope={}.", oldScope.getName());
            }
            return false;
        }

        return true;
    }

    private void entryScope(final Trace trace) {
        final TraceScope scope = trace.getScope(SCOPE_NAME);
        if (scope != null) {
            scope.tryEnter();
            if (isDebug) {
                logger.debug("Try enter trace scope={}", scope.getName());
            }
        }
    }

    private boolean leaveScope(final Trace trace) {
        final TraceScope scope = trace.getScope(SCOPE_NAME);
        if (scope != null) {
            if (scope.canLeave()) {
                scope.leave();
                if (isDebug) {
                    logger.debug("Leave trace scope={}", scope.getName());
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean hasScope(final Trace trace) {
        final TraceScope scope = trace.getScope(SCOPE_NAME);
        return scope != null;
    }

    private boolean isEndScope(final Trace trace) {
        final TraceScope scope = trace.getScope(SCOPE_NAME);
        return scope != null && !scope.isActive();
    }

    private boolean validate(final Object[] args) {
        if (!( (ConnectionObserver.State)args[1] == HttpServerState.REQUEST_RECEIVED)) {
            if (isDebug) {
                logger.debug("The HttpServerState is not [REQUEST_RECEIVED],current server state is  ({}).", args[1]);
            }
            return false;
        }

        return true;
    }
}
