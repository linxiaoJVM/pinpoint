package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;
import com.navercorp.pinpoint.common.trace.ServiceTypeProperty;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;

/**
 * Created by linxiao on 2018/12/6.
 */
public class ReactorNettyConstants {
    private ReactorNettyConstants(){}


    public static final ServiceType REACTOR_NETTY = ServiceTypeFactory.of(1122, "REACTOR_NETTY", ServiceTypeProperty.RECORD_STATISTICS);
    public static final ServiceType REACTOR_NETTY_INTERNAL = ServiceTypeFactory.of(1123, "REACTOR_NETTY_INTERNAL","REACTOR_NETTY");
    //http server
    public static final ServiceType REACTOR_NETTY_HTTP_SERVER = ServiceTypeFactory.of(1124, "REACTOR_NETTY_HTTP_SERVER",RECORD_STATISTICS);
    public static final ServiceType REACTOR_NETTY_HTTP_SERVER_METHOD = ServiceTypeFactory.of(1125, "REACTOR_NETTY_HTTP_SERVER_METHOD","REACTOR_NETTY_HTTP_SERVER");
    //http client
    public static final ServiceType REACTOR_NETTY_HTTP_CLIENT = ServiceTypeFactory.of(9170, "REACTOR_NETTY_HTTP_CLIENT", RECORD_STATISTICS);
    public static final ServiceType REACTOR_NETTY_HTTP_CLIENT_METHOD = ServiceTypeFactory.of(9171, "REACTOR_NETTY_HTTP_CLIENT_METHOD", "REACTOR_NETTY_HTTP_CLIENT");

    public static final String HTTP_CLIENT_REQUEST_SCOPE = "HttpClientRequestScope";
    public static final String HTTP_CLIENT_CREATE_REQUEST_SCOPE = "HttpClientCreateRequestScope";

}
