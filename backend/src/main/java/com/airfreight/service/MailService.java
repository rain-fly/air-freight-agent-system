package com.airfreight.service;

import com.airfreight.entity.MailAccount;
import com.airfreight.entity.MailMessage;
import com.airfreight.repository.MailAccountRepository;
import com.airfreight.repository.MailMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 邮件客户端服务 - POP3收信 + SMTP发信
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
        if (account.getPop3Ssl() == null) account.setPop3Ssl(true);
        if (account.getSmtpPort() == null) account.setSmtpPort(465);
        if (account.getPop3Port() == null) account.setPop3Port(995);
        if (account.getStatus() == null) account.setStatus("ACTIVE");
        return accountRepository.save(account);
    }

    public List<MailAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    // ===== POP3 收信 =====

    @Transactional
    public List<MailMessage> receiveMails(Long accountId) {
        MailAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("邮件账号不存在"));

        List<MailMessage> messages = new ArrayList<>();

        try {
            // POP3配置
            Properties props = new Properties();
            props.put("mail.pop3.host", account.getPop3Host());
            props.put("mail.pop3.port", account.getPop3Port());
            props.put("mail.pop3.ssl.enable", account.getPop3Ssl());
            props.put("mail.pop3.auth", "true");
            props.put("mail.pop3.connectiontimeout", "10000");
            props.put("mail.pop3.timeout", "10000");

            Session session = Session.getInstance(props);
            Store store = session.getStore("pop3");
            store.connect(account.getPop3Host(), account.getPop3Port(),
                    account.getEmailAddress(), account.getPassword());

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // 只获取未读邮件
            Message[] msgs = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            if (msgs.length == 0) {
                msgs = inbox.getMessages();
            }

            log.info("POP3收信: {} 共{}封", account.getEmailAddress(), msgs.length);

            for (Message msg : msgs) {
                try {
                    MailMessage mail = convertToMailMessage(msg, account);
                    mail.setDirection("INBOX");
                    MailMessage saved = messageRepository.save(mail);
                    messages.add(saved);
                } catch (Exception e) {
                    log.warn("解析邮件失败: {}", e.getMessage());
                }
            }

            inbox.close(false);
            store.close();

            account.setLastCheckAt(LocalDateTime.now());
            accountRepository.save(account);

        } catch (Exception e) {
            log.error("POP3收信失败: {}", e.getMessage());
            throw new RuntimeException("收信失败: " + e.getMessage());
        }

        return messages;
    }

    // ===== SMTP 发信 =====

    public MailMessage sendMail(Long accountId, String to, String cc, String subject,
                                String content, String priority) {
        MailAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("邮件账号不存在"));

        try {
            // SMTP配置
            Properties props = new Properties();
            props.put("mail.smtp.host", account.getSmtpHost());
            props.put("mail.smtp.port", account.getSmtpPort());
            props.put("mail.smtp.ssl.enable", account.getSmtpSsl());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(account.getEmailAddress(), account.getPassword());
                }
            });

            // 构建邮件
            MimeMessage mimeMsg = new MimeMessage(session);
            mimeMsg.setFrom(new InternetAddress(account.getEmailAddress()));
            mimeMsg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            if (cc != null && !cc.isEmpty()) {
                mimeMsg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            }

            mimeMsg.setSubject(subject, "UTF-8");
            mimeMsg.setText(content, "UTF-8");
            mimeMsg.setSentDate(new Date());

            // 优先级
            if ("HIGH".equals(priority)) {
                mimeMsg.setHeader("X-Priority", "1");
                mimeMsg.setHeader("Importance", "high");
            }

            Transport.send(mimeMsg);

            log.info("SMTP发信成功: {} -> {}", account.getEmailAddress(), to);

            // 保存到已发送
            MailMessage saved = new MailMessage();
            saved.setAccount(account);
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

            return messageRepository.save(saved);

        } catch (Exception e) {
            log.error("SMTP发信失败: {}", e.getMessage());
            throw new RuntimeException("发信失败: " + e.getMessage());
        }
    }

    // ===== 邮件查询 =====

    public List<MailMessage> getMailsByAccount(Long accountId) {
        return messageRepository.findByAccountIdOrderByReceivedDateDesc(accountId);
    }

    public List<MailMessage> searchMails(String keyword) {
        return messageRepository.findBySubjectContainingOrContentContaining(keyword, keyword);
    }

    // ===== 私有方法 =====

    private MailMessage convertToMailMessage(Message msg, MailAccount account) throws Exception {
        MailMessage mail = new MailMessage();
        mail.setAccount(account);

        // 邮件ID
        mail.setMessageId(msg.getHeader("Message-ID") != null ? msg.getHeader("Message-ID")[0] : UUID.randomUUID().toString());

        // 发件人
        Address[] from = msg.getFrom();
        if (from != null && from.length > 0) {
            InternetAddress addr = (InternetAddress) from[0];
            mail.setFromAddress(addr.getAddress());
            mail.setFromPersonal(addr.getPersonal());
        }

        // 收件人
        mail.setToAddress(formatAddresses(msg.getRecipients(Message.RecipientType.TO)));
        mail.setCcAddress(formatAddresses(msg.getRecipients(Message.RecipientType.CC)));

        // 主题
        mail.setSubject(msg.getSubject());

        // 正文
        Object content = msg.getContent();
        if (content instanceof String) {
            mail.setContent((String) content);
        } else if (content instanceof Multipart) {
            StringBuilder textContent = new StringBuilder();
            extractTextFromMultipart((Multipart) content, textContent);
            mail.setContent(textContent.toString());
        }

        // 日期
        Date sentDate = msg.getSentDate();
        if (sentDate != null) {
            mail.setSentDate(sentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        mail.setReceivedDate(LocalDateTime.now());

        // 标志
        mail.setIsRead(msg.isSet(Flags.Flag.SEEN));
        mail.setIsStarred(false);
        mail.setHasAttachments(msg.getContentType() != null && msg.getContentType().contains("multipart"));

        // 优先级
        String[] priorityHeaders = msg.getHeader("X-Priority");
        if (priorityHeaders != null) {
            mail.setPriority("1".equals(priorityHeaders[0]) ? "HIGH" : "NORMAL");
        } else {
            mail.setPriority("NORMAL");
        }

        return mail;
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

    private void extractTextFromMultipart(Multipart multipart, StringBuilder sb) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                sb.append((String) part.getContent());
            } else if (part.isMimeType("multipart/*")) {
                extractTextFromMultipart((Multipart) part.getContent(), sb);
            }
        }
    }
}