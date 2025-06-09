package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KendoGridResponse<T> {
    private List<T> data;
    private long total;
    private Object aggregateResults;
    private Object errors;

}