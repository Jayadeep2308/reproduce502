package com.error.lb.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class sampleDTO {
    private List<String> itemList;
    private BigDecimal total;
}
