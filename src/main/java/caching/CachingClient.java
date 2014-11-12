package caching;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CachingClient {

    public static final String SOME_THING_PATH = "/some/thing";
    public static final String SOME_THING_ELSE_PATH = "/some/thing/else";

    private final CloseableHttpClient cachingClient;

    private CacheResponseStatus responseStatus;
    private String body;

    public CachingClient() {
        CacheConfig cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(1000)
                .setMaxObjectSize(8192)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(30000)
                .build();
        cachingClient = CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public void getWithHeaders(String path, Map<String, String> headers) throws IOException {
        HttpCacheContext context = HttpCacheContext.create();

        HttpGet httpget = new HttpGet("http://localhost:8089" + path);
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpget.setHeader(key, headers.get(key));
            }
        }

        System.out.println(httpget);
        printHeaders(httpget.getAllHeaders(), "Request");

        try (CloseableHttpResponse response = cachingClient.execute(httpget, context)) {
            responseStatus = context.getCacheResponseStatus();

            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            body = IOUtils.toString(stream);

            printHeaders(response.getAllHeaders(), "Response");
        }
    }

    public void get(String path) throws IOException {
        this.getWithHeaders(path, null);
    }

    public CacheResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public String getBody() {
        return body;
    }

    private static void printHeaders(Header[] headers, String type) {
        if (headers.length != 0) {
            System.out.println(type + " headers:");
            for (Header getHeader : headers) {
                System.out.println("  " + getHeader.getName() + ": " + getHeader.getValue());
            }
        } else {
            System.out.println("No " + type + " headers");
        }
    }
}
