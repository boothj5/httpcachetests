package caching;

import org.apache.http.client.cache.CacheResponseStatus;

import static org.junit.Assert.assertEquals;

public class Asserts {
    public static void assertCacheStatusAndBody(CacheResponseStatus expectedCacheStatus, String expectedResposeBody, CachingClient actualClient) {
        assertEquals(expectedCacheStatus, actualClient.getResponseStatus());
        assertEquals(expectedResposeBody, actualClient.getBody());
    }

    public static void assertCacheStatus(CacheResponseStatus expectedCacheStatus, CachingClient actualClient) {
        assertEquals(expectedCacheStatus, actualClient.getResponseStatus());
    }
}
