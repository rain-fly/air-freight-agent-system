package com.airfreight.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
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

    @Column(length = 64)
    private String messageId;             // 邮件唯一ID（IMAP UID或标准化Message-ID，>63则SHA256）

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

    @Column(length = 1000)
    private String attachmentNames;       // 附件名称列表（逗号分隔）

    private Boolean isRead;               // 是否已读
    private Boolean isStarred;            // 是否星标

    @Column(length = 20)
    private String priority;              // HIGH / NORMAL / LOW

    private LocalDateTime sentDate;       // 发送时间
    private LocalDateTime receivedDate;   // 接收时间

    // ===== 分类相关字段 =====
    @Column(length = 50)
    private String category;              // 主分类：工作/商务/营销/通知/社交/垃圾邮件/其他

    @Column(length = 200)
    private String tags;                  // 标签：逗号分隔，如"询价,紧急,客户A"

    @Column(length = 50)
    private String classificationMethod;  // 分类方法：RULE/LLM/HYBRID/MANUAL

    @Column(precision = 4, scale = 3)
    private BigDecimal classificationConfidence;  // 分类置信度 0.000-1.000

    @Column(columnDefinition = "TEXT")
    private String classificationReason;  // 分类理由（LLM返回的推理）

    private Boolean isClassified;         // 是否已分类
    private LocalDateTime classifiedAt;   // 分类时间

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
