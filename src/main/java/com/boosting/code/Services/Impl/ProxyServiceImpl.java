package com.boosting.code.Services.Impl;

import com.boosting.code.Config.WebConfig;
import com.boosting.code.Dto.ProxyResponseDto;
import com.boosting.code.Exceptions.ProxyServiceException;
import com.boosting.code.Models.FileInfo;
import com.boosting.code.Models.RequestResources;
import com.boosting.code.Services.IProxyService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


@Service
@AllArgsConstructor
public class ProxyServiceImpl implements IProxyService {
    private final WebConfig config;
    private final String GATEWAY_PREFIX="/api/v1/data";
    private RequestResources extractResources(HttpServletRequest request, String body,String baseURL) {


        String rawURI = request.getRequestURI();
        String uri = StringUtils.hasText(rawURI)?rawURI:"";
        if(uri.startsWith(GATEWAY_PREFIX))
            uri=uri.replace(GATEWAY_PREFIX,"");

        Mono<String> bodyMono = StringUtils.hasText(body)?Mono.just(body):Mono.never();

        String queryString = request.getQueryString();

        MultiValueMap<String, String> queryParams = extractQueryParams(queryString);

        RequestResources resources = new RequestResources(uri,bodyMono,queryParams,baseURL);
        resources.setBinary(!queryParams.isEmpty());
        return resources;
    }

    private MultiValueMap<String, String> extractQueryParams(String queryString) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (queryString != null) {
            String[] queryParamsArray = queryString.split("&");
            for (String queryParam : queryParamsArray) {
                String[] keyValue = queryParam.split("=");
                if (keyValue.length == 2) {
                    String key = null;
                    String value = null;
                    try {
                        key = URLDecoder.decode(keyValue[0], "UTF-8");
                        value = URLDecoder.decode(keyValue[1], "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    List<String> array = new ArrayList<>();
                    array.add(value);
                    queryParams.put(key, array);
                }
            }
        }

        LinkedMultiValueMap<String,String> response = new LinkedMultiValueMap<>(queryParams);

        return response;
    }

    private ProxyResponseDto resolveResponse(RequestResources extractedResources, String method) {
        WebClient client = clientSelector(extractedResources.isBinary(),extractedResources.getBase());
        WebClient.RequestHeadersSpec<?> base = createBase(extractedResources, client, method);
        ProxyResponseDto response = decodeResponse(base, extractedResources);
        return response;
    }

    private WebClient.RequestHeadersSpec<?> createBase(RequestResources resources, WebClient client,
                                                       String method) {
        WebClient.RequestHeadersSpec<?> base;
        switch (method) {
            case "GET":
                base = client.get()
                        .uri(resources.getUri(),
                                uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build());
                break;
            case "POST":
                base = client.post()
                        .uri(resources.getUri(), uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build())
                        .body(resources.getBody(), String.class);
                break;
            case "PUT":
                base = client.put()
                        .uri(resources.getUri(), uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build())
                        .body(resources.getBody(), String.class);
                break;
            case "DELETE":
                base = client.delete()
                        .uri(resources.getUri(),
                                uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build());
                break;
            default:
                base = null;
        }
        return base;
    }

    private ProxyResponseDto decodeResponse(WebClient.RequestHeadersSpec<?> base, RequestResources resources) {
        if (resources.isBinary()) {
            byte[] data;
            AtomicReference<HttpHeaders> headers = new AtomicReference<>();
            Mono<byte[]> mono = base.exchangeToMono(responseData -> {
                headers.set(responseData.headers().asHttpHeaders());
                return responseData.bodyToMono(byte[].class);
            });
            data = mono.block();
            String mime = extractMimeType(headers);
            String uuid= extractUuid(headers);
            return new ProxyResponseDto(null, new FileInfo(uuid, mime,data), true);
        }
        WebClient.ResponseSpec responseSpec = base.retrieve();
        JsonNode body = responseSpec.bodyToMono(JsonNode.class).block();
        return new ProxyResponseDto(body, null, false);
    }

    private String extractUuid(AtomicReference<HttpHeaders> headers) {
        String value = headers.get().getFirst("UUID");
        if (null == value)
            value="UUID header not present";
        return value;
    }

    private String extractMimeType(AtomicReference<HttpHeaders> headers) {
        String value = headers.get().getFirst("Content-Disposition");
        if (null == value)
            throw new ProxyServiceException("Content-Disposition header not present");
        return value;
    }

    private WebClient clientSelector(boolean isBinaryClient,String base) {
        return isBinaryClient ? config.getBinaryClient(base) : config.getJSONClient(base);
    }

    public ProxyResponseDto processGetRequest(HttpServletRequest request, String trackingID, String baseURL) {
        RequestResources extractedResources = extractResources(request, null,baseURL);

        ProxyResponseDto responseDto = resolveResponse(extractedResources, "GET");
        return responseDto;
    }

    public ProxyResponseDto processPostRequest(HttpServletRequest request, String body, String trackingID, String baseURL) {
        RequestResources extractedResources = extractResources(request, body,baseURL);
        ProxyResponseDto responseDto = resolveResponse(extractedResources, "POST");
        return responseDto;
    }

    public ProxyResponseDto processPutRequest(HttpServletRequest request, String body, String trackingID, String baseURL) {
        RequestResources extractedResources = extractResources(request, body,baseURL);
        ProxyResponseDto responseDto = resolveResponse(extractedResources, "PUT");
        return responseDto;
    }

    public ProxyResponseDto processDeleteRequest(HttpServletRequest request, String trackingID, String baseURL) {
        RequestResources extractedResources = extractResources(request, null,baseURL);
        ProxyResponseDto responseDto = resolveResponse(extractedResources, "DELETE");
        return responseDto;
    }

    public ProxyResponseDto processRequestGivenResources(RequestResources resources,String baseURL,String httpMethod){
        ProxyResponseDto responseDto = resolveResponse(resources, httpMethod);
        return responseDto;
    }

}
