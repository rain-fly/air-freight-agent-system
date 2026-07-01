package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分类规则 - 基于关键词的邮件分类规则
 */
@Data
@Entity
@Table(name = "classification_rules")
public class ClassificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                  // 规则名称

    @Column(length = 20)
    private String field;                 // 匹配字段：SUBJECT/CONTENT/BOTH

    @Column(length = 500)
    private String keywords;              // 关键词，逗号分隔

    @Column(length = 50)
    private String category;              // 分类

    @Column(length = 200)
    private String tags;                  // 标签

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;        // 置信度

    private Boolean isActive;             // 是否激活

    private Integer priority;             // 优先级（数字越小优先级越高）

    @Column(length = 500)
    private String description;           // 描述

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
