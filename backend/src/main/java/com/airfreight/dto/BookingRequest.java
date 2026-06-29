package com.airfreight.dto;

import lombok.Data;

/**
 * 订舱请求DTO - 对应工作流第4步
 */
@Data
public class BookingRequest {
    private Long shipmentId;
    private String bookingNote;
    private String airlineCode;
}