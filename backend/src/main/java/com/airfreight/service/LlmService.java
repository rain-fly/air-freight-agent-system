package com.airfreight.service;

import com.airfreight.entity.LlmConfig;
import com.airfreight.entity.MailMessage;
import com.airfreight.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * LLM集成服务 - 调用大语言模型API进行邮件分类
 * 支持多服务商：OpenAI / Claude / Azure / 本地兼容OpenAI接口的模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestTemplate restTemplate;
    private final LlmConfigRepository llmConfigRepository;

    /**
     * 获取当前激活的LLM配置
     */
    public Optional<LlmConfig> getActiveConfig() {
        return llmConfigRepository.findByIsActiveTrue();
    }

    /**
     * 调用LLM对邮件进行分类
     * @return 分类结果Map：{category, tags, confidence, reason}
     */
    public Map<String, Object> classifyMail(MailMessage mail, LlmConfig config) {
        String prompt = buildClassificationPrompt(mail, config);
        String response = callLlm(config, prompt);
        return parseLlmResponse(response);
    }

    /**
     * 通用LLM调用 - 根据服务商类型分发
     */
    public String callLlm(LlmConfig config, String prompt) {
        if (config == null) {
            throw new RuntimeException("未配置LLM");
        }
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new RuntimeException("LLM API密钥未配置");
        }

        String provider = config.getProvider() == null ? "OPENAI" : config.getProvider().toUpperCase();

        switch (provider) {
            case "OPENAI":
            case "LOCAL":
                return callOpenAiCompatible(config, prompt);
            case "AZURE":
                return callAzureOpenAi(config, prompt);
            case "CLAUDE":
                return callClaude(config, prompt);
            default:
                // 默认按OpenAI兼容接口处理（支持ollama、vLLM等本地模型）
                log.warn("未知的LLM服务商 {}，按OpenAI兼容接口处理", provider);
                return callOpenAiCompatible(config, prompt);
        }
    }

    /**
     * 调用OpenAI兼容接口（OpenAI / 本地模型 / ollama / vLLM）
     */
    @SuppressWarnings("unchecked")
    private String callOpenAiCompatible(LlmConfig config, String prompt) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://api.openai.com";
        }
        // 拼接chat completions端点
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        String url = baseUrl + "v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个专业的邮件分类助手，专门对空运代理业务相关的邮件进行分类。请严格按照用户要求的JSON格式返回结果。");

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel() == null ? "gpt-3.5-turbo" : config.getModel());
        requestBody.put("messages", Arrays.asList(systemMsg, userMsg));
        requestBody.put("temperature", config.getTemperature() == null ? 0.3 : config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens() == null ? 500 : config.getMaxTokens());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("LLM返回空响应");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("LLM返回的choices为空");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("调用OpenAI兼容接口失败: {}", e.getMessage());
            throw new RuntimeException("调用LLM失败: " + e.getMessage());
        }
    }

    /**
     * 调用Azure OpenAI
     */
    @SuppressWarnings("unchecked")
    private String callAzureOpenAi(LlmConfig config, String prompt) {
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        String apiVersion = config.getApiVersion() == null ? "2024-02-15-preview" : config.getApiVersion();
        String url = baseUrl + "openai/deployments/" + config.getModel() + "/chat/completions?api-version=" + apiVersion;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", config.getApiKey());

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个专业的邮件分类助手。请严格按照JSON格式返回结果。");

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", Arrays.asList(systemMsg, userMsg));
        requestBody.put("temperature", config.getTemperature() == null ? 0.3 : config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens() == null ? 500 : config.getMaxTokens());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("调用Azure OpenAI失败: {}", e.getMessage());
            throw new RuntimeException("调用Azure OpenAI失败: " + e.getMessage());
        }
    }

    /**
     * 调用Anthropic Claude
     */
    @SuppressWarnings("unchecked")
    private String callClaude(LlmConfig config, String prompt) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://api.anthropic.com";
        }
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        String url = baseUrl + "v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", config.getApiKey());
        headers.set("anthropic-version", config.getApiVersion() == null ? "2023-06-01" : config.getApiVersion());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel() == null ? "claude-3-5-sonnet-20241022" : config.getModel());
        requestBody.put("max_tokens", config.getMaxTokens() == null ? 500 : config.getMaxTokens());
        requestBody.put("temperature", config.getTemperature() == null ? 0.3 : config.getTemperature());

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("type", "text");
        systemMsg.put("text", "你是一个专业的邮件分类助手，专门对空运代理业务相关的邮件进行分类。请严格按照用户要求的JSON格式返回结果。");

        requestBody.put("system", Collections.singletonList(systemMsg));

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        requestBody.put("messages", Collections.singletonList(userMsg));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
            if (content == null || content.isEmpty()) {
                throw new RuntimeException("Claude返回空内容");
            }
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            log.error("调用Claude失败: {}", e.getMessage());
            throw new RuntimeException("调用Claude失败: " + e.getMessage());
        }
    }

    /**
     * 构建分类提示词
     */
    public String buildClassificationPrompt(MailMessage mail, LlmConfig config) {
        // 优先使用配置中的自定义提示词模板
        if (config != null && config.getClassificationPrompt() != null && !config.getClassificationPrompt().isEmpty()) {
            String custom = config.getClassificationPrompt();
            custom = custom.replace("{subject}", safeStr(mail.getSubject()));
            custom = custom.replace("{content}", truncate(safeStr(mail.getContent()), 1500));
            custom = custom.replace("{from}", safeStr(mail.getFromAddress()));
            return custom;
        }

        // 默认提示词
        String subject = safeStr(mail.getSubject());
        String content = truncate(safeStr(mail.getContent()), 1500);
        String from = safeStr(mail.getFromAddress());

        return "请对以下邮件进行分类。\n\n" +
                "发件人：" + from + "\n" +
                "邮件标题：" + subject + "\n" +
                "邮件内容：" + content + "\n\n" +
                "请从以下分类中选择最合适的一个：\n" +
                "- 工作：与具体工作任务、订单处理、订舱、运单相关\n" +
                "- 商务：询价、报价、合同、合作等商务沟通\n" +
                "- 财务：发票、付款、对账、税务相关\n" +
                "- 通知：系统通知、航班动态、状态更新\n" +
                "- 社交：私人问候、闲聊、社交往来\n" +
                "- 营销：广告推广、产品推介、活动通知\n" +
                "- 垃圾邮件：无关、可疑、钓鱼或垃圾内容\n" +
                "- 其他：无法归入以上分类的内容\n\n" +
                "请严格按照以下JSON格式返回（不要包含其他任何文字）：\n" +
                "{\"category\":\"分类名称\",\"tags\":[\"标签1\",\"标签2\"],\"confidence\":0.95,\"reason\":\"分类理由\"}\n" +
                "说明：\n" +
                "- category必须是上述8个分类之一\n" +
                "- tags是1-3个关键词标签\n" +
                "- confidence是0到1之间的置信度\n" +
                "- reason是简要的分类理由（50字以内）";
    }

    /**
     * 解析LLM返回的分类结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseLlmResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        if (response == null || response.trim().isEmpty()) {
            result.put("category", "其他");
            result.put("tags", new ArrayList<String>());
            result.put("confidence", 0.3);
            result.put("reason", "LLM返回空响应");
            return result;
        }

        // 提取JSON部分（LLM可能在前后加了markdown代码块或文字）
        String json = extractJson(response);

        try {
            // 简易JSON解析（避免引入额外依赖）
            // 这里用正则提取关键字段，兼容LLM返回格式差异
            result.put("category", extractJsonField(json, "category", "其他"));
            result.put("tags", extractJsonArray(json, "tags"));
            result.put("confidence", extractJsonNumber(json, "confidence", 0.5));
            result.put("reason", extractJsonField(json, "reason", ""));
        } catch (Exception e) {
            log.warn("解析LLM响应失败，使用默认值: {}", e.getMessage());
            result.put("category", "其他");
            result.put("tags", new ArrayList<String>());
            result.put("confidence", 0.3);
            result.put("reason", "解析失败: " + truncate(e.getMessage(), 100));
        }

        return result;
    }

    /**
     * 测试LLM连接
     */
    public Map<String, Object> testConnection(LlmConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            String testPrompt = "请回复\"连接成功\"四个字。";
            String response = callLlm(config, testPrompt);
            result.put("success", true);
            result.put("message", "连接测试成功");
            result.put("response", truncate(response, 200));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", truncate(e.getMessage(), 200));
        }
        return result;
    }

    // ===== 私有辅助方法 =====

    private String safeStr(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /**
     * 从LLM响应中提取JSON部分
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        // 去除markdown代码块
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        // 提取第一个{ 到最后一个 }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * 提取JSON字符串字段
     */
    private String extractJsonField(String json, String field, String defaultValue) {
        // 匹配 "field":"value" 或 "field": "value"
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return defaultValue;
        int colonIdx = json.indexOf(':', idx);
        if (colonIdx < 0) return defaultValue;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return defaultValue;
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return defaultValue;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * 提取JSON数组字段（字符串数组）
     */
    @SuppressWarnings("unchecked")
    private List<String> extractJsonArray(String json, String field) {
        List<String> result = new ArrayList<>();
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return result;
        int bracketStart = json.indexOf('[', idx);
        int bracketEnd = json.indexOf(']', idx);
        if (bracketStart < 0 || bracketEnd < 0) return result;
        String arrayStr = json.substring(bracketStart + 1, bracketEnd);
        // 提取所有引号内的字符串
        int pos = 0;
        while (pos < arrayStr.length()) {
            int q1 = arrayStr.indexOf('"', pos);
            if (q1 < 0) break;
            int q2 = arrayStr.indexOf('"', q1 + 1);
            if (q2 < 0) break;
            result.add(arrayStr.substring(q1 + 1, q2));
            pos = q2 + 1;
        }
        return result;
    }

    /**
     * 提取JSON数字字段
     */
    private double extractJsonNumber(String json, String field, double defaultValue) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return defaultValue;
        int colonIdx = json.indexOf(':', idx);
        if (colonIdx < 0) return defaultValue;
        int pos = colonIdx + 1;
        // 跳过空格
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        StringBuilder num = new StringBuilder();
        while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.' || json.charAt(pos) == '-')) {
            num.append(json.charAt(pos));
            pos++;
        }
        if (num.length() == 0) return defaultValue;
        try {
            return Double.parseDouble(num.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
