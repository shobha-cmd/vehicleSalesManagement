// OrderStatsResponse.java (place in your `dto/apiresponse` package)
package com.vehicle.salesmanagement.domain.dto.apirequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsResponse {
    private long totalOrders;
    private long pendingOrders;
    private long financePendingOrders;
    private long closedOrders;
}
