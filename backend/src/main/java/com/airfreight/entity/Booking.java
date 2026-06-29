package com.airfreight.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订舱记录 - 对应工作流第4步
 */
@Data
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String bookingNo;             // 订舱编号

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 50)
    private String bookingStatus;         // 状态: SUBMITTED/CONFIRMED/CANCELLED

    @Column(length = 500, columnDefinition = "TEXT")
    private String bookingNote;           // 订舱备注

    @Column(length = 200)
    private String warehouseReceipt;      // 入仓通知单号

    private LocalDateTime cutOffTime;     // 截止交货时间
    private LocalDateTime confirmedAt;    // 确认时间
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