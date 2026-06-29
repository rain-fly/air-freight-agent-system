package com.airfreight.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 客户信息 - 支持国内/国外客户
 */
@Data
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;                  // 客户名称

    @Column(length = 20)
    private String type;                  // SHIPPER/CONSIGNEE/AGENT

    @Column(length = 100)
    private String contactPerson;         // 联系人

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String country;               // 国家

    @Column(length = 50)
    private String taxId;                 // 税号

    @Column(length = 50)
    private String bankAccount;           // 银行账号

    @Column(length = 200)
    private String bankName;              // 开户行

    @Column(length = 20)
    private String swiftCode;             // SWIFT代码（国际转账用）

    @Column(length = 200)
    private String remark;

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