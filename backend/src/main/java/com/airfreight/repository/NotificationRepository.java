package com.airfreight.repository;

import com.airfreight.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByShipmentId(Long shipmentId);
    List<Notification> findByStatus(String status);
    List<Notification> findByNotifyType(String notifyType);
}