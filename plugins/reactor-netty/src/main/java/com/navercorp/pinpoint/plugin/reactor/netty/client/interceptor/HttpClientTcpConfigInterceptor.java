package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

/**
 * Created by linxiao on 2019/3/21.
 */
public class HttpClientTcpConfigInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public HttpClientTcpConfigInterceptor(final TraceContext traceContext, final MethodDescriptor methodDescriptor) {
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

        if (target instanceof AsyncContextAccessor) {
            AsyncContext asyncContext = ((AsyncContextAccessor) target)._$PINPOINT$_getAsyncContext();
            logger.debug("target  {}, AsyncContextAccessor {} ", target, asyncContext);
            if (result instanceof AsyncContextAccessor) {
                ((AsyncContextAccessor) result)._$PINPOINT$_setAsyncContext(asyncContext);
                logger.debug("result  {}, AsyncContextAccessor {} ", result, asyncContext);
            }else {
                logger.debug("result {} not AsyncContextAccessor", result);
            }

        } else {
            logger.debug("target {} not AsyncContextAccessor", target);
        }

    }
}
