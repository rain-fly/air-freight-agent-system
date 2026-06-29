package com.airfreight.controller;

import com.airfreight.dto.BookingRequest;
import com.airfreight.entity.Booking;
import com.airfreight.service.BookingService;
import com.airfreight.repository.BookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订舱管理 - 对应工作流第4步：订舱申请与确认
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "订舱管理", description = "空运代理15步工作流 - 第4步：订舱申请与确认（含IATA对接）")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @PostMapping
    @Operation(summary = "提交订舱", description = "提交订舱申请，对接IATA航空订舱API")
    public ResponseEntity<Booking> submitBooking(@RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.submitBooking(request));
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认舱位", description = "航司确认舱位后更新订舱状态")
    public ResponseEntity<Booking> confirmBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }

    @GetMapping
    @Operation(summary = "查询所有订舱记录")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询订舱详情")
    public ResponseEntity<Booking> getBooking(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/generate-awb")
    @Operation(summary = "生成空运提单号", description = "生成MAWB或HAWB提单号")
    public ResponseEntity<String> generateAwb(@RequestParam String type,
                                              @RequestParam String airlineCode) {
        return ResponseEntity.ok(bookingService.generateAwbNumber(type, airlineCode));
    }
}