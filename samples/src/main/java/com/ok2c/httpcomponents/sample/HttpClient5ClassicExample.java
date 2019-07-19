/*
 * Copyright 2018 OK2 Consulting Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ok2c.httpcomponents.sample;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieSpecs;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClient5ClassicExample {

    public static void main(String... args) throws Exception {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(SSLContexts.createSystemDefault())
                        .setTlsVersions(TLS.V_1_3, TLS.V_1_2)
                        .build())
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofSeconds(5))
                        .build())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setConnectionTimeToLive(TimeValue.ofMinutes(1L))
                .build();

        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(5))
                        .setResponseTimeout(Timeout.ofSeconds(5))
                        .setCookieSpec(CookieSpecs.STANDARD_STRICT.ident)
                        .build())
                .build();

        CookieStore cookieStore = new BasicCookieStore();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        HttpClientContext clientContext = HttpClientContext.create();
        clientContext.setCookieStore(cookieStore);
        clientContext.setCredentialsProvider(credentialsProvider);
        clientContext.setRequestConfig(RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build());

        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);

        HttpPost httpPost = new HttpPost("https://httpbin.org/post");

        List<NameValuePair> requestData = Arrays.asList(
                new BasicNameValuePair("name1", "value1"),
                new BasicNameValuePair("name2", "value2"));
        httpPost.setEntity(HttpEntities.create(outstream -> {
            objectMapper.writeValue(outstream, requestData);
            outstream.flush();
        }, ContentType.APPLICATION_JSON));

        JsonNode responseData = client.execute(httpPost, response -> {
            if (response.getCode() >= 300) {
                throw new ClientProtocolException(new StatusLine(response).toString());
            }
            final HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                return null;
            }
            try (InputStream inputStream = responseEntity.getContent()) {
                return objectMapper.readTree(inputStream);
            }
        });
        System.out.println(responseData);

        client.close();
    }

}
