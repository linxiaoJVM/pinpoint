package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.reactor.netty.client.HttpClientGetter;
import org.springframework.web.server.ServerWebExchangeDecorator;
//import reactor.ipc.netty.http.client.HttpClient;

/**
 * Created by linxiao on 2019/1/14.
 */
public class NettyRoutingFilterInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;

    public NettyRoutingFilterInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        final AsyncContext asyncContext = getAsyncContextFromArgs(args);
        if (asyncContext == null) {
            logger.debug("AsyncContext not found");
            return;
        }

//        final Trace trace = getAsyncTrace(asyncContext);
//        if (trace == null) {
//            logger.debug("trace not found");
//            return;
//        }

        Object httpClient = ((HttpClientGetter)target)._$PINPOINT$_getHttpClient();
        logger.debug("NettyRoutingFilterInterceptor httpClient is {}", httpClient);

        if (httpClient instanceof AsyncContextAccessor) {
            ((AsyncContextAccessor) httpClient)._$PINPOINT$_setAsyncContext(asyncContext);
            logger.debug("NettyRoutingFilterInterceptor  AsyncContextAccessor is {}", asyncContext);
        }

//        final SpanEventRecorder recorder = trace.traceBlockBegin();
//        recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_CLIENT);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }

        final AsyncContext asyncContext = getAsyncContextFromArgs(args);
        if (asyncContext == null) {
            logger.debug("AsyncContext not found");
            return;
        }
        asyncContext.close();
//
//        final Trace trace = getAsyncTrace(asyncContext);
//        if (trace == null) {
//            return;
//        }
//
//        try {
//            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
//            recorder.recordApi(methodDescriptor);
//            recorder.recordException(throwable);
//        } finally {
//            trace.traceBlockEnd();
////            deleteAsyncContext(trace, asyncContext);
//        }
    }

//    @Override
//    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncContext asyncContext, Object target, Object[] args) {
//        Object httpClient = ((HttpClientGetter)target)._$PINPOINT$_getHttpClient();
//        logger.debug("NettyRoutingFilterInterceptor httpClient is {}", httpClient);
//
//        if (httpClient instanceof AsyncContextAccessor) {
//            ((AsyncContextAccessor) httpClient)._$PINPOINT$_setAsyncContext(asyncContext);
//            logger.debug("NettyRoutingFilterInterceptor  AsyncContextAccessor is {}", asyncContext);
//        }
//    }

    protected AsyncContext getAsyncContextFromArgs(Object[] args) {
        return AsyncContextAccessorUtils.getAsyncContext(((ServerWebExchangeDecorator)args[0]).getDelegate());
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
