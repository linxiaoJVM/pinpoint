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
import com.navercorp.pinpoint.bootstrap.plugin.request.util.ParameterExtractor;
import com.navercorp.pinpoint.common.util.StringUtils;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SslHandler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.ipc.netty.http.server.HttpServerRequest;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author linxiao
 */
public class HttpServerParameterExtractor implements ParameterExtractor<HttpServerRequest> {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private int eachLimit;
    private int totalLimit;

    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

    public HttpServerParameterExtractor(int eachLimit, int totalLimit) {
        this.eachLimit = eachLimit;
        this.totalLimit = totalLimit;
    }

    private  URI uri;


    @Override
    public String extractParameter(HttpServerRequest request) {
        try {
            this.uri = initUri(request);
        }catch ( URISyntaxException e) {

        }

        if (getQueryParams() == null) {
            return "";
        }

        final StringBuilder params = new StringBuilder(64);
        MultiValueMap<String, String> entries = getQueryParams();
        if (isDebug) {
            logger.debug("request.getQueryParams() {}", entries);
        }
        for (Map.Entry<String, List<String>> entry : entries.entrySet()) {
            if (params.length() != 0) {
                params.append('&');
            }
            // skip appending parameters if parameter size is bigger than totalLimit
            if (params.length() > totalLimit) {
                params.append("...");
                return params.toString();
            }

            String key = entry.getKey();
            params.append(StringUtils.abbreviate(key, eachLimit));
            params.append('=');
            Object value = entry.getValue().get(0);
            if (value != null) {
                params.append(StringUtils.abbreviate(StringUtils.toString(value), eachLimit));
            }
        }
        return params.toString();
    }

    private static URI initUri(HttpServerRequest request) throws URISyntaxException {
        Assert.notNull(request, "HttpServerRequest must not be null");
        return new URI(resolveBaseUrl(request).toString() + resolveRequestUri(request));
    }

    private static URI resolveBaseUrl(HttpServerRequest request) throws URISyntaxException {
        String scheme = getScheme(request);
        String header = request.requestHeaders().get(HttpHeaderNames.HOST);
        if (header != null) {
            final int portIndex;
            if (header.startsWith("[")) {
                portIndex = header.indexOf(':', header.indexOf(']'));
            }
            else {
                portIndex = header.indexOf(':');
            }
            if (portIndex != -1) {
                try {
                    return new URI(scheme, null, header.substring(0, portIndex),
                            Integer.parseInt(header.substring(portIndex + 1)), null, null, null);
                }
                catch (NumberFormatException ex) {
                    throw new URISyntaxException(header, "Unable to parse port", portIndex);
                }
            }
            else {
                return new URI(scheme, header, null, null);
            }
        }
        else {
            InetSocketAddress localAddress = (InetSocketAddress) request.context().channel().localAddress();
            return new URI(scheme, null, localAddress.getHostString(),
                    localAddress.getPort(), null, null, null);
        }
    }

    private static String getScheme(HttpServerRequest request) {
        ChannelPipeline pipeline = request.context().channel().pipeline();
        boolean ssl = pipeline.get(SslHandler.class) != null;
        return ssl ? "https" : "http";
    }

    private static String resolveRequestUri(HttpServerRequest request) {
        String uri = request.uri();
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                break;
            }
            if (c == ':' && (i + 2 < uri.length())) {
                if (uri.charAt(i + 1) == '/' && uri.charAt(i + 2) == '/') {
                    for (int j = i + 3; j < uri.length(); j++) {
                        c = uri.charAt(j);
                        if (c == '/' || c == '?' || c == '#') {
                            return uri.substring(j);
                        }
                    }
                    return "";
                }
            }
        }
        return uri;
    }


    public MultiValueMap<String, String> getQueryParams() {

        return CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
    }

    /**
     * A method for parsing of the query into name-value pairs. The return
     * value is turned into an immutable map and cached.
     * <p>Note that this method is invoked lazily on first access to
     * {@link #getQueryParams()}. The invocation is not synchronized but the
     * parsing is thread-safe nevertheless.
     */
    protected MultiValueMap<String, String> initQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        String query = this.uri.getRawQuery();
        if (query != null) {
            Matcher matcher = QUERY_PATTERN.matcher(query);
            while (matcher.find()) {
                String name = decodeQueryParam(matcher.group(1));
                String eq = matcher.group(2);
                String value = matcher.group(3);
                value = (value != null ? decodeQueryParam(value) : (org.springframework.util.StringUtils.hasLength(eq) ? "" : null));
                queryParams.add(name, value);
            }
        }
        return queryParams;
    }

    @SuppressWarnings("deprecation")
    private String decodeQueryParam(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not decode query param [" + value + "] as 'UTF-8'. " +
                        "Falling back on default encoding; exception message: " + ex.getMessage());
            }
            return URLDecoder.decode(value);
        }
    }
}
