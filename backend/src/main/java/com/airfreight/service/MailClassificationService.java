package com.airfreight.service;

import com.airfreight.entity.ClassificationRule;
import com.airfreight.entity.LlmConfig;
import com.airfreight.entity.MailMessage;
import com.airfreight.repository.ClassificationRuleRepository;
import com.airfreight.repository.MailMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 邮件分类服务 - 支持规则分类、LLM分类、混合分类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailClassificationService {

    private final MailMessageRepository messageRepository;
    private final ClassificationRuleRepository ruleRepository;
    private final LlmService llmService;

    /**
     * 分类方法枚举
     */
    public static final String METHOD_RULE = "RULE";        // 基于规则
    public static final String METHOD_LLM = "LLM";          // 基于LLM
    public static final String METHOD_HYBRID = "HYBRID";    // 混合模式
    public static final String METHOD_MANUAL = "MANUAL";    // 手动分类

    /**
     * 规则分类置信度阈值，低于此值时混合模式会用LLM重新分类
     */
    private static final BigDecimal RULE_CONFIDENCE_THRESHOLD = new BigDecimal("0.700");

    /**
     * 对单封邮件进行分类
     */
    @Transactional
    public MailMessage classifyMail(Long mailId, String method) {
        MailMessage mail = messageRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("邮件不存在: " + mailId));
        return classifyMail(mail, method);
    }

    /**
     * 对单封邮件进行分类（内存对象）
     */
    @Transactional
    public MailMessage classifyMail(MailMessage mail, String method) {
        if (method == null) method = METHOD_HYBRID;

        switch (method.toUpperCase()) {
            case "RULE":
                return doClassifyByRule(mail);
            case "LLM":
                return doClassifyByLlm(mail);
            case "HYBRID":
                return doClassifyByHybrid(mail);
            case "MANUAL":
                // 手动分类不在此处理，由前端直接修改category字段
                return mail;
            default:
                return doClassifyByHybrid(mail);
        }
    }

    /**
     * 批量分类（按邮件ID列表）
     */
    @Transactional
    public List<MailMessage> classifyMailsBatch(List<Long> mailIds, String method) {
        List<MailMessage> results = new ArrayList<>();
        for (Long id : mailIds) {
            try {
                MailMessage classified = classifyMail(id, method);
                results.add(classified);
            } catch (Exception e) {
                log.error("邮件 {} 分类失败: {}", id, e.getMessage());
            }
        }
        return results;
    }

    /**
     * 对账号下所有未分类邮件进行分类
     */
    @Transactional
    public int classifyAccountMails(Long accountId, String method) {
        List<MailMessage> unclassified = messageRepository.findByAccountIdOrderByReceivedDateDesc(accountId)
                .stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsClassified()))
                .collect(Collectors.toList());

        log.info("账号 {} 共 {} 封未分类邮件待处理", accountId, unclassified.size());

        int successCount = 0;
        for (MailMessage mail : unclassified) {
            try {
                classifyMail(mail, method);
                successCount++;
            } catch (Exception e) {
                log.error("邮件 {} 分类失败: {}", mail.getId(), e.getMessage());
            }
        }
        log.info("账号 {} 分类完成，成功 {}/{} 封", accountId, successCount, unclassified.size());
        return successCount;
    }

    /**
     * 手动分类 - 用户指定分类和标签
     */
    @Transactional
    public MailMessage manualClassify(Long mailId, String category, String tags) {
        MailMessage mail = messageRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("邮件不存在: " + mailId));
        mail.setCategory(category);
        mail.setTags(tags);
        mail.setClassificationMethod(METHOD_MANUAL);
        mail.setClassificationConfidence(BigDecimal.valueOf(1.000).setScale(3, RoundingMode.HALF_UP));
        mail.setClassificationReason("用户手动分类");
        mail.setIsClassified(true);
        mail.setClassifiedAt(LocalDateTime.now());
        return messageRepository.save(mail);
    }

    // ===== 分类方法实现 =====

    /**
     * 基于规则分类
     */
    private MailMessage doClassifyByRule(MailMessage mail) {
        List<ClassificationRule> rules = ruleRepository.findByIsActiveTrueOrderByPriorityAsc();

        if (rules.isEmpty()) {
            // 没有配置规则，使用内置默认规则
            applyDefaultRules(mail);
        } else {
            applyCustomRules(mail, rules);
        }

        mail.setClassificationMethod(METHOD_RULE);
        if (mail.getClassificationConfidence() == null) {
            mail.setClassificationConfidence(new BigDecimal("0.500"));
        }
        mail.setIsClassified(true);
        mail.setClassifiedAt(LocalDateTime.now());
        return messageRepository.save(mail);
    }

    /**
     * 基于LLM分类
     */
    private MailMessage doClassifyByLlm(MailMessage mail) {
        LlmConfig config = llmService.getActiveConfig()
                .orElseThrow(() -> new RuntimeException("未配置激活的LLM，请先在LLM配置中添加并激活一个配置"));

        try {
            Map<String, Object> result = llmService.classifyMail(mail, config);

            mail.setCategory((String) result.get("category"));

            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) result.get("tags");
            if (tags != null && !tags.isEmpty()) {
                mail.setTags(String.join(",", tags));
            }

            Object confidence = result.get("confidence");
            if (confidence instanceof Number) {
                mail.setClassificationConfidence(BigDecimal.valueOf(((Number) confidence).doubleValue()).setScale(3, RoundingMode.HALF_UP));
            }

            mail.setClassificationReason((String) result.get("reason"));
            mail.setClassificationMethod(METHOD_LLM);
            mail.setIsClassified(true);
            mail.setClassifiedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("LLM分类失败: {}", e.getMessage());
            // LLM分类失败时回退到规则分类
            applyDefaultRules(mail);
            mail.setClassificationMethod(METHOD_RULE);
            mail.setClassificationReason("LLM分类失败，已回退到规则分类: " + e.getMessage());
            mail.setIsClassified(true);
            mail.setClassifiedAt(LocalDateTime.now());
        }
        return messageRepository.save(mail);
    }

    /**
     * 混合分类：先用规则，置信度低于阈值时用LLM重新分类
     */
    private MailMessage doClassifyByHybrid(MailMessage mail) {
        // 第一步：规则分类
        List<ClassificationRule> rules = ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
        if (rules.isEmpty()) {
            applyDefaultRules(mail);
        } else {
            applyCustomRules(mail, rules);
        }

        BigDecimal ruleConfidence = mail.getClassificationConfidence();
        if (ruleConfidence == null) ruleConfidence = new BigDecimal("0.500");

        // 第二步：如果规则分类置信度低于阈值，使用LLM重新分类
        if (ruleConfidence.compareTo(RULE_CONFIDENCE_THRESHOLD) < 0) {
            try {
                LlmConfig config = llmService.getActiveConfig().orElse(null);
                if (config != null) {
                    Map<String, Object> result = llmService.classifyMail(mail, config);
                    mail.setCategory((String) result.get("category"));

                    @SuppressWarnings("unchecked")
                    List<String> tags = (List<String>) result.get("tags");
                    if (tags != null && !tags.isEmpty()) {
                        mail.setTags(String.join(",", tags));
                    }

                    Object confidence = result.get("confidence");
                    if (confidence instanceof Number) {
                        mail.setClassificationConfidence(BigDecimal.valueOf(((Number) confidence).doubleValue()).setScale(3, RoundingMode.HALF_UP));
                    }
                    mail.setClassificationReason((String) result.get("reason"));
                    mail.setClassificationMethod(METHOD_HYBRID);
                } else {
                    // 没有配置LLM，保持规则分类结果
                    mail.setClassificationMethod(METHOD_HYBRID);
                    mail.setClassificationReason("未配置LLM，使用规则分类结果");
                }
            } catch (Exception e) {
                log.error("混合分类中LLM调用失败，保持规则分类结果: {}", e.getMessage());
                mail.setClassificationMethod(METHOD_HYBRID);
                mail.setClassificationReason("LLM调用失败，使用规则分类结果: " + e.getMessage());
            }
        } else {
            // 规则置信度足够，直接使用规则分类结果
            mail.setClassificationMethod(METHOD_HYBRID);
        }

        mail.setIsClassified(true);
        mail.setClassifiedAt(LocalDateTime.now());
        return messageRepository.save(mail);
    }

    // ===== 规则匹配逻辑 =====

    /**
     * 应用自定义分类规则
     */
    private void applyCustomRules(MailMessage mail, List<ClassificationRule> rules) {
        for (ClassificationRule rule : rules) {
            if (matchRule(mail, rule)) {
                mail.setCategory(rule.getCategory());
                mail.setTags(rule.getTags());
                mail.setClassificationConfidence(rule.getConfidence() == null ? new BigDecimal("0.700") : rule.getConfidence());
                mail.setClassificationReason("匹配规则: " + rule.getName());
                return;
            }
        }
        // 没有匹配到任何规则
        mail.setCategory("其他");
        mail.setTags(null);
        mail.setClassificationConfidence(new BigDecimal("0.400"));
        mail.setClassificationReason("未匹配到任何分类规则");
    }

    /**
     * 应用内置默认规则（空运代理业务相关）
     */
    private void applyDefaultRules(MailMessage mail) {
        String subject = safeLower(mail.getSubject());
        String content = safeLower(mail.getContent());
        String from = safeLower(mail.getFromAddress());

        // 商务类：询价、报价
        if (containsAny(subject, "询价", "报价", "quotation", "quote", "price", "价格", "rate", "运费报价")) {
            setClassification(mail, "商务", "询价,报价", 0.85, "匹配商务类关键词");
            return;
        }
        // 工作类：订单、订舱、运单
        if (containsAny(subject, "订舱", "booking", "订单", "order", "运单", "awb", "提单", "提货", "换单", "装机")) {
            setClassification(mail, "工作", "订舱,运单", 0.85, "匹配工作类关键词");
            return;
        }
        // 财务类：发票、付款
        if (containsAny(subject, "发票", "invoice", "付款", "payment", "对账", "结算", "账单", "汇率", "swift")) {
            setClassification(mail, "财务", "发票,付款", 0.85, "匹配财务类关键词");
            return;
        }
        // 通知类：航班动态、状态更新
        if (containsAny(subject, "航班", "flight", "通知", "notice", "状态", "status", "到货", "起飞", "清关", "报关")) {
            setClassification(mail, "通知", "航班动态", 0.80, "匹配通知类关键词");
            return;
        }
        // 报关类归入工作
        if (containsAny(subject, "报关", "customs", "海关", "hs编码", "关税")) {
            setClassification(mail, "工作", "报关", 0.80, "匹配报关类关键词");
            return;
        }
        // 营销类
        if (containsAny(subject, "推广", "优惠", "活动", "广告", "subscribe", "unsubscribe", "退订", "marketing") ||
                containsAny(from, "noreply", "no-reply", "newsletter", "marketing")) {
            setClassification(mail, "营销", "广告推广", 0.75, "匹配营销类关键词");
            return;
        }
        // 垃圾邮件
        if (containsAny(subject, "中奖", "free", "viagra", "casino", "lottery", "click here", "限时免费", "领奖") ||
                containsAny(content, "点击链接", "立即领取", "可疑链接")) {
            setClassification(mail, "垃圾邮件", "可疑", 0.70, "匹配垃圾邮件关键词");
            return;
        }
        // 社交类
        if (containsAny(subject, "你好", "问候", "thanks", "thank you", "regards", "best regards", "会议", "meeting", "邀请")) {
            setClassification(mail, "社交", "问候", 0.65, "匹配社交类关键词");
            return;
        }

        // 默认未匹配
        setClassification(mail, "其他", null, 0.40, "未匹配到任何关键词");
    }

    private void setClassification(MailMessage mail, String category, String tags, double confidence, String reason) {
        mail.setCategory(category);
        mail.setTags(tags);
        mail.setClassificationConfidence(BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP));
        mail.setClassificationReason(reason);
    }

    /**
     * 判断邮件是否匹配某条规则
     */
    private boolean matchRule(MailMessage mail, ClassificationRule rule) {
        if (rule.getKeywords() == null || rule.getKeywords().isEmpty()) return false;

        String[] keywords = rule.getKeywords().split(",");
        String field = rule.getField() == null ? "BOTH" : rule.getField().toUpperCase();
        String subject = safeLower(mail.getSubject());
        String content = safeLower(mail.getContent());

        for (String kw : keywords) {
            String keyword = kw.trim().toLowerCase();
            if (keyword.isEmpty()) continue;

            if (field.equals("SUBJECT") && subject.contains(keyword)) {
                return true;
            } else if (field.equals("CONTENT") && content.contains(keyword)) {
                return true;
            } else if (field.equals("BOTH") && (subject.contains(keyword) || content.contains(keyword))) {
                return true;
            }
        }
        return false;
    }

    // ===== 工具方法 =====

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
