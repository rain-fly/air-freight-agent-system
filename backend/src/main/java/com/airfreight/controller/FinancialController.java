package com.airfreight.controller;

import com.airfreight.entity.Financial;
import com.airfreight.service.FinancialService;
import com.airfreight.repository.FinancialRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财务管理 - 对应工作流中的费用结算
 * 包含：账单生成、SWIFT国际支付、费用查询
 */
@RestController
@RequestMapping("/api/financials")
@RequiredArgsConstructor
@Tag(name = "财务管理", description = "费用结算、账单生成、SWIFT国际支付")
@CrossOrigin(origins = "*")
public class FinancialController {

    private final FinancialService financialService;
    private final FinancialRepository financialRepository;

    @PostMapping("/invoice/{shipmentId}")
    @Operation(summary = "生成账单", description = "根据运单ID生成完整的费用账单（含空运费、FSC、关税等）")
    public ResponseEntity<List<Financial>> generateInvoice(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(financialService.generateInvoice(shipmentId));
    }

    @GetMapping
    @Operation(summary = "查询所有账单")
    public ResponseEntity<List<Financial>> getAllFinancials() {
        return ResponseEntity.ok(financialRepository.findAll());
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "按运单查询账单")
    public ResponseEntity<List<Financial>> getFinancialsByShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(financialRepository.findByShipmentId(shipmentId));
    }

    @PostMapping("/{id}/swift-payment")
    @Operation(summary = "SWIFT国际支付", description = "通过SWIFT发起国际电汇支付账单")
    public ResponseEntity<Financial> processSwiftPayment(
            @PathVariable Long id,
            @RequestParam String swiftCode,
            @RequestParam String bankAccount) {
        return ResponseEntity.ok(financialService.processSwiftPayment(id, swiftCode, bankAccount));
    }
}