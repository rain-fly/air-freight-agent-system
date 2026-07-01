package com.airfreight.repository;

import com.airfreight.entity.ClassificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {

    /**
     * 查询所有激活的规则，按优先级排序
     */
    List<ClassificationRule> findByIsActiveTrueOrderByPriorityAsc();
}
