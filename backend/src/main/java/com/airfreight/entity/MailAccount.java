package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件客户端配置 - 保存POP3/SMTP邮箱账号配置
 */
@Data
@Entity
@Table(name = "mail_accounts")
public class MailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String accountName;           // 账号别名（如"我的QQ邮箱"）

    @Column(nullable = false, length = 200)
    private String emailAddress;          // 邮箱地址

    @Column(nullable = false, length = 200)
    private String password;              // 密码/授权码

    // SMTP配置
    @Column(nullable = false, length = 200)
    private String smtpHost;              // SMTP服务器
    private Integer smtpPort;             // SMTP端口（默认587或465）
    private Boolean smtpSsl;              // SMTP是否SSL

    // POP3配置
    @Column(nullable = false, length = 200)
    private String pop3Host;              // POP3服务器
    private Integer pop3Port;             // POP3端口（默认995）
    private Boolean pop3Ssl;              // POP3是否SSL

    @Column(length = 20)
    private String status;                // ACTIVE / DISABLED

    private LocalDateTime lastCheckAt;    // 最后检查时间
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