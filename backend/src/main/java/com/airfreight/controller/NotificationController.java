package com.airfreight.controller;

import com.airfreight.entity.Notification;
import com.airfreight.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知推送 - 对应工作流第10步（到货通知）
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知推送", description = "邮件/短信/微信/APP通知推送（到货通知、报关状态、付款提醒）")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/arrival/{shipmentId}")
    @Operation(summary = "发送到货通知")
    public ResponseEntity<Notification> sendArrivalNotice(
            @PathVariable Long shipmentId,
            @RequestParam(defaultValue = "EMAIL") String channel,
            @RequestParam String recipient) {
        return ResponseEntity.ok(notificationService.sendArrivalNotice(shipmentId, channel, recipient));
    }

    @PostMapping("/payment-reminder/{shipmentId}")
    @Operation(summary = "发送付款提醒")
    public ResponseEntity<Notification> sendPaymentReminder(
            @PathVariable Long shipmentId,
            @RequestParam(defaultValue = "EMAIL") String channel,
            @RequestParam String recipient) {
        return ResponseEntity.ok(notificationService.sendPaymentReminder(shipmentId, channel, recipient));
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "查询运单通知记录")
    public ResponseEntity<List<Notification>> getByShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(notificationService.getByShipment(shipmentId));
    }
}