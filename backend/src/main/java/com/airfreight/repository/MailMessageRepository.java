package com.airfreight.repository;

import com.airfreight.entity.MailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MailMessageRepository extends JpaRepository<MailMessage, Long> {
    List<MailMessage> findByAccountIdOrderByReceivedDateDesc(Long accountId);
    List<MailMessage> findByAccountIdAndDirection(Long accountId, String direction);
    List<MailMessage> findBySubjectContainingOrContentContaining(String keyword1, String keyword2);
    void deleteByAccountId(Long accountId);

    @Query("select m.messageId from MailMessage m where m.account.id = :accountId and m.messageId is not null")
    List<String> findMessageIdsByAccountId(@Param("accountId") Long accountId);
}
