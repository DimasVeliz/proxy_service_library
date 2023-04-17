package com.boosting.code.Dto;

import com.boosting.code.Models.FileInfo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyResponseDto {
    private JsonNode jsonData;
    private FileInfo fileInfo;
    private boolean isBinary;
    private HttpHeaders headers;
}
