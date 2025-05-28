package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse<T> {
    private int statusCode;
    private String message;
    private T data;


}
