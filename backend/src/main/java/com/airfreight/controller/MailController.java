package com.airfreight.controller;

import com.airfreight.entity.MailAccount;
import com.airfreight.entity.MailMessage;
import com.airfreight.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邮件客户端 - POP3/SMTP 收发邮件
 */
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "邮件客户端", description = "POP3收信 + SMTP发信 + 账号管理")
@CrossOrigin(origins = "*")
public class MailController {

    private final MailService mailService;

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

    // ===== POP3 收信 =====

    @PostMapping("/receive/{accountId}")
    @Operation(summary = "收取邮件（POP3）")
    public ResponseEntity<List<MailMessage>> receiveMails(@PathVariable Long accountId) {
        return ResponseEntity.ok(mailService.receiveMails(accountId));
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
}