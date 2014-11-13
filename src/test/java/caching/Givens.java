package caching;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class Givens {
    public static void givenResponseWithBody(String path, String body) {
        givenThat(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(body)));
    }

    public static void givenResponseWithStatus(String path, int status) {
        givenThat(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(0)));
    }

    public static void givenResponseWithBodyAndCacheControl(String path, String body, String controlHeaderValue) {
        givenThat(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Cache-Control", controlHeaderValue)
                        .withBody(body)));
    }

}
