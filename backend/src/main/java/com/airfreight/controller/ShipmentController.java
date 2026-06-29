package com.airfreight.controller;

import com.airfreight.dto.QuotationRequest;
import com.airfreight.dto.QuotationResponse;
import com.airfreight.dto.VolumeWeightResult;
import com.airfreight.entity.Shipment;
import com.airfreight.service.QuotationService;
import com.airfreight.repository.ShipmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 询价与Shipment管理 - 对应工作流第1步
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "询价与运单管理", description = "空运代理15步工作流 - 第1步：需求确认与询价")
@CrossOrigin(origins = "*")
public class ShipmentController {

    private final QuotationService quotationService;
    private final ShipmentRepository shipmentRepository;

    @PostMapping("/calculate-volume-weight")
    @Operation(summary = "计算体积重", description = "输入长宽高和毛重，计算体积重和计费重")
    public ResponseEntity<VolumeWeightResult> calculateVolumeWeight(
            @RequestParam BigDecimal grossWeight,
            @RequestParam BigDecimal length,
            @RequestParam BigDecimal width,
            @RequestParam BigDecimal height) {
        return ResponseEntity.ok(quotationService.calculateVolumeWeight(grossWeight, length, width, height));
    }

    @PostMapping("/quotation")
    @Operation(summary = "生成报价", description = "基于货物信息生成完整报价（含FSC、SSG、地面费等）")
    public ResponseEntity<QuotationResponse> generateQuotation(@RequestBody QuotationRequest request) {
        return ResponseEntity.ok(quotationService.generateQuotation(request));
    }

    @PostMapping("/create-from-quotation")
    @Operation(summary = "从报价创建运单", description = "确认报价后，生成正式运单记录")
    public ResponseEntity<Shipment> createShipment(@RequestBody QuotationRequest request) {
        QuotationResponse quotation = quotationService.generateQuotation(request);
        Shipment shipment = quotationService.createShipmentFromQuotation(request, quotation);
        return ResponseEntity.ok(shipment);
    }

    @GetMapping
    @Operation(summary = "查询所有运单", description = "获取全部运单列表")
    public ResponseEntity<List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(shipmentRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询运单详情", description = "按ID获取运单详细信息")
    public ResponseEntity<Shipment> getShipment(@PathVariable Long id) {
        return shipmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "按状态查询运单", description = "按状态筛选运单")
    public ResponseEntity<List<Shipment>> getShipmentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(shipmentRepository.findByStatus(status));
    }
}