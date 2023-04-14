package com.boosting.code.Services;

import com.boosting.code.Dto.ProxyResponseDto;
import com.boosting.code.Models.RequestResources;
import jakarta.servlet.http.HttpServletRequest;

public interface IProxyService {
    ProxyResponseDto processGetRequest(HttpServletRequest request, String trackingID,String baseURL) ;
    ProxyResponseDto processPostRequest(HttpServletRequest request,String body, String trackingID,String baseURL);
    ProxyResponseDto processPutRequest(HttpServletRequest request, String body, String trackingID,String baseURL) ;
    ProxyResponseDto processDeleteRequest(HttpServletRequest request, String trackingID, String baseURL);
    ProxyResponseDto processRequestGivenResources(RequestResources resources, String baseURL, String httpMethod);
}
