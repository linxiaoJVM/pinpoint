package com.navercorp.pinpoint.plugin.reactor.netty.client.interceptor;

import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AsyncContextSpanEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by linxiao on 2019/1/9.
 */
public class HttpClientOperationsResponseCodeInterceptor extends AsyncContextSpanEventSimpleAroundInterceptor {

    public HttpClientOperationsResponseCodeInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncContext asyncContext, Object target, Object[] args) {
//        if (!validate(args)) {
//            return;
//        }

        final HttpResponse response = (HttpResponse) args[0];
//        int code = response.getStatus().code();
        int code = response.status().code();
        recorder.recordAttribute(AnnotationKey.HTTP_STATUS_CODE, code);

//        ((AsyncContextAccessor) response)._$PINPOINT$_setAsyncContext(asyncContext);
    }

    private boolean validate(final Object[] args) {
//        if (args == null || args.length < 1 || !(args[0] instanceof HttpClientResponseImpl)) {
//            if (isDebug) {
//                logger.debug("Invalid args[0] object. args={}.", args);
//            }
//            return false;
//        }
//
//        if (!(args[0] instanceof AsyncContextAccessor)) {
//            if (isDebug) {
//                logger.debug("Invalid args[0] object. Need metadata accessor({}).", AsyncContextAccessor.class.getName());
//            }
//            return false;
//        }

        return true;
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(methodDescriptor);
        recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_CLIENT_METHOD);
        recorder.recordException(throwable);
    }
}
