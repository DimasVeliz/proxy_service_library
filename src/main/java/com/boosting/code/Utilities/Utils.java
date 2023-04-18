package com.boosting.code.Utilities;

import com.boosting.code.Constants.Constants;
import com.boosting.code.Dto.ProtoRequest;
import com.boosting.code.Models.RequestResources;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public class Utils {

    public static HttpHeaders extractHeaders(HttpServletRequest request){
        HttpHeaders headers = new HttpHeaders();

        // Get the header names from the request object
        Enumeration<String> headerNames = request.getHeaderNames();

        // Loop through the header names and add them to the HttpHeaders object
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                headers.add(headerName, headerValue);
            }
        }

        return headers;
    }
    public static RequestResources extractResources(HttpServletRequest request,
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
    public static RequestResources extractResources(ProtoRequest request) {
        String rawURI = request.getUri();
        String uri = StringUtils.hasText(rawURI)?rawURI:"";
        if(uri.startsWith(Constants.GATEWAY_PREFIX))
            uri=uri.replace(Constants.GATEWAY_PREFIX,"");

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




    private static MultiValueMap<String, String> extractQueryParams(String queryString) {
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
}
