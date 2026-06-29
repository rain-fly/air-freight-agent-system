package com.airfreight.controller;

import com.airfreight.entity.TrackingEvent;
import com.airfreight.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在途跟踪 - 对应工作流第9步
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "在途跟踪", description = "货物运输全程跟踪，对接IATA Cargo Tracking API")
@CrossOrigin(origins = "*")
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/event/{shipmentId}")
    @Operation(summary = "记录跟踪事件")
    public ResponseEntity<TrackingEvent> recordEvent(
            @PathVariable Long shipmentId,
            @RequestParam String eventType,
            @RequestParam String location,
            @RequestParam String description) {
        return ResponseEntity.ok(trackingService.recordEvent(shipmentId, eventType, location, description));
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "查询运单跟踪记录")
    public ResponseEntity<List<TrackingEvent>> getTracking(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(trackingService.getTrackingByShipment(shipmentId));
    }
}