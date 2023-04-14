package com.boosting.code.Services;

import com.boosting.code.Dto.ProxyResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface IProxyService {
    ProxyResponseDto processGetRequest(HttpServletRequest request, String trackingID,String baseURL) ;
    ProxyResponseDto processPostRequest(HttpServletRequest request,String body, String trackingID,String baseURL);
    ProxyResponseDto processPutRequest(HttpServletRequest request, String body, String trackingID,String baseURL) ;
    ProxyResponseDto processDeleteRequest(HttpServletRequest request, String trackingID, String baseURL);

}
