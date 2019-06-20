package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

/**
 * Created by linxiao on 2019/3/22.
 */
public class TcpClientConnectInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public TcpClientConnectInterceptor(final TraceContext traceContext, final MethodDescriptor methodDescriptor) {
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        if (target instanceof AsyncContextAccessor) {
            AsyncContext asyncContext = ((AsyncContextAccessor) target)._$PINPOINT$_getAsyncContext();
            logger.debug("target  {} AsyncContextAccessor {} ", target, asyncContext);
            if (args[0] instanceof AsyncContextAccessor) {
                ((AsyncContextAccessor) args[0])._$PINPOINT$_setAsyncContext(asyncContext);
                logger.debug("args[0]  {} AsyncContextAccessor {} ", args[0], asyncContext);
            }else {
                logger.debug("args[0] {} not AsyncContextAccessor  ", args[0]);
            }
        } else {
            logger.debug("target {} not AsyncContextAccessor ", target);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }
    }
}
