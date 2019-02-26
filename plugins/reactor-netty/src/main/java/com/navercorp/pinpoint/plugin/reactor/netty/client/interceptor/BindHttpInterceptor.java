package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

/**
 * Created by linxiao on 2019/2/25.
 */
public class BindHttpInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public BindHttpInterceptor(final TraceContext traceContext, final MethodDescriptor methodDescriptor) {
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        if (args[1] instanceof AsyncContextAccessor) {
            AsyncContext asyncContext = ((AsyncContextAccessor) args[1])._$PINPOINT$_getAsyncContext();
            logger.debug("args[1]  AsyncContextAccessor {} ", asyncContext);
            if (target instanceof AsyncContextAccessor) {
                ((AsyncContextAccessor) target)._$PINPOINT$_setAsyncContext(asyncContext);
                logger.debug("target  AsyncContextAccessor {} ", asyncContext);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }
    }
}
