package com.boosting.code.Utilities;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Enumeration;

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
}
