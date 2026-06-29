package com.airfreight.service;

import com.airfreight.dto.QuotationRequest;
import com.airfreight.dto.QuotationResponse;
import com.airfreight.dto.VolumeWeightResult;
import com.airfreight.entity.Shipment;
import com.airfreight.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 询价服务 - 对应工作流第1步：需求确认与询价
 * 包含：体积重计算、Incoterms校验、报价生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationService {

    private final ShipmentRepository shipmentRepository;

    /**
     * 计算体积重
     * 空运标准：体积重(kg) = 长(cm) × 宽(cm) × 高(cm) / 6000
     */
    public VolumeWeightResult calculateVolumeWeight(BigDecimal grossWeight,
                                                    BigDecimal length,
                                                    BigDecimal width,
                                                    BigDecimal height) {
        VolumeWeightResult result = new VolumeWeightResult();
        result.setGrossWeight(grossWeight);

        // 体积重 = 长 × 宽 × 高 / 6000
        BigDecimal volumeWeight = length.multiply(width).multiply(height)
                .divide(BigDecimal.valueOf(6000), 2, RoundingMode.HALF_UP);
        result.setVolumeWeight(volumeWeight);

        // 计费重 = max(毛重, 体积重)
        BigDecimal chargeable = grossWeight.compareTo(volumeWeight) > 0 ? grossWeight : volumeWeight;
        result.setChargeableWeight(chargeable);
        result.setCalculationFormula(
                String.format("体积重=%.0f×%.0f×%.0f/6000=%.2fkg, 计费重=max(毛重%.2fkg, 体积重%.2fkg)=%.2fkg",
                        length, width, height, volumeWeight, grossWeight, volumeWeight, chargeable));

        log.info("体积重计算: {}", result.getCalculationFormula());
        return result;
    }

    /**
     * 生成报价
     * 模拟费率（实际使用中应对接航空公司实时运价API）
     */
    public QuotationResponse generateQuotation(QuotationRequest request) {
        // 1. 计算体积重和计费重
        VolumeWeightResult vwResult = calculateVolumeWeight(
                request.getGrossWeight(),
                request.getLengthCm(),
                request.getWidthCm(),
                request.getHeightCm()
        );

        // 2. 模拟航线费率（元/kg，实际应查询IATA/航司API）
        BigDecimal ratePerKg = getSimulatedRate(request.getOriginAirport(), request.getDestAirport());

        // 3. 计算各项费用
        BigDecimal chargeableWeight = vwResult.getChargeableWeight();
        BigDecimal freightCharge = ratePerKg.multiply(chargeableWeight).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fuelSurcharge = freightCharge.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP); // 15% FSC
        BigDecimal securitySurcharge = chargeableWeight.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP); // 0.5元/kg SSC
        BigDecimal groundHandlingFee = chargeableWeight.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP); // 0.8元/kg
        BigDecimal customsBrokerFee = BigDecimal.valueOf(200).setScale(2, RoundingMode.HALF_UP); // 固定报关费200元
        BigDecimal insuranceFee = freightCharge.multiply(BigDecimal.valueOf(0.002)).setScale(2, RoundingMode.HALF_UP); // 0.2%保险费

        BigDecimal totalCharge = freightCharge.add(fuelSurcharge).add(securitySurcharge)
                .add(groundHandlingFee).add(customsBrokerFee).add(insuranceFee)
                .setScale(2, RoundingMode.HALF_UP);

        // 4. 组装响应
        QuotationResponse response = new QuotationResponse();
        response.setQuotationNo("QTN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + new Random().nextInt(1000));
        response.setVolumeWeight(vwResult.getVolumeWeight());
        response.setChargeableWeight(vwResult.getChargeableWeight());
        response.setFreightCharge(freightCharge);
        response.setFuelSurcharge(fuelSurcharge);
        response.setSecuritySurcharge(securitySurcharge);
        response.setGroundHandlingFee(groundHandlingFee);
        response.setCustomsBrokerFee(customsBrokerFee);
        response.setInsuranceFee(insuranceFee);
        response.setTotalCharge(totalCharge);
        response.setCurrency("CNY");
        response.setValidUntil(LocalDateTime.now().plusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        response.setRemarks(String.format("基于计费重%.2fkg, 费率%.2f元/kg, 含FSC+SSG+地面费+报关费+保险费",
                chargeableWeight, ratePerKg));

        log.info("报价生成: {} 总费用={} {}",
                response.getQuotationNo(), response.getTotalCharge(), response.getCurrency());

        return response;
    }

    /**
     * 模拟航线费率（实际应对接IATA API）
     */
    private BigDecimal getSimulatedRate(String origin, String dest) {
        // 亚洲-北美航线
        if ("PVG".equalsIgnoreCase(origin) && "JFK".equalsIgnoreCase(dest)) {
            return BigDecimal.valueOf(28.50);
        }
        if ("PVG".equalsIgnoreCase(origin) && "LAX".equalsIgnoreCase(dest)) {
            return BigDecimal.valueOf(22.00);
        }
        // 亚洲-欧洲航线
        if ("PVG".equalsIgnoreCase(origin) && "LHR".equalsIgnoreCase(dest)) {
            return BigDecimal.valueOf(26.00);
        }
        if ("PVG".equalsIgnoreCase(origin) && "FRA".equalsIgnoreCase(dest)) {
            return BigDecimal.valueOf(25.50);
        }
        // 亚洲-东南亚
        if ("PVG".equalsIgnoreCase(origin) && "SIN".equalsIgnoreCase(dest)) {
            return BigDecimal.valueOf(12.00);
        }
        // 默认费率
        return BigDecimal.valueOf(30.00);
    }

    /**
     * 根据询价创建正式Shipment
     */
    public Shipment createShipmentFromQuotation(QuotationRequest request, QuotationResponse response) {
        Shipment shipment = new Shipment();
        shipment.setShipmentNo("AFR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        shipment.setStatus("QUOTATION");
        shipment.setGoodsName(request.getGoodsName());
        shipment.setGoodsCategory(request.getGoodsCategory());
        shipment.setPieceCount(request.getPieceCount());
        shipment.setGrossWeight(request.getGrossWeight());
        shipment.setLengthCm(request.getLengthCm());
        shipment.setWidthCm(request.getWidthCm());
        shipment.setHeightCm(request.getHeightCm());
        shipment.setVolumeWeight(response.getVolumeWeight());
        shipment.setChargeableWeight(response.getChargeableWeight());
        shipment.setOriginAirport(request.getOriginAirport());
        shipment.setDestAirport(request.getDestAirport());
        shipment.setIncoterm(request.getIncoterm());
        shipment.setShipperName(request.getShipperName());
        shipment.setConsigneeName(request.getConsigneeName());
        shipment.setFreightCharge(response.getFreightCharge());
        shipment.setFuelSurcharge(response.getFuelSurcharge());
        shipment.setSecuritySurcharge(response.getSecuritySurcharge());
        shipment.setGroundHandlingFee(response.getGroundHandlingFee());
        shipment.setCustomsBrokerFee(response.getCustomsBrokerFee());
        shipment.setInsuranceFee(response.getInsuranceFee());
        shipment.setTotalCharge(response.getTotalCharge());
        shipment.setCurrency(response.getCurrency());
        shipment.setPaid(false);

        return shipmentRepository.save(shipment);
    }
}