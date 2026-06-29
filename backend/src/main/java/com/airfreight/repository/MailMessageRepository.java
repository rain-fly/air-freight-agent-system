package com.airfreight.repository;

import com.airfreight.entity.MailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MailMessageRepository extends JpaRepository<MailMessage, Long> {
    List<MailMessage> findByAccountIdOrderByReceivedDateDesc(Long accountId);
    List<MailMessage> findByAccountIdAndDirection(Long accountId, String direction);
    List<MailMessage> findBySubjectContainingOrContentContaining(String keyword1, String keyword2);
}