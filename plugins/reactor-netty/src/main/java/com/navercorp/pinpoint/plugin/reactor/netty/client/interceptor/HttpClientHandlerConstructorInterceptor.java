package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.reactor.netty.client.MonoHttpClientResponseAccess;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientResponse;

/**
 * Created by linxiao on 2019/1/16.
 */
public class HttpClientHandlerConstructorInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public HttpClientHandlerConstructorInterceptor(final TraceContext traceContext, final MethodDescriptor methodDescriptor) {
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }

        if (args[0] instanceof AsyncContextAccessor) {
            AsyncContext asyncContext = ((AsyncContextAccessor) args[0])._$PINPOINT$_getAsyncContext();
            logger.debug("args[0]  AsyncContextAccessor {} ", asyncContext);
            if (target instanceof AsyncContextAccessor) {
                ((AsyncContextAccessor) target)._$PINPOINT$_setAsyncContext(asyncContext);
                logger.debug("target  AsyncContextAccessor {} ", asyncContext);
            }else {
                logger.debug("target {} not AsyncContextAccessor", target);
            }

        } else {
            logger.debug("args[0] {} not AsyncContextAccessor", args[0]);
        }

        if (target instanceof MonoHttpClientResponseAccess) {
            ((MonoHttpClientResponseAccess) target)._$PINPOINT$_setHttpClientResponse((Mono<HttpClientResponse>)args[0]);
        }else {
            logger.debug("target {} not MonoHttpClientResponseAccess", target);
        }

    }
}
