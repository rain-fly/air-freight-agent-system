package com.airfreight.repository;

import com.airfreight.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByShipmentIdOrderByEventTimeDesc(Long shipmentId);
    List<TrackingEvent> findByShipmentIdAndStatus(Long shipmentId, String status);
}