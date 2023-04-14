package com.boosting.code.Services;

import com.boosting.code.Dto.ProtoRequest;
import com.boosting.code.Dto.ProxyResponseDto;
import com.boosting.code.Models.RequestResources;
import jakarta.servlet.http.HttpServletRequest;

public interface IProxyService {
    ProxyResponseDto processGetRequest(HttpServletRequest request, String trackingID,String baseURL,boolean needsBinaryClient) ;
    ProxyResponseDto processPostRequest(HttpServletRequest request,String body, String trackingID,String baseURL,boolean needsBinaryClient);
    ProxyResponseDto processPutRequest(HttpServletRequest request, String body, String trackingID,String baseURL,boolean needsBinaryClient) ;
    ProxyResponseDto processDeleteRequest(HttpServletRequest request, String trackingID, String baseURL,boolean needsBinaryClient);
    ProxyResponseDto processRequestGivenResources(ProtoRequest request, String httpMethod);
}
