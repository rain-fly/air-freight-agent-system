package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 运输跟踪记录 - 对应工作流第9步
 */
@Data
@Entity
@Table(name = "tracking_events")
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 20)
    private String eventType;              // DEPARTURE / TRANSFER / ARRIVAL / CUSTOMS / DELIVERY

    @Column(length = 100)
    private String location;               // 当前位置 (机场代码)

    @Column(columnDefinition = "TEXT")
    private String description;            // 事件描述

    @Column(length = 20)
    private String status;                 // IN_TRANSIT / COMPLETED

    private LocalDateTime eventTime;       // 事件发生时间
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}