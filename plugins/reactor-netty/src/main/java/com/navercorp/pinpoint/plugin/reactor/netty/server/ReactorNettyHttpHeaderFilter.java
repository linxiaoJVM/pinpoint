/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.reactor.netty.server;

import com.navercorp.pinpoint.bootstrap.context.Header;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.ipc.netty.http.server.HttpServerRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author  linxiao
 */
public class ReactorNettyHttpHeaderFilter {
    private final boolean enable;

    public ReactorNettyHttpHeaderFilter(boolean enable) {
        this.enable = enable;
    }

    public void filter(final HttpServerRequest request) {
        if (!enable || request == null || request.requestHeaders() == null) {
            return;
        }

        request.requestHeaders().names().forEach(name -> {
            if (Header.startWithPinpointHeader(name)) {
                request.requestHeaders().remove(name);
            }
        });
    }
}
