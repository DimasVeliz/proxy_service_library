package com.boosting.code.Services.Impl;

import com.boosting.code.Config.WebConfig;
import com.boosting.code.Dto.ProtoRequest;
import com.boosting.code.Dto.ProxyResponseDto;
import com.boosting.code.Exceptions.ProxyServiceException;
import com.boosting.code.Models.FileInfo;
import com.boosting.code.Models.RequestResources;
import com.boosting.code.Services.IProxyService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import static com.boosting.code.Utilities.Utils.extractHeaders;


@Service
@AllArgsConstructor
public class ProxyServiceImpl implements IProxyService {
    private final WebConfig config;
    private final String GATEWAY_PREFIX="/api/v1/data";
    private RequestResources extractResources(ProtoRequest request) {
        String rawURI = request.getUri();
        String uri = StringUtils.hasText(rawURI)?rawURI:"";
        if(uri.startsWith(GATEWAY_PREFIX))
            uri=uri.replace(GATEWAY_PREFIX,"");

        Mono<String> bodyMono = StringUtils.hasText(request.getBody())?
                Mono.just(request.getBody()):Mono.never();

        String queryString = request.getQueryString();

        MultiValueMap<String, String> queryParams = extractQueryParams(queryString);
        RequestResources resources =  RequestResources
                .builder()
                .uri(uri)
                .body(bodyMono)
                .paramInfo(queryParams)
                .base(request.getBaseURL())
                .headers(request.getHeaders())
                .isBinary(request.isBinaryClient())
                .build();
        resources.setBinary(request.isBinaryClient());

        return resources;
    }

    private RequestResources extractResources(HttpServletRequest request,
                                              String body,
                                              String baseURL,
                                              boolean isBinaryClient) {

        var headers = extractHeaders(request);

        ProtoRequest protoRequest = ProtoRequest
                .builder()
                .queryString(request.getQueryString())
                .uri(request.getRequestURI())
                .body(body)
                .baseURL(baseURL)
                .isBinaryClient(isBinaryClient)
                .headers(headers)
                .build();

        return extractResources(protoRequest);
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
                                uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build())
                        .headers(httpHeaders -> httpHeaders.addAll(resources.getHeaders()));
                break;
            case "POST":
                base = client.post()
                        .uri(resources.getUri(), uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build())
                        .body(resources.getBody(), String.class)
                        .headers(httpHeaders -> httpHeaders.addAll(resources.getHeaders()));
                break;
            case "PUT":
                base = client.put()
                        .uri(resources.getUri(), uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build())
                        .body(resources.getBody(), String.class)
                        .headers(httpHeaders -> httpHeaders.addAll(resources.getHeaders()));
                break;
            case "DELETE":
                base = client.delete()
                        .uri(resources.getUri(),
                                uriBuilder -> uriBuilder.queryParams(resources.getParamInfo()).build())
                        .headers(httpHeaders -> httpHeaders.addAll(resources.getHeaders()));
                break;
            default:
                base = null;
        }
        return base;
    }

    private ProxyResponseDto decodeResponse(WebClient.RequestHeadersSpec<?> base, RequestResources resources) {
        AtomicReference<HttpHeaders> headers = new AtomicReference<>();
        if (resources.isBinary()) {
            byte[] data;
            Mono<byte[]> mono = base.exchangeToMono(responseData -> {
                headers.set(responseData.headers().asHttpHeaders());
                return responseData.bodyToMono(byte[].class);
            });
            data = mono.block();
            return new ProxyResponseDto(null, new FileInfo(null, null,data), true,headers.get());
        }
        WebClient.ResponseSpec responseSpec = base.retrieve();
        JsonNode body = responseSpec.bodyToMono(JsonNode.class).block();
        return new ProxyResponseDto(body, null, false,headers.get());
    }

    private String extractUuid(AtomicReference<HttpHeaders> headers) {
        String value = headers.get().getFirst("UUID");
        if (null == value)
            value="UUID header not present";
        return value;
    }

    private String extractMimeType(AtomicReference<HttpHeaders> headers) {
        String value=null;
        if(headers.get().containsKey("Content-Type"))
            value= headers.get().getFirst("Content-Type");
        if (null == value)
            value= MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return value;
    }
    private String extractContentDisposition(AtomicReference<HttpHeaders> headers){
        String value = headers.get().getFirst("Content-Disposition");
        if (null == value)
            value= "filename=\"file\"";
        return value;
    }

    private WebClient clientSelector(boolean isBinaryClient,String base) {
        return isBinaryClient ? config.getBinaryClient(base) : config.getJSONClient(base);
    }

    public ProxyResponseDto processGetRequest(HttpServletRequest request, String trackingID, String baseURL,boolean isBinaryClient) {
        RequestResources extractedResources = extractResources(request, null,baseURL, isBinaryClient);

        ProxyResponseDto responseDto = resolveResponse(extractedResources, "GET");
        return responseDto;
    }

    public ProxyResponseDto processPostRequest(HttpServletRequest request, String body, String trackingID, String baseURL,boolean isBinaryClient) {
        RequestResources extractedResources = extractResources(request, body,baseURL, isBinaryClient);
        ProxyResponseDto responseDto = resolveResponse(extractedResources, "POST");
        return responseDto;
    }

    public ProxyResponseDto processPutRequest(HttpServletRequest request, String body, String trackingID, String baseURL,boolean isBinaryClient) {
        RequestResources extractedResources = extractResources(request, body,baseURL, isBinaryClient);
        ProxyResponseDto responseDto = resolveResponse(extractedResources, "PUT");
        return responseDto;
    }

    public ProxyResponseDto processDeleteRequest(HttpServletRequest request, String trackingID, String baseURL,boolean isBinaryClient) {
        RequestResources extractedResources = extractResources(request, null,baseURL, isBinaryClient);
        ProxyResponseDto responseDto = resolveResponse(extractedResources, "DELETE");
        return responseDto;
    }

    public ProxyResponseDto processRequestGivenResources(ProtoRequest request, String httpMethod){
        RequestResources extractedResources = extractResources(request);

        ProxyResponseDto responseDto = resolveResponse(extractedResources, httpMethod);
        return responseDto;
    }

}
