package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * LLM配置 - 大语言模型服务配置
 * 支持多服务商：OpenAI / Claude / Azure / 本地模型
 */
@Data
@Entity
@Table(name = "llm_configs")
public class LlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                  // 配置名称，如"OpenAI GPT-4"

    @Column(length = 20)
    private String provider;              // 服务商：OPENAI/CLAUDE/AZURE/LOCAL

    @Column(length = 500)
    private String apiKey;                // API密钥

    @Column(length = 500)
    private String baseUrl;               // API基础URL

    @Column(length = 100)
    private String model;                 // 模型名称，如"gpt-4-turbo"

    private Double temperature;           // 温度参数（0.0-2.0）

    private Integer maxTokens;            // 最大token数

    @Column(length = 20)
    private String apiVersion;            // API版本（Azure用）

    private Boolean isActive;             // 是否激活（同时只能有一个激活）

    @Column(columnDefinition = "TEXT")
    private String classificationPrompt;  // 分类提示词模板

    @Column(length = 500)
    private String remark;                // 备注

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
