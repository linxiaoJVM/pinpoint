package com.navercorp.pinpoint.plugin.reactor.netty.client;

import com.navercorp.pinpoint.bootstrap.config.DumpType;
import com.navercorp.pinpoint.bootstrap.config.HttpDumpConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

/**
 * Created by linxiao on 2019/1/10.
 */
public class ReactorNettyHttpClientConfig {
    // client
    private boolean param = true;
    private HttpDumpConfig httpDumpConfig;
    private boolean statusCode = true;

    public ReactorNettyHttpClientConfig(ProfilerConfig config) {
        this.param = config.readBoolean("profiler.reactor.netty.http.client.param", true);
        boolean cookie = config.readBoolean("profiler.reactor.netty.http.client.cookie", false);
        DumpType cookieDumpType = config.readDumpType("profiler.reactor.netty.http.client.cookie.dumptype", DumpType.EXCEPTION);
        int cookieSamplingRate = config.readInt("profiler.reactor.netty.http.client.cookie.sampling.rate", 1);
        int cookieDumpSize = config.readInt("profiler.reactor.netty.http.client.cookie.dumpsize", 1024);
        this.httpDumpConfig = HttpDumpConfig.get(cookie, cookieDumpType, cookieSamplingRate, cookieDumpSize, false, cookieDumpType, 1, 1024);
        this.statusCode = config.readBoolean("profiler.reactor.netty.http.client.entity.statuscode", true);
    }

    public boolean isParam() {
        return param;
    }

    public HttpDumpConfig getHttpDumpConfig() {
        return httpDumpConfig;
    }

    public boolean isStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VertxHttpClientConfig{");
        sb.append("param=").append(param);
        sb.append(", httpDumpConfig=").append(httpDumpConfig);
        sb.append(", statusCode=").append(statusCode);
        sb.append('}');
        return sb.toString();
    }
}
