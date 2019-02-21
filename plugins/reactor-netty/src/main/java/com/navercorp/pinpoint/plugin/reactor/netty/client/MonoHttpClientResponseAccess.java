package com.navercorp.pinpoint.plugin.reactor.netty.client;

import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientResponse;

/**
 * Created by linxiao on 2019/1/16.
 */
public interface MonoHttpClientResponseAccess {
    void _$PINPOINT$_setHttpClientResponse(Mono<HttpClientResponse> httpClientResponse);
    Mono<HttpClientResponse> _$PINPOINT$_getHttpClientResponse();
}
