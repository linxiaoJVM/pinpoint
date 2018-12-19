package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;

/**
 * Created by linxiao on 2018/12/6.
 */
public class ReactorNettyConstants {
    private ReactorNettyConstants(){}


    public static final ServiceType REACTOR_NETTY = ServiceTypeFactory.of(1122, "REACTOR_NETTY", RECORD_STATISTICS);
    public static final ServiceType REACTOR_NETTY_METHOD = ServiceTypeFactory.of(1123, "REACTOR_NETTY_METHOD");
    public static final ServiceType REACTOR_NETTY_HTTP_SERVER = ServiceTypeFactory.of(1124, "REACTOR_NETTY_HTTP_SERVER", "REACTOR_NETTY");

}
