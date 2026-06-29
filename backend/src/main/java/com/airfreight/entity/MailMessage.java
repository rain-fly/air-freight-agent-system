package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件记录 - 存储收取的邮件
 */
@Data
@Entity
@Table(name = "mail_messages")
public class MailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private MailAccount account;

    @Column(length = 50)
    private String messageId;             // 邮件唯一ID

    @Column(length = 20)
    private String direction;             // INBOX / SENT

    @Column(length = 200)
    private String fromAddress;           // 发件人地址

    @Column(length = 200)
    private String fromPersonal;          // 发件人名称

    @Column(length = 500)
    private String toAddress;             // 收件人地址

    @Column(length = 500)
    private String ccAddress;             // 抄送

    @Column(length = 500)
    private String bccAddress;            // 密送

    @Column(length = 500)
    private String subject;               // 主题

    @Column(columnDefinition = "TEXT")
    private String content;               // 正文（纯文本）

    @Column(columnDefinition = "TEXT")
    private String contentHtml;           // 正文（HTML）

    private Boolean hasAttachments;       // 是否有附件
    private Boolean isRead;               // 是否已读
    private Boolean isStarred;            // 是否星标

    @Column(length = 20)
    private String priority;              // HIGH / NORMAL / LOW

    private LocalDateTime sentDate;       // 发送时间
    private LocalDateTime receivedDate;   // 接收时间
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}