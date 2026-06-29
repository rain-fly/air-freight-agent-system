package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知推送记录
 */
@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 20)
    private String channel;                // EMAIL / SMS / WECHAT / APP

    @Column(length = 50)
    private String recipient;              // 收件人（邮箱/手机号/微信ID）

    @Column(length = 100)
    private String subject;                // 标题

    @Column(columnDefinition = "TEXT")
    private String content;                // 内容

    @Column(length = 20)
    private String status;                 // PENDING / SENT / FAILED

    @Column(length = 20)
    private String notifyType;             // ARRIVAL_NOTICE / CUSTOMS_STATUS / PAYMENT_REMINDER / DELIVERY

    private LocalDateTime sentAt;          // 发送时间
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}