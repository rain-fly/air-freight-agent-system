package com.airfreight.controller;

import com.airfreight.entity.MailAccount;
import com.airfreight.entity.MailMessage;
import com.airfreight.repository.MailAccountRepository;
import com.airfreight.service.MailClassificationService;
import com.airfreight.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 邮件客户端 - POP3/SMTP 收发邮件
 */
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "邮件客户端", description = "POP3收信 + SMTP发信 + 账号管理 + 邮件分类")
@CrossOrigin(origins = "*")
public class MailController {

    private final MailService mailService;
    private final MailAccountRepository mailAccountRepository;
    private final MailClassificationService mailClassificationService;

    // ===== 账号管理 =====

    @PostMapping("/accounts")
    @Operation(summary = "添加邮箱账号")
    public ResponseEntity<MailAccount> addAccount(@RequestBody MailAccount account) {
        return ResponseEntity.ok(mailService.saveAccount(account));
    }

    @PutMapping("/accounts/{id}")
    @Operation(summary = "更新邮箱账号")
    public ResponseEntity<MailAccount> updateAccount(@PathVariable Long id, @RequestBody MailAccount account) {
        account.setId(id);
        return ResponseEntity.ok(mailService.saveAccount(account));
    }

    @GetMapping("/accounts")
    @Operation(summary = "查询所有邮箱账号")
    public ResponseEntity<List<MailAccount>> getAllAccounts() {
        return ResponseEntity.ok(mailService.getAllAccounts());
    }

    @DeleteMapping("/accounts/{id}")
    @Operation(summary = "删除邮箱账号")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        mailService.deleteAccount(id);
        return ResponseEntity.ok().build();
    }

    // ===== 账号验证 =====

    @PostMapping("/accounts/test-connection")
    @Operation(summary = "测试邮箱账号连接", description = "验证POP3/SMTP配置是否正确")
    public ResponseEntity<?> testAccountConnection(@RequestBody MailAccount account) {
        return ResponseEntity.ok(mailService.testAccountConnection(account));
    }

    @PostMapping("/accounts/{id}/test")
    @Operation(summary = "测试已保存账号连接", description = "验证已保存账号的POP3/SMTP连接")
    public ResponseEntity<?> testSavedAccount(@PathVariable Long id) {
        MailAccount account = mailAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("邮件账号不存在: " + id));
        return ResponseEntity.ok(mailService.testAccountConnection(account));
    }

    @PostMapping("/accounts/test-send")
    @Operation(summary = "测试邮箱账号发信", description = "使用未保存或编辑中的账号配置发送一封测试邮件")
    public ResponseEntity<?> testAccountSend(@RequestBody MailAccount account, @RequestParam String to) {
        return ResponseEntity.ok(mailService.testSendMail(account, to));
    }

    @PostMapping("/accounts/{id}/test-send")
    @Operation(summary = "测试已保存账号发信", description = "使用已保存账号发送一封测试邮件")
    public ResponseEntity<?> testSavedAccountSend(@PathVariable Long id, @RequestParam String to) {
        return ResponseEntity.ok(mailService.testSavedAccountSend(id, to));
    }

    // ===== POP3 收信 =====

    @PostMapping("/receive/{accountId}")
    @Operation(summary = "收取邮件（异步后台同步）", description = "首次同步最多maxMessages封（默认1000），后续只同步最新邮件")
    public ResponseEntity<Map<String, Object>> receiveMails(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "1000") int maxMessages) {
        mailService.receiveMailsAsync(accountId, maxMessages);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", "processing");
        result.put("message", "正在后台同步邮件，请稍后刷新列表");
        result.put("maxMessages", maxMessages);
        return ResponseEntity.ok(result);
    }

    // ===== SMTP 发信 =====

    @PostMapping("/send/{accountId}")
    @Operation(summary = "发送邮件（SMTP）")
    public ResponseEntity<MailMessage> sendMail(
            @PathVariable Long accountId,
            @RequestParam String to,
            @RequestParam(required = false) String cc,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(defaultValue = "NORMAL") String priority) {
        return ResponseEntity.ok(mailService.sendMail(accountId, to, cc, subject, content, priority));
    }

    // ===== 邮件查询 =====

    @GetMapping("/messages/{accountId}")
    @Operation(summary = "查询账号下的邮件")
    public ResponseEntity<List<MailMessage>> getMails(@PathVariable Long accountId) {
        return ResponseEntity.ok(mailService.getMailsByAccount(accountId));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索邮件")
    public ResponseEntity<List<MailMessage>> searchMails(@RequestParam String keyword) {
        return ResponseEntity.ok(mailService.searchMails(keyword));
    }

    // ===== 邮件分类 =====

    @PostMapping("/classify/{mailId}")
    @Operation(summary = "对单封邮件进行分类", description = "使用指定方法对单封邮件进行分类：RULE(规则)/LLM(AI)/HYBRID(混合)")
    public ResponseEntity<MailMessage> classifyMail(
            @PathVariable Long mailId,
            @RequestParam(defaultValue = "HYBRID") String method) {
        return ResponseEntity.ok(mailClassificationService.classifyMail(mailId, method));
    }

    @PostMapping("/classify/batch")
    @Operation(summary = "批量邮件分类", description = "对多封邮件进行批量分类")
    public ResponseEntity<List<MailMessage>> classifyMailsBatch(
            @RequestParam List<Long> mailIds,
            @RequestParam(defaultValue = "HYBRID") String method) {
        return ResponseEntity.ok(mailClassificationService.classifyMailsBatch(mailIds, method));
    }

    @PostMapping("/classify/account/{accountId}")
    @Operation(summary = "对账号下所有未分类邮件进行分类", description = "自动分类该账号下所有未分类的邮件")
    public ResponseEntity<Map<String, Object>> classifyAccountMails(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "HYBRID") String method) {
        int count = mailClassificationService.classifyAccountMails(accountId, method);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("successCount", count);
        result.put("method", method);
        result.put("message", "分类完成，成功处理 " + count + " 封邮件");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/classify/{mailId}/manual")
    @Operation(summary = "手动设置邮件分类", description = "用户手动指定邮件的分类和标签")
    public ResponseEntity<MailMessage> manualClassify(
            @PathVariable Long mailId,
            @RequestParam String category,
            @RequestParam(required = false) String tags) {
        return ResponseEntity.ok(mailClassificationService.manualClassify(mailId, category, tags));
    }
}
