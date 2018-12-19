package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

import java.util.List;

/**
 * Created by linxiao on 2018/12/17.
 */
public class ReactorNettyConfig {
    private final boolean enable;
    private final boolean enableHttpServer;
//    private final boolean enableHttpClient;
    private final List<String> bootstrapMains;

    public ReactorNettyConfig(ProfilerConfig config) {
        if (config == null) {
            throw new NullPointerException("config must not be null");
        }

        // plugin
        this.enable = config.readBoolean("profiler.reactor.netty.enable", true);
        this.enableHttpServer = config.readBoolean("profiler.reactor.netty.http.server.enable", true);
//        this.enableHttpClient = config.readBoolean("profiler.reactor.netty.http.client.enable", true);
        this.bootstrapMains = config.readList("profiler.reactor.netty.bootstrap.main");
    }

    public boolean isEnable() {
        return enable;
    }

    public boolean isEnableHttpServer() {
        return enableHttpServer;
    }

//    public boolean isEnableHttpClient() {
//        return enableHttpClient;
//    }

    public List<String> getBootstrapMains() {
        return bootstrapMains;
    }

    @Override
    public String toString() {
        return "ReactorNettyConfig{" +
                "enable=" + enable +
                ", enableHttpServer=" + enableHttpServer +
                ", bootstrapMains=" + bootstrapMains +
                '}';
    }
}
