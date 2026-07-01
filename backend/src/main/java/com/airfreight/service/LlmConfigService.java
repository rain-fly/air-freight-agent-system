package com.airfreight.service;

import com.airfreight.entity.LlmConfig;
import com.airfreight.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * LLM配置服务 - 管理LLM配置（多服务商）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmConfigService {

    private final LlmConfigRepository llmConfigRepository;
    private final LlmService llmService;

    /**
     * 保存LLM配置
     */
    @Transactional
    public LlmConfig saveConfig(LlmConfig config) {
        // 设置默认值
        if (config.getTemperature() == null) config.setTemperature(0.3);
        if (config.getMaxTokens() == null) config.setMaxTokens(500);
        if (config.getIsActive() == null) config.setIsActive(false);
        if (config.getProvider() == null) config.setProvider("OPENAI");

        // 如果设置为激活，则将其他配置置为非激活（同时只能有一个激活）
        if (Boolean.TRUE.equals(config.getIsActive())) {
            deactivateAll();
        }

        return llmConfigRepository.save(config);
    }

    /**
     * 获取所有配置
     */
    public List<LlmConfig> getAllConfigs() {
        return llmConfigRepository.findAll();
    }

    /**
     * 获取单个配置
     */
    public Optional<LlmConfig> getConfig(Long id) {
        return llmConfigRepository.findById(id);
    }

    /**
     * 获取激活的配置
     */
    public Optional<LlmConfig> getActiveConfig() {
        return llmConfigRepository.findByIsActiveTrue();
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        llmConfigRepository.deleteById(id);
    }

    /**
     * 激活某个配置（同时停用其他配置）
     */
    @Transactional
    public LlmConfig activateConfig(Long id) {
        LlmConfig config = llmConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LLM配置不存在: " + id));
        deactivateAll();
        config.setIsActive(true);
        return llmConfigRepository.save(config);
    }

    /**
     * 测试LLM连接
     */
    public java.util.Map<String, Object> testConnection(Long id) {
        LlmConfig config = llmConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LLM配置不存在: " + id));
        return llmService.testConnection(config);
    }

    /**
     * 将所有配置设为非激活
     */
    @Transactional
    public void deactivateAll() {
        List<LlmConfig> all = llmConfigRepository.findAll();
        for (LlmConfig c : all) {
            if (Boolean.TRUE.equals(c.getIsActive())) {
                c.setIsActive(false);
                llmConfigRepository.save(c);
            }
        }
    }
}
