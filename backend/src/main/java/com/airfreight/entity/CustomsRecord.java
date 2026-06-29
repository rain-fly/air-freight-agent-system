package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 报关记录 - 对应工作流第7步（出口报关）和第12步（目的港清关）
 */
@Data
@Entity
@Table(name = "customs_records")
public class CustomsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String declarationNo;          // 报关编号

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 20)
    private String customsType;            // EXPORT(出口) / IMPORT(进口)

    @Column(length = 30)
    private String hsCode;                 // HS编码

    @Column(length = 20)
    private String status;                 // PENDING / SUBMITTED / INSPECTING / CLEARED / REJECTED

    @Column(columnDefinition = "TEXT")
    private String declarationData;        // 申报数据（JSON格式）

    @Column(length = 200)
    private String inspectorName;          // 查验人

    private LocalDateTime submittedAt;     // 申报时间
    private LocalDateTime clearedAt;       // 放行时间
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