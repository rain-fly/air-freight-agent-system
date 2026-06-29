package com.airfreight.service;

import com.airfreight.entity.TrackingEvent;
import com.airfreight.entity.Shipment;
import com.airfreight.repository.TrackingEventRepository;
import com.airfreight.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在途跟踪服务 - 对应工作流第9步
 * 国外对接：IATA Cargo Tracking API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 记录跟踪事件
     */
    @Transactional
    public TrackingEvent recordEvent(Long shipmentId, String eventType, String location, String description) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("运单不存在: " + shipmentId));

        TrackingEvent event = new TrackingEvent();
        event.setShipment(shipment);
        event.setEventType(eventType);
        event.setLocation(location);
        event.setDescription(description);
        event.setStatus("COMPLETED");
        event.setEventTime(LocalDateTime.now());

        // 更新运单状态
        switch (eventType) {
            case "DEPARTURE":
                shipment.setStatus("IN_TRANSIT");
                shipment.setActualDeparture(LocalDateTime.now());
                break;
            case "ARRIVAL":
                shipment.setStatus("ARRIVED");
                shipment.setActualArrival(LocalDateTime.now());
                break;
            case "DELIVERY":
                shipment.setStatus("DELIVERED");
                break;
        }
        shipmentRepository.save(shipment);

        TrackingEvent saved = trackingEventRepository.save(event);
        log.info("跟踪事件: {} 运单={} 位置={} 描述={}",
                eventType, shipment.getShipmentNo(), location, description);

        return saved;
    }

    public List<TrackingEvent> getTrackingByShipment(Long shipmentId) {
        return trackingEventRepository.findByShipmentIdOrderByEventTimeDesc(shipmentId);
    }
}