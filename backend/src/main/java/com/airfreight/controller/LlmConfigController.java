package com.airfreight.controller;

import com.airfreight.entity.LlmConfig;
import com.airfreight.service.LlmConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LLM配置管理 - 配置大语言模型服务（多服务商）
 */
@RestController
@RequestMapping("/api/llm-config")
@RequiredArgsConstructor
@Tag(name = "LLM配置管理", description = "大语言模型配置管理（OpenAI/Claude/Azure/本地模型）")
@CrossOrigin(origins = "*")
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    @PostMapping
    @Operation(summary = "添加LLM配置")
    public ResponseEntity<LlmConfig> addConfig(@RequestBody LlmConfig config) {
        return ResponseEntity.ok(llmConfigService.saveConfig(config));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新LLM配置")
    public ResponseEntity<LlmConfig> updateConfig(@PathVariable Long id, @RequestBody LlmConfig config) {
        config.setId(id);
        return ResponseEntity.ok(llmConfigService.saveConfig(config));
    }

    @GetMapping
    @Operation(summary = "获取所有LLM配置")
    public ResponseEntity<List<LlmConfig>> getAllConfigs() {
        return ResponseEntity.ok(llmConfigService.getAllConfigs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个LLM配置")
    public ResponseEntity<LlmConfig> getConfig(@PathVariable Long id) {
        return llmConfigService.getConfig(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    @Operation(summary = "获取当前激活的LLM配置")
    public ResponseEntity<LlmConfig> getActiveConfig() {
        return llmConfigService.getActiveConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活LLM配置", description = "激活指定配置，同时停用其他配置")
    public ResponseEntity<LlmConfig> activateConfig(@PathVariable Long id) {
        return ResponseEntity.ok(llmConfigService.activateConfig(id));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试LLM连接", description = "测试LLM服务是否可用")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(llmConfigService.testConnection(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除LLM配置")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        llmConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }
}
