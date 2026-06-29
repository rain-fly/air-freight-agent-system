package com.airfreight.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运输主表 - 对应15步工作流的核心实体
 */
@Data
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 1. 基本信息 =====
    @Column(unique = true, nullable = false, length = 50)
    private String shipmentNo;            // 运单号（系统生成，如 AFR20260629001）

    @Column(length = 20)
    private String status;                // 状态: QUOTATION/BOOKED/RECEIVED/CUSTOMS_CLEARED/IN_TRANSIT/ARRIVED/DELIVERED

    // ===== 2. 货物信息 =====
    @Column(nullable = false, length = 200)
    private String goodsName;             // 品名

    @Column(length = 50)
    private String goodsCategory;         // 品类: GENERAL/BATTERY/DANGEROUS/FOOD/COSMETIC

    private Integer pieceCount;           // 件数
    private BigDecimal grossWeight;       // 毛重 (kg)
    private BigDecimal volumeWeight;      // 体积重 (kg) = 长*宽*高/6000
    private BigDecimal chargeableWeight;  // 计费重 = max(毛重, 体积重)
    private BigDecimal lengthCm;          // 长 cm
    private BigDecimal widthCm;           // 宽 cm
    private BigDecimal heightCm;          // 高 cm

    // ===== 3. 运输路线 =====
    @Column(nullable = false, length = 10)
    private String originAirport;         // 起运港代码 (PVG/CAN)

    @Column(nullable = false, length = 10)
    private String destAirport;           // 目的港代码 (JFK/LHR)

    @Column(length = 20)
    private String incoterm;              // 贸易术语: FOB/CIF/EXW/DDP

    // ===== 4. 当事人 =====
    @Column(nullable = false, length = 200)
    private String shipperName;           // 发货人

    @Column(length = 500)
    private String shipperAddress;

    @Column(length = 50)
    private String shipperPhone;

    @Column(nullable = false, length = 200)
    private String consigneeName;         // 收货人

    @Column(length = 500)
    private String consigneeAddress;

    @Column(length = 50)
    private String consigneePhone;

    // ===== 5. 运输信息 =====
    @Column(length = 20)
    private String awbType;               // 提单类型: MAWB/HAWB/DAWB

    @Column(length = 50)
    private String mawbNumber;            // 主单号

    @Column(length = 50)
    private String hawNumber;            // 分单号

    @Column(length = 50)
    private String airlineCode;           // 航空公司代码

    @Column(length = 50)
    private String flightNo;              // 航班号

    private LocalDateTime etd;            // 预计起飞
    private LocalDateTime eta;            // 预计到达
    private LocalDateTime actualDeparture; // 实际起飞
    private LocalDateTime actualArrival;   // 实际到达

    // ===== 6. 报关信息 =====
    @Column(length = 30)
    private String hsCode;                // HS编码

    private Boolean customsCleared;       // 是否已报关放行
    private LocalDateTime customsClearanceTime;

    // ===== 7. 财务 =====
    private BigDecimal freightCharge;     // 空运费
    private BigDecimal fuelSurcharge;     // 燃油附加费 FSC
    private BigDecimal securitySurcharge; // 战争险附加费 SSC
    private BigDecimal groundHandlingFee; // 地面操作费
    private BigDecimal customsBrokerFee;  // 报关费
    private BigDecimal customsDuty;       // 关税
    private BigDecimal vatAmount;         // 增值税
    private BigDecimal storageFee;        // 滞仓费
    private BigDecimal insuranceFee;      // 保险费
    private BigDecimal totalCharge;       // 总费用
    private String currency;              // 币种 (CNY/USD/EUR)

    private Boolean paid;                 // 是否已支付
    private LocalDateTime paidTime;

    // ===== 8. 时间戳 =====
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}