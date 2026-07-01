package com.airfreight.service;

import com.airfreight.entity.MailAccount;
import com.airfreight.entity.MailMessage;
import com.airfreight.repository.MailAccountRepository;
import com.airfreight.repository.MailMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SearchTerm;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 邮件客户端服务 - IMAP/POP3收信 + SMTP发信
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailAccountRepository accountRepository;
    private final MailMessageRepository messageRepository;

    // ===== 账号管理 =====

    public MailAccount saveAccount(MailAccount account) {
        if (account.getSmtpSsl() == null) account.setSmtpSsl(true);
        if (account.getSmtpPort() == null) account.setSmtpPort(465);
        if (account.getReceiveProtocol() == null) account.setReceiveProtocol("IMAP");
        if (account.getStatus() == null) account.setStatus("ACTIVE");

        // IMAP默认值
        if ("IMAP".equals(account.getReceiveProtocol())) {
            if (account.getImapSsl() == null) account.setImapSsl(true);
            if (account.getImapPort() == null) account.setImapPort(993);
        }

        // POP3默认值
        if ("POP3".equals(account.getReceiveProtocol())) {
            if (account.getPop3Ssl() == null) account.setPop3Ssl(true);
            if (account.getPop3Port() == null) account.setPop3Port(995);
        }

        return accountRepository.save(account);
    }

    public List<MailAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("邮件账号不存在: " + id);
        }
        messageRepository.deleteByAccountId(id);
        accountRepository.deleteById(id);
    }

    // ===== 账号验证 =====

    /**
     * 测试邮箱账号连接
     * @param account 邮箱账号
     * @return 验证结果，包含是否成功和详细错误信息
     */
    public Map<String, Object> testAccountConnection(MailAccount account) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        Map<String, Object> receiveResult = testReceiveConnection(account);
        if (Boolean.TRUE.equals(receiveResult.get("success"))) {
            String protocol = "IMAP".equals(account.getReceiveProtocol()) ? "IMAP" : "POP3";
            successes.add(protocol + "连接成功");
        } else {
            errors.add(receiveResult.getOrDefault("protocol", "收信") + "连接失败: " + receiveResult.get("error"));
        }

        Map<String, Object> smtpResult = testSmtpConnection(account);
        if (Boolean.TRUE.equals(smtpResult.get("success"))) {
            successes.add("SMTP连接成功");
        } else {
            errors.add("SMTP连接失败: " + smtpResult.get("error"));
        }

        boolean isValid = errors.isEmpty();
        result.put("valid", isValid);
        result.put("successes", successes);
        result.put("errors", errors);
        result.put("message", isValid ? "邮箱账号配置验证成功" : "邮箱账号配置验证失败，请检查配置");

        return result;
    }

    public Map<String, Object> testSendMail(MailAccount account, String to) {
        Map<String, Object> result = new HashMap<>();
        if (isBlank(to)) {
            result.put("success", false);
            result.put("message", "请填写测试收件人");
            return result;
        }

        String subject = "邮箱发信测试";
        String content = "这是一封来自空运代理系统的邮箱发信测试邮件。\n\n"
                + "发信账号: " + safe(account.getEmailAddress()) + "\n"
                + "测试时间: " + LocalDateTime.now();

        try {
            sendMailWithAccount(account, to, null, subject, content, "NORMAL", false);
            result.put("success", true);
            result.put("message", "测试邮件已发送到 " + to);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试发信失败: " + simplifyErrorMessage(e.getMessage()));
        }
        return result;
    }

    public Map<String, Object> testSavedAccountSend(Long id, String to) {
        MailAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("邮件账号不存在: " + id));
        return testSendMail(account, to);
    }

    /**
     * 测试收信连接（IMAP或POP3）
     */
    private Map<String, Object> testReceiveConnection(MailAccount account) {
        if ("IMAP".equals(account.getReceiveProtocol())) {
            return testImapConnection(account);
        } else {
            return testPop3Connection(account);
        }
    }

    /**
     * 测试IMAP连接
     */
    private Map<String, Object> testImapConnection(MailAccount account) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocol", "IMAP");

        try {
            Properties props = new Properties();
            props.put("mail.imap.host", account.getImapHost());
            props.put("mail.imap.port", account.getImapPort());
            props.put("mail.imap.ssl.enable", account.getImapSsl());
            props.put("mail.imap.auth", "true");
            props.put("mail.imap.connectiontimeout", "5000");
            props.put("mail.imap.timeout", "5000");
            if (Boolean.TRUE.equals(account.getImapSsl())) {
                props.put("mail.imap.ssl.trust", account.getImapHost());
            }

            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            store.connect(account.getImapHost(), account.getImapPort(),
                    account.getEmailAddress(), account.getPassword());
            store.close();

            result.put("success", true);
            log.info("IMAP连接测试成功: {}", account.getEmailAddress());
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("IMAP连接测试失败: {} - {}", account.getEmailAddress(), errorMsg);
            result.put("success", false);
            result.put("error", simplifyErrorMessage(errorMsg));
        }

        return result;
    }

    /**
     * 测试POP3连接
     */
    private Map<String, Object> testPop3Connection(MailAccount account) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocol", "POP3");

        try {
            Properties props = new Properties();
            props.put("mail.pop3.host", account.getPop3Host());
            props.put("mail.pop3.port", account.getPop3Port());
            props.put("mail.pop3.ssl.enable", account.getPop3Ssl());
            props.put("mail.pop3.auth", "true");
            props.put("mail.pop3.connectiontimeout", "5000");
            props.put("mail.pop3.timeout", "5000");

            Session session = Session.getInstance(props);
            Store store = session.getStore("pop3");
            store.connect(account.getPop3Host(), account.getPop3Port(),
                    account.getEmailAddress(), account.getPassword());
            store.close();

            result.put("success", true);
            log.info("POP3连接测试成功: {}", account.getEmailAddress());
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("POP3连接测试失败: {} - {}", account.getEmailAddress(), errorMsg);
            result.put("success", false);
            result.put("error", simplifyErrorMessage(errorMsg));
        }

        return result;
    }

    /**
     * 测试SMTP连接
     */
    private Map<String, Object> testSmtpConnection(MailAccount account) {
        Map<String, Object> result = new HashMap<>();

        try {
            Transport transport = createSmtpSession(account).getTransport("smtp");
            transport.connect();
            transport.close();

            result.put("success", true);
            log.info("SMTP连接测试成功: {}", account.getEmailAddress());
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("SMTP连接测试失败: {} - {}", account.getEmailAddress(), errorMsg);
            result.put("success", false);
            result.put("error", simplifyErrorMessage(errorMsg));
        }

        return result;
    }

    /**
     * 简化错误信息，使其对用户更友好
     */
    private String simplifyErrorMessage(String errorMsg) {
        if (errorMsg == null) return "未知错误";

        if (errorMsg.contains("Connection refused") || errorMsg.contains("connect timed out")) {
            return "连接被拒绝或超时，请检查服务器地址和端口";
        } else if (errorMsg.contains("Authentication failed") || errorMsg.contains("535")
                || errorMsg.contains("LOGIN") && errorMsg.contains("disabled")) {
            return "认证失败，请检查邮箱地址和密码/授权码";
        } else if (errorMsg.contains("UnknownHostException")) {
            return "无法解析服务器地址，请检查服务器地址是否正确";
        } else if (errorMsg.contains("SSL handshake")) {
            return "SSL握手失败，请检查SSL配置是否正确";
        } else if (errorMsg.contains("No route to host")) {
            return "无法连接到主机，请检查网络连接";
        }

        return errorMsg.length() > 100 ? errorMsg.substring(0, 100) + "..." : errorMsg;
    }

    // ===== 收信（IMAP/POP3自动选择） =====

    /**
     * 后台异步收取邮件（首次同步最多maxMessages封，后续只同步最新邮件）
     */
    @Async("mailSyncExecutor")
    public void receiveMailsAsync(Long accountId, int maxMessages) {
        try {
            log.info("后台邮件同步开始: accountId={}, maxMessages={}", accountId, maxMessages);
            MailAccount account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("邮件账号不存在"));

            List<MailMessage> received;
            if ("IMAP".equals(account.getReceiveProtocol())) {
                received = receiveMailsViaImap(account, maxMessages);
            } else {
                received = receiveMailsViaPop3(account, maxMessages);
            }
            log.info("后台邮件同步完成: accountId={}, 新增{}封", accountId, received.size());
        } catch (Exception e) {
            log.error("后台邮件同步失败: accountId={}, error={}", accountId, e.getMessage(), e);
        }
    }

    @Transactional
    public List<MailMessage> receiveMails(Long accountId, int maxMessages) {
        MailAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("邮件账号不存在"));

        if ("IMAP".equals(account.getReceiveProtocol())) {
            return receiveMailsViaImap(account, maxMessages);
        } else {
            return receiveMailsViaPop3(account, maxMessages);
        }
    }

    @Transactional
    public List<MailMessage> receiveMails(Long accountId) {
        return receiveMails(accountId, 1000);
    }

    /**
     * IMAP收信 - 首次全量同步（最多maxMessages封），后续增量同步
     */
    @Transactional
    public List<MailMessage> receiveMailsViaImap(MailAccount account, int maxMessages) {
        List<MailMessage> receivedMessages = new ArrayList<>();
        Set<String> syncedMessageIds = new HashSet<>(
                messageRepository.findMessageIdsByAccountId(account.getId()));

        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.put("mail.imap.host", account.getImapHost());
            props.put("mail.imap.port", account.getImapPort());
            props.put("mail.imap.ssl.enable", account.getImapSsl());
            props.put("mail.imap.auth", "true");
            props.put("mail.imap.connectiontimeout", "10000");
            props.put("mail.imap.timeout", "10000");
            if (Boolean.TRUE.equals(account.getImapSsl())) {
                props.put("mail.imap.ssl.trust", account.getImapHost());
            }

            Session session = Session.getInstance(props);
            store = session.getStore("imap");
            store.connect(account.getImapHost(), account.getImapPort(),
                    account.getEmailAddress(), account.getPassword());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] msgs;
            boolean isIncremental = account.getLastCheckAt() != null;

            if (isIncremental) {
                // 增量同步：收取上次检查之后发送的邮件 + 所有未读邮件
                java.util.Date lastCheckDate = java.util.Date.from(
                        account.getLastCheckAt().atZone(ZoneId.systemDefault()).toInstant());
                SearchTerm dateTerm = new SentDateTerm(ComparisonTerm.GE, lastCheckDate);
                SearchTerm unseenTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                msgs = inbox.search(new OrTerm(dateTerm, unseenTerm));
                log.info("IMAP增量同步: {} 自{}后共{}封（含未读）",
                        account.getEmailAddress(), account.getLastCheckAt(), msgs.length);
            } else {
                // 首次同步：收取最近 maxMessages 封邮件
                int total = inbox.getMessageCount();
                int start = Math.max(1, total - maxMessages + 1);
                msgs = inbox.getMessages(start, total);
                log.info("IMAP首次同步: {} 共{}封（最近{}封）",
                        account.getEmailAddress(), msgs.length, maxMessages);
            }

            // 从最新到最旧处理
            for (int i = msgs.length - 1; i >= 0; i--) {
                Message msg = msgs[i];
                try {
                    String messageId = resolveMessageId(msg, inbox);
                    if (syncedMessageIds.contains(messageId)) {
                        continue;
                    }

                    MailMessage mail = convertToMailMessage(msg, account, messageId);
                    mail.setDirection("INBOX");
                    MailMessage saved = messageRepository.save(mail);
                    syncedMessageIds.add(messageId);
                    receivedMessages.add(saved);
                } catch (Exception e) {
                    log.warn("解析邮件失败: {}", e.getMessage());
                }
            }

            account.setLastCheckAt(LocalDateTime.now());
            accountRepository.save(account);

        } catch (Exception e) {
            log.error("IMAP收信失败: {}", e.getMessage());
            throw new RuntimeException("收信失败: " + e.getMessage());
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) inbox.close(false);
            } catch (Exception ignored) {}
            try {
                if (store != null) store.close();
            } catch (Exception ignored) {}
        }

        return receivedMessages;
    }

    @Transactional
    public List<MailMessage> receiveMailsViaImap(MailAccount account) {
        return receiveMailsViaImap(account, 1000);
    }

    /**
     * POP3收信 - 首次全量同步（最多maxMessages封），后续增量同步
     */
    @Transactional
    public List<MailMessage> receiveMailsViaPop3(MailAccount account, int maxMessages) {
        List<MailMessage> receivedMessages = new ArrayList<>();
        Set<String> syncedMessageIds = new HashSet<>(
                messageRepository.findMessageIdsByAccountId(account.getId()));

        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.put("mail.pop3.host", account.getPop3Host());
            props.put("mail.pop3.port", account.getPop3Port());
            props.put("mail.pop3.ssl.enable", account.getPop3Ssl());
            props.put("mail.pop3.auth", "true");
            props.put("mail.pop3.connectiontimeout", "10000");
            props.put("mail.pop3.timeout", "10000");

            Session session = Session.getInstance(props);
            store = session.getStore("pop3");
            store.connect(account.getPop3Host(), account.getPop3Port(),
                    account.getEmailAddress(), account.getPassword());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] allMsgs = inbox.getMessages();
            boolean isIncremental = account.getLastCheckAt() != null;
            LocalDateTime lastCheckTime = account.getLastCheckAt();

            log.info("POP3收信: {} 共{}封待检查（{}）",
                    account.getEmailAddress(), allMsgs.length,
                    isIncremental ? "增量模式" : "首次全量模式");

            int processedCount = 0;
            int skippedOlderCount = 0;

            // 从最新到最旧遍历
            for (int i = allMsgs.length - 1; i >= 0; i--) {
                Message msg = allMsgs[i];
                try {
                    String messageId = resolveMessageId(msg, inbox);
                    if (syncedMessageIds.contains(messageId)) {
                        // 已同步过，对于增量模式可以提前终止（因为按时间倒序，后面都是更旧的）
                        if (isIncremental) {
                            log.debug("POP3增量同步: 遇到已同步邮件，提前终止");
                            break;
                        }
                        continue;
                    }

                    // 增量模式：检查邮件时间是否早于上次同步时间
                    if (isIncremental && lastCheckTime != null) {
                        java.util.Date sentDate = msg.getSentDate();
                        if (sentDate != null) {
                            LocalDateTime msgTime = sentDate.toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
                            if (msgTime.isBefore(lastCheckTime)) {
                                skippedOlderCount++;
                                if (skippedOlderCount >= 3) {
                                    // 连续3封都早于上次同步时间，说明后面的更旧，可以终止
                                    log.debug("POP3增量同步: 连续{}封早于上次同步时间，提前终止", skippedOlderCount);
                                    break;
                                }
                                continue;
                            }
                        }
                    }

                    MailMessage mail = convertToMailMessage(msg, account, messageId);
                    mail.setDirection("INBOX");
                    MailMessage saved = messageRepository.save(mail);
                    syncedMessageIds.add(messageId);
                    receivedMessages.add(saved);
                    processedCount++;

                    // 首次同步：达到上限则停止
                    if (!isIncremental && processedCount >= maxMessages) {
                        log.info("POP3首次同步: 已达上限{}封，停止", maxMessages);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("POP3解析邮件失败: {}", e.getMessage());
                }
            }

            log.info("POP3收信完成: {} 新增{}封", account.getEmailAddress(), receivedMessages.size());

            inbox.close(false);
            store.close();

            account.setLastCheckAt(LocalDateTime.now());
            accountRepository.save(account);

        } catch (Exception e) {
            log.error("POP3收信失败: {}", e.getMessage());
            throw new RuntimeException("收信失败: " + e.getMessage());
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) inbox.close(false);
            } catch (Exception ignored) {}
            try {
                if (store != null) store.close();
            } catch (Exception ignored) {}
        }

        return receivedMessages;
    }

    @Transactional
    public List<MailMessage> receiveMailsViaPop3(MailAccount account) {
        return receiveMailsViaPop3(account, 1000);
    }

    // ===== SMTP 发信 =====

    public MailMessage sendMail(Long accountId, String to, String cc, String subject,
                                String content, String priority) {
        MailAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("邮件账号不存在"));
        return sendMailWithAccount(account, to, cc, subject, content, priority, true);
    }

    private MailMessage sendMailWithAccount(MailAccount account, String to, String cc, String subject,
                                            String content, String priority, boolean saveSent) {
        try {
            Session session = createSmtpSession(account);

            MimeMessage mimeMsg = new MimeMessage(session);
            mimeMsg.setFrom(new InternetAddress(account.getEmailAddress()));
            mimeMsg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            if (!isBlank(cc)) {
                mimeMsg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            }

            mimeMsg.setSubject(subject, "UTF-8");
            mimeMsg.setText(content, "UTF-8");
            mimeMsg.setSentDate(new Date());

            if ("HIGH".equals(priority)) {
                mimeMsg.setHeader("X-Priority", "1");
                mimeMsg.setHeader("Importance", "high");
            }

            mimeMsg.saveChanges();
            Transport.send(mimeMsg);

            log.info("SMTP发信成功: {} -> {}", account.getEmailAddress(), to);

            if (!saveSent) {
                return null;
            }

            MailMessage saved = new MailMessage();
            saved.setAccount(account);
            String rawMessageId = !isBlank(mimeMsg.getMessageID()) ? mimeMsg.getMessageID() : UUID.randomUUID().toString();
            String normalizedMessageId = normalizeMessageId(rawMessageId);
            // 进一步确保长度不超限（>255则sha256截断）
            if (normalizedMessageId.length() > 63) {
                normalizedMessageId = "X:" + sha256(normalizedMessageId).substring(0, 62);
            }
            saved.setMessageId(normalizedMessageId);
            saved.setDirection("SENT");
            saved.setFromAddress(account.getEmailAddress());
            saved.setToAddress(to);
            saved.setCcAddress(cc);
            saved.setSubject(subject);
            saved.setContent(content);
            saved.setPriority(priority != null ? priority : "NORMAL");
            saved.setSentDate(LocalDateTime.now());
            saved.setReceivedDate(LocalDateTime.now());
            saved.setIsRead(true);
            saved.setIsStarred(false);
            saved.setHasAttachments(false);

            return messageRepository.save(saved);

        } catch (Exception e) {
            log.error("SMTP发信失败: {}", e.getMessage());
            throw new RuntimeException("发信失败: " + e.getMessage());
        }
    }

    private Session createSmtpSession(MailAccount account) {
        Properties props = new Properties();
        props.put("mail.smtp.host", account.getSmtpHost());
        props.put("mail.smtp.port", account.getSmtpPort());
        props.put("mail.smtp.ssl.enable", account.getSmtpSsl());
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.ssl.trust", account.getSmtpHost());

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(account.getEmailAddress(), account.getPassword());
            }
        });
    }

    // ===== 邮件查询 =====

    public List<MailMessage> getMailsByAccount(Long accountId) {
        return messageRepository.findByAccountIdOrderByReceivedDateDesc(accountId);
    }

    public List<MailMessage> searchMails(String keyword) {
        return messageRepository.findBySubjectContainingOrContentContaining(keyword, keyword);
    }

    // ===== 私有方法 =====

    private MailMessage convertToMailMessage(Message msg, MailAccount account, String messageId) throws Exception {
        MailMessage mail = new MailMessage();
        mail.setAccount(account);
        mail.setMessageId(messageId);

        Address[] from = msg.getFrom();
        if (from != null && from.length > 0) {
            InternetAddress addr = (InternetAddress) from[0];
            mail.setFromAddress(truncate(addr.getAddress(), 200));
            mail.setFromPersonal(truncate(addr.getPersonal(), 200));
        }

        mail.setToAddress(truncate(formatAddresses(msg.getRecipients(Message.RecipientType.TO)), 500));
        mail.setCcAddress(truncate(formatAddresses(msg.getRecipients(Message.RecipientType.CC)), 500));
        mail.setSubject(truncate(msg.getSubject(), 500));

        Object content = msg.getContent();
        if (content instanceof String) {
            mail.setContent((String) content);
            mail.setHasAttachments(false);
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            // 提取正文和附件信息
            StringBuilder textContent = new StringBuilder();
            StringBuilder htmlContent = new StringBuilder();
            java.util.List<String> attachmentNames = new java.util.ArrayList<>();
            extractFromMultipart(multipart, textContent, htmlContent, attachmentNames);

            if (textContent.length() > 0) {
                mail.setContent(textContent.toString());
            }
            if (htmlContent.length() > 0) {
                // 截断 HTML 内容，只保留前 65535 字符（TEXT 字段限制）
                String html = htmlContent.toString();
                mail.setContentHtml(html.length() > 65535 ? html.substring(0, 65535) : html);
            }
            // 如果有纯文本内容就去掉HTML，否则保留文本
            if (textContent.length() == 0 && htmlContent.length() > 0) {
                // 从HTML中提取纯文本
                String stripped = htmlContent.toString().replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                mail.setContent(stripped);
            }

            boolean hasAttachments = !attachmentNames.isEmpty();
            mail.setHasAttachments(hasAttachments);
            if (hasAttachments) {
                mail.setAttachmentNames(String.join(", ", attachmentNames));
            }
        }

        Date sentDate = msg.getSentDate();
        if (sentDate != null) {
            mail.setSentDate(sentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        mail.setReceivedDate(LocalDateTime.now());

        mail.setIsRead(msg.isSet(Flags.Flag.SEEN));
        mail.setIsStarred(false);

        String[] priorityHeaders = msg.getHeader("X-Priority");
        if (priorityHeaders != null) {
            mail.setPriority("1".equals(priorityHeaders[0]) ? "HIGH" : "NORMAL");
        } else {
            mail.setPriority("NORMAL");
        }

        return mail;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) {
            return value;
        }
        log.warn("字段超长截断: {} -> {}", maxLength, value.substring(0, 50) + "...");
        return value.substring(0, maxLength);
    }

    private String resolveMessageId(Message msg, Folder folder) throws Exception {
        String[] headers = msg.getHeader("Message-ID");
        if (headers != null && headers.length > 0 && !isBlank(headers[0])) {
            String id = headers[0].trim();
            // 标准化：去除首尾尖括号
            if (id.startsWith("<") && id.endsWith(">")) {
                id = id.substring(1, id.length() - 1);
            }
            return truncateMessageId(id);
        }

        String uid = readFolderUid(folder, msg);
        if (!isBlank(uid)) {
            return "U:" + truncateString(uid, 60);
        }

        String signature = safe(msg.getSubject()) + "|"
                + dateMillis(msg.getSentDate()) + "|"
                + safe(formatAddresses(msg.getFrom())) + "|"
                + msg.getSize();
        return truncateMessageId("H:" + sha256(signature));
    }

    /**
     * 强制保证messageId长度不超63字符（数据库字段varchar(64)）
     */
    private String truncateMessageId(String value) throws Exception {
        if (value == null) return "";
        value = value.trim();
        if (value.length() <= 63) {
            return value;
        }
        // 超长则哈希
        return "X:" + sha256(value).substring(0, 62);
    }

    private String truncateString(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String normalizeMessageId(String messageId) throws Exception {
        String normalized = messageId == null ? "" : messageId.trim();
        // messageId数据库字段为varchar(255)，长度限制为240，留点余量
        if (normalized.length() <= 240) {
            return normalized;
        }
        return "HASH:" + sha256(normalized);
    }

    private String readFolderUid(Folder folder, Message msg) {
        try {
            java.lang.reflect.Method getUid = folder.getClass().getMethod("getUID", Message.class);
            Object uid = getUid.invoke(folder, msg);
            return uid == null ? null : String.valueOf(uid);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatAddresses(Address[] addresses) {
        if (addresses == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Address addr : addresses) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(addr.toString());
        }
        return sb.toString();
    }

    /**
     * 解析 Multipart 内容：提取纯文本、HTML正文和附件名称
     */
    private void extractFromMultipart(Multipart multipart, StringBuilder textContent,
                                      StringBuilder htmlContent, java.util.List<String> attachmentNames) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String disposition = part.getDisposition();
            String fileName = part.getFileName();

            // 判断是否为真正的附件：有 disposition=attachment 或有文件名
            boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                    || (fileName != null && !fileName.isEmpty());

            if (isAttachment) {
                if (fileName != null && !fileName.isEmpty()) {
                    // 解码 RFC 2047 编码的文件名
                    try {
                        fileName = javax.mail.internet.MimeUtility.decodeText(fileName);
                    } catch (Exception ignored) {}
                    attachmentNames.add(fileName);
                } else {
                    attachmentNames.add("unnamed-" + (attachmentNames.size() + 1));
                }
                continue;  // 跳过附件内容，不提取正文
            }

            // 处理正文部分
            try {
                if (part.isMimeType("text/plain")) {
                    textContent.append((String) part.getContent());
                } else if (part.isMimeType("text/html")) {
                    htmlContent.append((String) part.getContent());
                } else if (part.isMimeType("multipart/*")) {
                    extractFromMultipart((Multipart) part.getContent(), textContent, htmlContent, attachmentNames);
                } else if (part.isMimeType("message/rfc822")) {
                    // 嵌套邮件，作为附件处理
                    String nestedName = fileName != null ? fileName : "forwarded-message.eml";
                    attachmentNames.add(nestedName);
                }
            } catch (Exception e) {
                log.warn("解析邮件部分内容失败: {}", e.getMessage());
            }
        }
    }

    private long dateMillis(Date date) {
        return date == null ? 0L : date.getTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
