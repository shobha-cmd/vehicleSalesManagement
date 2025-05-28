package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.Data;
import java.util.List;
@Data

public class MultiOrderResponse {
    private int status;
    private String message;
    private List<OrderResponse> orderResponses;
    public MultiOrderResponse(int status, String message, List<OrderResponse> orderResponses) {
        this.status = status;
        this.message = message;
        this.orderResponses = orderResponses;
    }
}