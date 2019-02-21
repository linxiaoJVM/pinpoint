/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.reactor.netty.server;

import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import reactor.ipc.netty.http.server.HttpServerRequest;

/**
 * Created by linxiao on 2019/1/29.
 */
public class HttpServerRequestAdaptor
        implements RequestAdaptor<HttpServerRequest>
{
    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    private final boolean isDebug = this.logger.isDebugEnabled();

    public String getHeader(HttpServerRequest request, String name)
    {
        return request.requestHeaders().get(name);
    }

    public String getRpcName(HttpServerRequest request)
    {
        return path(request.uri());
    }

    public String getEndPoint(HttpServerRequest request)
    {
        if (request.remoteAddress() != null)
        {
            int port = request.remoteAddress().getPort();
            if (port <= 0) {
                return request.remoteAddress().getHostString();
            }
            return request.remoteAddress().getHostString() + ":" + port;
        }
        return null;
    }

    public String getRemoteAddress(HttpServerRequest request)
    {
        InetSocketAddress socketAddress = request.remoteAddress();
        if (socketAddress != null) {
            return remoteAddress(socketAddress.toString());
        }
        return "unknown";
    }

    private String remoteAddress(String address)
    {
        String uri = URI.create(address).getPath();
        if ((!uri.isEmpty()) &&
                (uri.charAt(0) == '/'))
        {
            uri = uri.substring(1);
            if (uri.length() <= 1) {
                return uri;
            }
        }
        return uri;
    }

    private String path(String path)
    {
        if (!path.isEmpty())
        {
            int n = path.indexOf("?");
            if (n > 0) {
                return path.substring(0, n);
            }
        }
        return path;
    }

    public String getAcceptorHost(HttpServerRequest request)
    {
        String acceptorHost = NetworkUtils.getHostFromURL(request.uri());
        if (this.isDebug) {
            this.logger.debug("The acceptorHost :{}", acceptorHost);
        }
        return acceptorHost;
    }
}
