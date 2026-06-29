package com.airfreight.service;

import com.airfreight.entity.Notification;
import com.airfreight.entity.Shipment;
import com.airfreight.repository.NotificationRepository;
import com.airfreight.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知推送服务 - 对应工作流第10步（到货通知）
 * 支持：邮件 / 短信 / 微信 / APP推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 发送到货通知
     */
    @Transactional
    public Notification sendArrivalNotice(Long shipmentId, String channel, String recipient) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("运单不存在: " + shipmentId));

        Notification notification = new Notification();
        notification.setShipment(shipment);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setNotifyType("ARRIVAL_NOTICE");
        notification.setSubject("到货通知 - " + shipment.getShipmentNo());
        notification.setContent(String.format(
                "您的货物已到达目的港。\n运单号: %s\n品名: %s\n起运港: %s → 目的港: %s\n请尽快办理清关提货手续。",
                shipment.getShipmentNo(), shipment.getGoodsName(),
                shipment.getOriginAirport(), shipment.getDestAirport()));
        notification.setStatus("SENT");
        notification.setSentAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        log.info("[通知推送] 到货通知已发送: {} -> {} ({}: {})",
                shipment.getShipmentNo(), recipient, channel, saved.getId());
        return saved;
    }

    /**
     * 发送报关状态通知
     */
    @Transactional
    public Notification sendCustomsStatusNotice(Long shipmentId, String channel, String recipient,
                                                 String status, String message) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("运单不存在: " + shipmentId));

        Notification notification = new Notification();
        notification.setShipment(shipment);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setNotifyType("CUSTOMS_STATUS");
        notification.setSubject("报关状态更新 - " + shipment.getShipmentNo());
        notification.setContent("报关状态: " + status + "\n" + message);
        notification.setStatus("SENT");
        notification.setSentAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * 发送付款提醒
     */
    @Transactional
    public Notification sendPaymentReminder(Long shipmentId, String channel, String recipient) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("运单不存在: " + shipmentId));

        Notification notification = new Notification();
        notification.setShipment(shipment);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setNotifyType("PAYMENT_REMINDER");
        notification.setSubject("付款提醒 - " + shipment.getShipmentNo());
        notification.setContent(String.format(
                "运单 %s 账单已生成，总费用 %.2f %s，请及时支付。",
                shipment.getShipmentNo(), shipment.getTotalCharge(), shipment.getCurrency()));
        notification.setStatus("SENT");
        notification.setSentAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    public List<Notification> getByShipment(Long shipmentId) {
        return notificationRepository.findByShipmentId(shipmentId);
    }
}