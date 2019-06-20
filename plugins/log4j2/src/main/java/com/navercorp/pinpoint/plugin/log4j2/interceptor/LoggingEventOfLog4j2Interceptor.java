/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.log4j2.interceptor;

import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor0;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import org.apache.logging.log4j.ThreadContext;

/**
 * @author minwoo.jung
 */
public class LoggingEventOfLog4j2Interceptor implements AroundInterceptor0 {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private static final String TRANSACTION_ID = "PtxId";
    private static final String SPAN_ID = "PspanId";

    private final TraceContext traceContext;

    public LoggingEventOfLog4j2Interceptor(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object target) {
        if (isDebug) {
            logger.beforeInterceptor(target, null);
        }

        Trace trace = traceContext.currentTraceObject();
        
        if (trace == null) {
            logger.debug("trace is null ");
            ThreadContext.remove(TRANSACTION_ID);
            ThreadContext.remove(SPAN_ID);
            return;
        } else {
            logger.debug("PtxId={},PspanId={}", trace.getTraceId().getTransactionId(), String.valueOf(trace.getTraceId().getSpanId()));
            ThreadContext.put(TRANSACTION_ID, trace.getTraceId().getTransactionId());
            ThreadContext.put(SPAN_ID, String.valueOf(trace.getTraceId().getSpanId()));
        }
    }

// #1375 Workaround java level Deadlock
// https://oss.navercorp.com/pinpoint/pinpoint-naver/issues/1375
//    @IgnoreMethod
    @Override
    public void after(Object target, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, null);
        }
    }
}
