package caching;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.cache.CacheResponseStatus;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static caching.Givens.*;
import static caching.Asserts.*;
import static org.apache.http.client.cache.CacheResponseStatus.*;

public class CachingClientTest {

    private static final String BODY_1 = "{ something: 1 }";
    private static final String BODY_2 = "{ something: 2 }";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private CachingClient client;

    @Before
    public void setUp() {
        client = new CachingClient();
    }

    @Test
    public void hasCacheMissOnOneRequest() throws IOException {
        givenResponse(BODY_1);

        client.get();

        assertCacheStatusAndBody(CACHE_MISS, BODY_1, client);

        verify(1, getRequestedFor(urlEqualTo(CachingClient.PATH)));
    }

    @Test
    public void hasCacheMissFollowedByCacheValidationWhenNoCacheControlHeaderInResponse() throws IOException {
        givenResponse(BODY_1);

        client.get();

        assertCacheStatusAndBody(CACHE_MISS, BODY_1, client);

        givenResponse(BODY_2);

        client.get();

        assertCacheStatusAndBody(VALIDATED, BODY_2, client);

        verify(2, getRequestedFor(urlEqualTo(CachingClient.PATH)));
    }

    @Test
    public void secondRequestIsCacheHitWhenMaxAge3SecondsInResponseAndTwoImmediateRequestsMade() throws IOException {
        givenResponseWithCacheControl(BODY_1, "max-age = 3");

        client.get();

        assertCacheStatusAndBody(CACHE_MISS, BODY_1, client);

        givenResponseWithCacheControl(BODY_2, "max-age = 3");

        client.get();

        assertCacheStatusAndBody(CACHE_HIT, BODY_1, client);

        verify(1, getRequestedFor(urlEqualTo(CachingClient.PATH)));
    }

    @Test
    public void secondRequestIsCacheValidationWhenMaxAge2SecondsInResponseAndTwoRequestsMade3SecondsApart() throws IOException, InterruptedException {
        givenResponseWithCacheControl(BODY_1, "max-age = 2");

        client.get();

        assertCacheStatusAndBody(CACHE_MISS, BODY_1, client);

        givenResponseWithCacheControl(BODY_2, "max-age = 2");

        Thread.sleep(3000);
        client.get();

        assertCacheStatusAndBody(VALIDATED, BODY_2, client);

        verify(2, getRequestedFor(urlEqualTo(CachingClient.PATH)));
    }

    @Test
    public void secondRequestIsCacheMissWhenMaxAge2SecondsInResponseAndTwoRequestsMadeWithNoCacheSet() throws IOException, InterruptedException {
        givenResponseWithCacheControl(BODY_1, "max-age = 2");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Cache-Control", "no-cache");
        client.get(requestHeaders);

        assertCacheStatusAndBody(CACHE_MISS, BODY_1, client);

        givenResponseWithCacheControl(BODY_2, "max-age = 2");

        client.get(requestHeaders);

        assertCacheStatusAndBody(CACHE_MISS, BODY_2, client);

        verify(2, getRequestedFor(urlEqualTo(CachingClient.PATH)));
    }

    @Test
    public void secondRequestIsCacheMissWhenMaxAge2SecondsInResponseAndTwoRequestsMadeWithNoStoreSet() throws IOException, InterruptedException {
        givenResponseWithCacheControl(BODY_1, "max-age = 2");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Cache-Control", "no-store");
        client.get(requestHeaders);

        assertCacheStatusAndBody(CACHE_MISS, BODY_1, client);

        givenResponseWithCacheControl(BODY_2, "max-age = 2");

        client.get(requestHeaders);

        assertCacheStatusAndBody(CACHE_MISS, BODY_2, client);

        verify(2, getRequestedFor(urlEqualTo(CachingClient.PATH)));
    }
}