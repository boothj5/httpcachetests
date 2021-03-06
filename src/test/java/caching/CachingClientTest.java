package caching;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static caching.Asserts.assertCacheStatus;
import static caching.Asserts.assertCacheStatusAndBody;
import static caching.Givens.givenResponseWithBody;
import static caching.Givens.givenResponseWithBodyAndCacheControl;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.http.client.cache.CacheResponseStatus.*;

public class CachingClientTest {

    private static final String RESPONSE_BODY1 = "{ something: 1 }";
    private static final String RESPONSE_BODY2 = "{ something: 2 }";
    private static final String RESPONSE_BODY3 = "{ somethingelse: 3 }";
    private static final String SOME_THING_PATH = CachingClient.SOME_THING_PATH;
    private static final String SOME_THING_ELSE_PATH = CachingClient.SOME_THING_ELSE_PATH;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private CachingClient client;

    @Before
    public void setUp() {
        client = new CachingClient();
    }

    @Test
    public void hasCacheMissOnOneRequest() throws IOException {
        givenResponseWithBody(SOME_THING_PATH, RESPONSE_BODY1);
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void hasCacheMissFollowedByCacheValidationWhenNoCacheControlHeaderInResponse() throws IOException {
        givenResponseWithBody(SOME_THING_PATH, RESPONSE_BODY1);
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBody(SOME_THING_PATH, RESPONSE_BODY2);
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(VALIDATED, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void secondRequestIsCacheHitWhenMaxAge3SecondsInResponseAndTwoImmediateRequestsMade() throws IOException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 3");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "max-age = 3");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_HIT, RESPONSE_BODY1, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void secondRequestIsCacheValidationWhenMaxAge2SecondsInResponseAndTwoRequestsMade3SecondsApart() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 2");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "max-age = 2");
        Thread.sleep(3000);
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(VALIDATED, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void secondRequestIsCacheMissWhenMaxAge2SecondsInResponseAndTwoRequestsMadeWithNoCacheSet() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 2");
        Map<String, String> requestHeaders = headersWithCacheControl("no-cache");
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "max-age = 2");
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void secondRequestIsCacheMissWhenMaxAge2SecondsInResponseAndTwoRequestsMadeWithNoStoreSet() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 2");
        Map<String, String> requestHeaders = headersWithCacheControl("no-store");
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "max-age = 2");
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void thirdRequestIsCacheMissWhenMaxAge5SecondsAndMadeWithNoCache() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 5");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "max-age = 5");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_HIT, RESPONSE_BODY1, client);

        Map<String, String> requestHeaders = headersWithCacheControl("no-cache");
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void callingDifferentPathDoesNotCache() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 3");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_ELSE_PATH, RESPONSE_BODY3, "max-age = 3");
        client.get(SOME_THING_ELSE_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY3, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
        verify(1, getRequestedFor(urlEqualTo(SOME_THING_ELSE_PATH)));
    }

    @Test
    public void cahceMissWhenReponseReturnMustRevalidateWithNoCacheNoStore() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "must-revalidate,no-cache,no-store");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "must-revalidate,no-cache,no-store");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void alwaysRevalidatedWhenReponseReturnMustRevalidateOnly() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "must-revalidate");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY2, "must-revalidate");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(VALIDATED, RESPONSE_BODY2, client);

        verify(2, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    // Failures

    @Test
    public void noResponseReturnsCachedResponseWhenNoHeaders() throws IOException, InterruptedException {
        givenResponseWithBody(SOME_THING_PATH, RESPONSE_BODY1);
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        stubFor(get(urlEqualTo(SOME_THING_PATH))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_HIT, RESPONSE_BODY1, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void noResponseReturnsCacheHitWhenMaxAgeNotPassed() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 5");
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        stubFor(get(urlEqualTo(SOME_THING_PATH))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_HIT, RESPONSE_BODY1, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void noResponseReturnsCacheMissWhenMaxAgePassed() throws IOException, InterruptedException {
        givenResponseWithBodyAndCacheControl(SOME_THING_PATH, RESPONSE_BODY1, "max-age = 2");

        client.get(SOME_THING_PATH);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        stubFor(get(urlEqualTo(SOME_THING_PATH))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        Thread.sleep(3000);
        client.get(SOME_THING_PATH);

        assertCacheStatus(CACHE_HIT, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }

    @Test
    public void noResponseReturnsErrorWhenNoHeaders() throws IOException, InterruptedException {
        givenResponseWithBody(SOME_THING_PATH, RESPONSE_BODY1);
        Map<String, String> requestHeaders = headersWithCacheControl("no-cache");
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatusAndBody(CACHE_MISS, RESPONSE_BODY1, client);

        stubFor(get(urlEqualTo(SOME_THING_PATH))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        client.getWithHeaders(SOME_THING_PATH, requestHeaders);
        assertCacheStatus(CACHE_MISS, client);

        verify(1, getRequestedFor(urlEqualTo(SOME_THING_PATH)));
    }



    private static Map<String, String> headersWithCacheControl(String cacheControlValue) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", cacheControlValue);
        return headers;
    }
}