package com.airfreight.service;

import com.airfreight.dto.BookingRequest;
import com.airfreight.entity.Booking;
import com.airfreight.entity.Shipment;
import com.airfreight.repository.BookingRepository;
import com.airfreight.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

/**
 * 订舱服务 - 对应工作流第4步：订舱申请与确认
 * 包含：提交订舱、确认舱位、生成入仓通知
 * 国外对接：IATA航空订舱API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 提交订舱申请
     */
    @Transactional
    public Booking submitBooking(BookingRequest request) {
        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new RuntimeException("运单不存在: " + request.getShipmentId()));

        // 更新运单状态
        shipment.setStatus("BOOKED");
        shipment.setAirlineCode(request.getAirlineCode());
        shipmentRepository.save(shipment);

        // 创建订舱记录
        Booking booking = new Booking();
        booking.setBookingNo("BKG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + new Random().nextInt(1000));
        booking.setShipment(shipment);
        booking.setBookingStatus("SUBMITTED");
        booking.setBookingNote(request.getBookingNote());

        // 模拟入仓通知单
        booking.setWarehouseReceipt("WR" + System.currentTimeMillis());
        booking.setCutOffTime(LocalDateTime.now().plusHours(24));

        Booking saved = bookingRepository.save(booking);
        log.info("订舱提交成功: {} 运单={} 航司={}", saved.getBookingNo(), shipment.getShipmentNo(), request.getAirlineCode());

        // 模拟国外对接 - IATA订舱API调用
        callIataBookingApi(booking);

        return saved;
    }

    /**
     * 确认舱位（模拟航空公司回执）
     */
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("订舱记录不存在: " + bookingId));

        booking.setBookingStatus("CONFIRMED");
        booking.setConfirmedAt(LocalDateTime.now());

        // 更新运单状态
        Shipment shipment = booking.getShipment();
        shipment.setStatus("CONFIRMED");
        shipmentRepository.save(shipment);

        Booking saved = bookingRepository.save(booking);
        log.info("舱位确认成功: {} 运单={}", saved.getBookingNo(), shipment.getShipmentNo());

        return saved;
    }

    /**
     * 模拟对接IATA航空订舱API
     */
    private void callIataBookingApi(Booking booking) {
        log.info("[IATA API] 提交订舱请求: BookingNo={}, 航司={}",
                booking.getBookingNo(), booking.getShipment().getAirlineCode());
        log.info("[IATA API] 响应: 订舱成功, 待航司确认舱位");
    }

    /**
     * 生成空运提单号（模拟）
     * MAWB: 航空公司主单
     * HAWB: 货代分单（拼舱货物）
     */
    public String generateAwbNumber(String type, String airlineCode) {
        String prefix = "MAWB".equalsIgnoreCase(type) ? airlineCode : "HAWB";
        String number = prefix + "-" + System.currentTimeMillis();
        log.info("生成提单号: type={} number={}", type, number);
        return number;
    }
}