package com.airfreight.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务记录 - 对应工作流中的费用结算
 */
@Data
@Entity
@Table(name = "financials")
public class Financial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String invoiceNo;             // 账单编号

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 50)
    private String feeType;               // 费用类型: FREIGHT/FSC/SSC/GROUND/CUSTOMS/DUTY/VAT/STORAGE/INSURANCE/TOTAL

    private BigDecimal amount;            // 金额
    private BigDecimal exchangeRate;      // 汇率

    @Column(length = 10)
    private String currency;              // 币种: CNY/USD/EUR

    @Column(length = 20)
    private String paymentStatus;         // 支付状态: PENDING/PAID/OVERDUE/CANCELLED

    @Column(length = 50)
    private String paymentMethod;         // 支付方式: SWIFT/BANK_TRANSFER/ALIPAY/WECHAT

    @Column(length = 200)
    private String paymentRef;            // 支付参考号（SWIFT交易号等）

    private LocalDateTime dueDate;        // 到期日
    private LocalDateTime paidAt;         // 支付时间
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