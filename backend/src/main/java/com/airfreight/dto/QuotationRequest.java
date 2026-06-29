package com.airfreight.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 询价请求DTO - 对应工作流第1步
 */
@Data
public class QuotationRequest {
    private String goodsName;
    private String goodsCategory;
    private Integer pieceCount;
    private BigDecimal grossWeight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private String originAirport;
    private String destAirport;
    private String incoterm;
    private String shipperName;
    private String consigneeName;
    private String currency;
}