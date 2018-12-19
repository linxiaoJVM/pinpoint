package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.resolver.ConditionProvider;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by linxiao on 2018/12/17.
 */
public class ReactorNettyDetector implements ApplicationTypeDetector {
    private static final String DEFAULT_BOOTSTRAP_MAIN = "reactor.ipc.netty.http.server.HttpServer";
    private static final String REQUIRED_CLASS = "reactor.ipc.netty.http.server.HttpServer";

    private final List<String> bootstrapMains;

    public ReactorNettyDetector(List<String> bootstrapMains) {
        if (CollectionUtils.isEmpty(bootstrapMains)) {
            this.bootstrapMains = Arrays.asList(DEFAULT_BOOTSTRAP_MAIN);
        } else {
            this.bootstrapMains = bootstrapMains;
        }
    }

    @Override
    public ServiceType getApplicationType() {
        return ReactorNettyConstants.REACTOR_NETTY;
    }

    @Override
    public boolean detect(ConditionProvider provider) {
        return provider.checkMainClass(bootstrapMains) &&
                provider.checkForClass(REQUIRED_CLASS);
    }
}
