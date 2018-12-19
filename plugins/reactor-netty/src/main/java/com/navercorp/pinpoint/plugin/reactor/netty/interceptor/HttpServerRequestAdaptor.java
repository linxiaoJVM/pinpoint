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

package com.navercorp.pinpoint.plugin.reactor.netty.interceptor;

import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import reactor.ipc.netty.http.server.HttpServerRequest;
import java.net.InetSocketAddress;

/**
 * Created by linxiao on 2018/12/18.
 */
public class HttpServerRequestAdaptor implements RequestAdaptor<HttpServerRequest> {

    public HttpServerRequestAdaptor() {
    }

    @Override
    public String getHeader(HttpServerRequest request, String name) {
        return request.requestHeaders().get(name);
    }


    @Override
    public String getRpcName(HttpServerRequest request) {
        return request.path();
    }

    @Override
    public String getEndPoint(HttpServerRequest request) {
        if (request.remoteAddress() != null) {
            final int port = request.remoteAddress().getPort();
            if (port <= 0) {
                return request.remoteAddress().getHostString();
            } else {
                return request.remoteAddress().getHostString() + ":" + port;
            }
        }
        return null;
    }

    @Override
    public String getRemoteAddress(HttpServerRequest request) {
        final InetSocketAddress socketAddress = request.remoteAddress();
        if (socketAddress != null) {
            return socketAddress.toString();
        }
        return "unknown";
    }

    @Override
    public String getAcceptorHost(HttpServerRequest request) {
        return NetworkUtils.getHostFromURL(request.uri().toString());
    }
}
