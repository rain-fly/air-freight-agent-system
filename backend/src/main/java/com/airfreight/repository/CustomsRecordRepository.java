package com.airfreight.repository;

import com.airfreight.entity.CustomsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomsRecordRepository extends JpaRepository<CustomsRecord, Long> {
    List<CustomsRecord> findByShipmentId(Long shipmentId);
    List<CustomsRecord> findByStatus(String status);
    List<CustomsRecord> findByCustomsType(String customsType);
}