package com.boosting.code.Config;

import com.boosting.code.Constants.Constants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebConfig {
    public WebClient getBinaryClient(String base) {
        WebClient client = WebClient.builder()
                .baseUrl(StringUtils.hasText(base)?base: Constants.OBJECT_STORE_HOST)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(5000 * 1024))
                .build();

        return client;
    }

    public WebClient getJSONClient(String base) {
        WebClient client = WebClient.builder()
                .baseUrl(StringUtils.hasText(base)?base:Constants.OBJECT_STORE_HOST)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(5000 * 1024))
                .build();

        return client;
    }
}
