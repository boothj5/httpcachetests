package caching;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class Givens {
    public static void givenResponse(String body) {
        givenThat(get(urlEqualTo(CachingClient.PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(body)));
    }

    public static void givenResponseWithCacheControl(String body, String controlHeaderValue) {
        givenThat(get(urlEqualTo(CachingClient.PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Cache-Control", controlHeaderValue)
                        .withBody(body)));
    }

}
