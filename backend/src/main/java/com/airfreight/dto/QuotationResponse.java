package com.airfreight.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 询价响应DTO
 */
@Data
public class QuotationResponse {
    private String quotationNo;
    private BigDecimal volumeWeight;
    private BigDecimal chargeableWeight;
    private BigDecimal freightCharge;
    private BigDecimal fuelSurcharge;
    private BigDecimal securitySurcharge;
    private BigDecimal groundHandlingFee;
    private BigDecimal customsBrokerFee;
    private BigDecimal insuranceFee;
    private BigDecimal totalCharge;
    private String currency;
    private String validUntil;
    private String remarks;
}