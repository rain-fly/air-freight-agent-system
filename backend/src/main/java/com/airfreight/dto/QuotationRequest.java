package com.airfreight.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 询价请求DTO - 对应工作流第1步
 */
@Data
public class QuotationRequest {
    private String goodsName;
    private String goodsCategory;       // GENERAL/BATTERY/DANGEROUS/FOOD/COSMETIC
    private Integer pieceCount;
    private BigDecimal grossWeight;     // 毛重 kg
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private String originAirport;       // PVG/CAN
    private String destAirport;         // JFK/LHR
    private String incoterm;            // FOB/CIF/EXW/DDP
    private String shipperName;
    private String consigneeName;
    private String currency;            // CNY/USD
}

/**
 * 询价响应DTO
 */
@Data
public class QuotationResponse {
    private String quotationNo;
    private BigDecimal volumeWeight;        // 体积重
    private BigDecimal chargeableWeight;    // 计费重
    private BigDecimal freightCharge;       // 空运费
    private BigDecimal fuelSurcharge;       // 燃油附加费
    private BigDecimal securitySurcharge;   // 安全附加费
    private BigDecimal groundHandlingFee;   // 地面操作费
    private BigDecimal customsBrokerFee;    // 报关费
    private BigDecimal insuranceFee;        // 保险费
    private BigDecimal totalCharge;         // 总费用
    private String currency;
    private String validUntil;              // 报价有效期
    private String remarks;
}

/**
 * 订舱请求DTO - 对应工作流第4步
 */
@Data
public class BookingRequest {
    private Long shipmentId;
    private String bookingNote;
    private String airlineCode;
}

/**
 * 体积重计算DTO
 */
@Data
public class VolumeWeightResult {
    private BigDecimal grossWeight;
    private BigDecimal volumeWeight;
    private BigDecimal chargeableWeight;
    private String calculationFormula;
}