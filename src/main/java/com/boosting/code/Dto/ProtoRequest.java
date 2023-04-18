package com.boosting.code.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProtoRequest {
    private String queryString;
    private String uri;
    private String body;
    private String baseURL;
    private boolean isBinaryClient;
    private HttpHeaders headers;
}
