package com.navercorp.pinpoint.plugin.reactor.netty.interceptor;


import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.plugin.http.HttpStatusCodeRecorder;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import reactor.ipc.netty.http.server.HttpServerResponse;

/**
 * Created by linxiao on 2018/12/19.
 */
public class HttpServerOperationsInterceptor extends AsyncContextSpanEventEndPointInterceptor{

    private final HttpStatusCodeRecorder httpStatusCodeRecorder;

    public HttpServerOperationsInterceptor(MethodDescriptor methodDescriptor, TraceContext traceContext) {
        super(traceContext, methodDescriptor);

        this.httpStatusCodeRecorder = new HttpStatusCodeRecorder(traceContext.getProfilerConfig().getHttpStatusCodeErrors());
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncContext asyncContext, Object target, Object[] args) {

    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(methodDescriptor);
        recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_HTTP_SERVER);
        recorder.recordException(throwable);

        if (target instanceof HttpServerResponse) {
            final HttpServerResponse response = (HttpServerResponse) target;
            // TODO more simple.
            final AsyncContext asyncContext = getAsyncContext(target);
            if (asyncContext != null) {
                final Trace trace = asyncContext.currentAsyncTraceObject();
                if (trace != null) {
                    final SpanRecorder spanRecorder = trace.getSpanRecorder();
                    this.httpStatusCodeRecorder.record(spanRecorder, response.status().code());
                }
            }
        }
    }
}
