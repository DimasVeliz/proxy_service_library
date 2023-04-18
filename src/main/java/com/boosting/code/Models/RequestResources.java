package com.boosting.code.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestResources {
    private boolean isBinary;
    private String uri;
    private Mono<String> body;
    MultiValueMap<String, String> paramInfo;
    private String base;
    private HttpHeaders headers;
}
