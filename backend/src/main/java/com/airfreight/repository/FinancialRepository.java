package com.airfreight.repository;

import com.airfreight.entity.Financial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinancialRepository extends JpaRepository<Financial, Long> {
    List<Financial> findByShipmentId(Long shipmentId);
    List<Financial> findByPaymentStatus(String paymentStatus);
}