package com.airfreight.repository;

import com.airfreight.entity.LlmConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LlmConfigRepository extends JpaRepository<LlmConfig, Long> {

    /**
     * 查找当前激活的LLM配置
     */
    Optional<LlmConfig> findByIsActiveTrue();
}
