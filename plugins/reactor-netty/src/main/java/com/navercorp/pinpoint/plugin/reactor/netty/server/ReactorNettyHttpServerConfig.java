package com.navercorp.pinpoint.plugin.reactor.netty.server;

import com.navercorp.pinpoint.bootstrap.config.*;

/**
 * Created by linxiao on 2018/12/18.
 */
public class ReactorNettyHttpServerConfig {
    // server
    private final boolean traceRequestParam;
    private final Filter<String> excludeUrlFilter;
    private final String realIpHeader;
    private final String realIpEmptyValue;
    private final Filter<String> excludeProfileMethodFilter;
    private final boolean hidePinpointHeader;

    public ReactorNettyHttpServerConfig(ProfilerConfig config) {
        if (config == null) {
            throw new NullPointerException("config must not be null");
        }

        // runtime
        this.traceRequestParam = config.readBoolean("profiler.reactor.netty.http.server.tracerequestparam", true);
        final String tomcatExcludeURL = config.readString("profiler.reactor.netty.http.server.excludeurl", "");
        if (!tomcatExcludeURL.isEmpty()) {
            this.excludeUrlFilter = new ExcludePathFilter(tomcatExcludeURL);
        } else {
            this.excludeUrlFilter = new SkipFilter<String>();
        }
        this.realIpHeader = config.readString("profiler.reactor.netty.http.server.realipheader", null);
        this.realIpEmptyValue = config.readString("profiler.reactor.netty.http.server.realipemptyvalue", null);

        final String tomcatExcludeProfileMethod = config.readString("profiler.reactor.netty.http.server.excludemethod", "");
        if (!tomcatExcludeProfileMethod.isEmpty()) {
            this.excludeProfileMethodFilter = new ExcludeMethodFilter(tomcatExcludeProfileMethod);
        } else {
            this.excludeProfileMethodFilter = new SkipFilter<String>();
        }
        this.hidePinpointHeader = config.readBoolean("profiler.reactor.netty.http.server.hidepinpointheader", true);

    }

    public boolean isTraceRequestParam() {
        return traceRequestParam;
    }

    public Filter<String> getExcludeUrlFilter() {
        return excludeUrlFilter;
    }

    public String getRealIpHeader() {
        return realIpHeader;
    }

    public String getRealIpEmptyValue() {
        return realIpEmptyValue;
    }

    public Filter<String> getExcludeProfileMethodFilter() {
        return excludeProfileMethodFilter;
    }

    public boolean isHidePinpointHeader() {
        return hidePinpointHeader;
    }
}
