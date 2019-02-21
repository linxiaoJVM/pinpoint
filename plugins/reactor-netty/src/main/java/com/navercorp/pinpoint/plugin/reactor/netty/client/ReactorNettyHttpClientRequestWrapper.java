package com.navercorp.pinpoint.plugin.reactor.netty.client;

import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestWrapper;
import com.navercorp.pinpoint.common.util.Assert;
import io.netty.handler.codec.http.HttpRequest;
import reactor.ipc.netty.http.client.HttpClientRequest;

/**
 * Created by linxiao on 2019/1/10.
 */
public class ReactorNettyHttpClientRequestWrapper implements ClientRequestWrapper {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final HttpClientRequest httpRequest;
    private final String host;


    public ReactorNettyHttpClientRequestWrapper(final HttpClientRequest httpRequest, final String host) {
        this.httpRequest = Assert.requireNonNull(httpRequest, "httpRequest must not be null");
        this.host = host;
    }


    @Override
    public String getDestinationId() {
        if (this.host != null) {
            return this.host;
        }
        return "Unknown";
    }

    @Override
    public String getUrl() {
        return this.httpRequest.uri();
    }

}

