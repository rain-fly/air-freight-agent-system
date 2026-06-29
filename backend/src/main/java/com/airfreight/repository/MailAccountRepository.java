package com.airfreight.repository;

import com.airfreight.entity.MailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {
}