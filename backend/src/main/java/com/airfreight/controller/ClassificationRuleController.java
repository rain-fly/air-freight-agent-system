package com.airfreight.controller;

import com.airfreight.entity.ClassificationRule;
import com.airfreight.service.ClassificationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分类规则管理 - 管理基于关键词的邮件分类规则
 */
@RestController
@RequestMapping("/api/classification-rules")
@RequiredArgsConstructor
@Tag(name = "分类规则管理", description = "邮件分类规则配置（基于关键词匹配）")
@CrossOrigin(origins = "*")
public class ClassificationRuleController {

    private final ClassificationRuleService ruleService;

    @PostMapping
    @Operation(summary = "添加分类规则")
    public ResponseEntity<ClassificationRule> addRule(@RequestBody ClassificationRule rule) {
        return ResponseEntity.ok(ruleService.saveRule(rule));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类规则")
    public ResponseEntity<ClassificationRule> updateRule(@PathVariable Long id, @RequestBody ClassificationRule rule) {
        rule.setId(id);
        return ResponseEntity.ok(ruleService.saveRule(rule));
    }

    @GetMapping
    @Operation(summary = "获取所有分类规则")
    public ResponseEntity<List<ClassificationRule>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    @GetMapping("/active")
    @Operation(summary = "获取激活的分类规则")
    public ResponseEntity<List<ClassificationRule>> getActiveRules() {
        return ResponseEntity.ok(ruleService.getActiveRules());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类规则")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/init-default")
    @Operation(summary = "初始化默认分类规则", description = "初始化空运代理业务的默认分类规则")
    public ResponseEntity<Map<String, Object>> initDefaultRules() {
        int count = ruleService.initDefaultRules();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("initCount", count);
        result.put("message", count > 0 ? "默认规则初始化完成，共 " + count + " 条" : "规则已存在，无需初始化");
        return ResponseEntity.ok(result);
    }
}
