package com.airfreight.service;

import com.airfreight.entity.ClassificationRule;
import com.airfreight.repository.ClassificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分类规则服务 - 管理基于关键词的邮件分类规则
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationRuleService {

    private final ClassificationRuleRepository ruleRepository;

    /**
     * 保存规则
     */
    @Transactional
    public ClassificationRule saveRule(ClassificationRule rule) {
        if (rule.getIsActive() == null) rule.setIsActive(true);
        if (rule.getPriority() == null) rule.setPriority(10);
        if (rule.getConfidence() == null) rule.setConfidence(new BigDecimal("0.800"));
        if (rule.getField() == null) rule.setField("BOTH");
        return ruleRepository.save(rule);
    }

    /**
     * 查询所有规则
     */
    public List<ClassificationRule> getAllRules() {
        return ruleRepository.findAll();
    }

    /**
     * 查询激活的规则
     */
    public List<ClassificationRule> getActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
    }

    /**
     * 删除规则
     */
    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    /**
     * 初始化默认分类规则（空运代理业务）
     */
    @Transactional
    public int initDefaultRules() {
        if (ruleRepository.count() > 0) {
            return 0;
        }

        saveRule(createRule("询价报价", "BOTH", "询价,报价,quotation,quote,price,运费报价", "商务", "询价,报价", 0.85, 1));
        saveRule(createRule("订舱运单", "BOTH", "订舱,booking,订单,order,运单,awb,提单,提货,换单", "工作", "订舱,运单", 0.85, 2));
        saveRule(createRule("发票付款", "BOTH", "发票,invoice,付款,payment,对账,结算,账单,swift", "财务", "发票,付款", 0.85, 3));
        saveRule(createRule("报关海关", "BOTH", "报关,customs,海关,hs编码,关税,放行", "工作", "报关", 0.80, 4));
        saveRule(createRule("航班通知", "BOTH", "航班,flight,通知,notice,到货,起飞,清关,状态更新", "通知", "航班动态", 0.80, 5));
        saveRule(createRule("营销推广", "BOTH", "推广,优惠,活动,广告,marketing,退订,unsubscribe", "营销", "广告推广", 0.75, 6));
        saveRule(createRule("垃圾邮件", "BOTH", "中奖,free,viagra,casino,lottery,限时免费,领奖", "垃圾邮件", "可疑", 0.70, 7));

        log.info("默认分类规则初始化完成");
        return 7;
    }

    private ClassificationRule createRule(String name, String field, String keywords,
                                         String category, String tags, double confidence, int priority) {
        ClassificationRule rule = new ClassificationRule();
        rule.setName(name);
        rule.setField(field);
        rule.setKeywords(keywords);
        rule.setCategory(category);
        rule.setTags(tags);
        rule.setConfidence(BigDecimal.valueOf(confidence));
        rule.setPriority(priority);
        rule.setDescription("默认规则: " + name);
        return rule;
    }
}
