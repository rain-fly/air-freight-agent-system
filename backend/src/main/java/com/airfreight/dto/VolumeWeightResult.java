package com.airfreight.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 体积重计算结果DTO
 */
@Data
public class VolumeWeightResult {
    private BigDecimal grossWeight;
    private BigDecimal volumeWeight;
    private BigDecimal chargeableWeight;
    private String calculationFormula;
}