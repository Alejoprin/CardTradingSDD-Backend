package com.cardtrading.ad.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAdRequest {

    private String type;

    private String description;

    private BigDecimal price;
}
