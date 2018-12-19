package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

/**
 * Created by linxiao on 2018/12/6.
 */
public class ReactorNettyTypeProvider implements TraceMetadataProvider {
    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(ReactorNettyConstants.REACTOR_NETTY);
        context.addServiceType(ReactorNettyConstants.REACTOR_NETTY_METHOD);
    }
}
